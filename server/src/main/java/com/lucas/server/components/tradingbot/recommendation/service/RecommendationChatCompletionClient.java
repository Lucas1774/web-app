package com.lucas.server.components.tradingbot.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.ConfigurationException;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationChatCompletionResponseMapper;
import com.lucas.utils.SlidingWindowRateLimiter;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;

@Component
public class RecommendationChatCompletionClient {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);
    private final ObjectNode systemMessage;
    private final ObjectNode systemLongTermMessage;
    private final ObjectNode context;
    private final ObjectNode fewShotMessage;
    private final ObjectNode fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final ObjectMapper objectMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final Map<String, SlidingWindowRateLimiter> rateLimiters;

    public RecommendationChatCompletionClient(AssetReportDataProvider assertReportDataProvider, AssetReportToMustacheMapper assetReportToMustacheMapper,
                                              ObjectMapper objectMapper, RecommendationChatCompletionResponseMapper mapper,
                                              Map<String, SlidingWindowRateLimiter> rateLimiters) {
        try (Reader contextReader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/prompt/context.json")),
                StandardCharsets.UTF_8);
             Reader systemReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/system.json")),
                     StandardCharsets.UTF_8);
             Reader systemLongTermReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/system-long-term.json")),
                     StandardCharsets.UTF_8);
             Reader fewShotReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/few-shot.json")),
                     StandardCharsets.UTF_8);
             Reader fixMeReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/fix-me.json")),
                     StandardCharsets.UTF_8)) {
            context = objectMapper.readValue(contextReader, ObjectNode.class);
            systemMessage = objectMapper.readValue(systemReader, ObjectNode.class);
            systemLongTermMessage = objectMapper.readValue(systemLongTermReader, ObjectNode.class);
            fewShotMessage = objectMapper.readValue(fewShotReader, ObjectNode.class);
            fixMeMessage = objectMapper.readValue(fixMeReader, ObjectNode.class);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.rateLimiters = rateLimiters;
    }

    public Set<Recommendation> getRecommendations(Set<DataManager.SymbolPayload> payload, AIClient client, boolean useOldNews) throws ClientException, JsonProcessingException, MappingException {
        Set<AssetReportRaw> reports = payload.stream()
                .map(assertReportDataProvider::provide)
                .collect(Collectors.toSet());
        ObjectNode rawReportMessage = objectMapper.readValue(assetReportToMustacheMapper.map(reports), ObjectNode.class);
        ObjectNode reportMessage;
        if (!client.getConfig().fixMe()) {
            reportMessage = rawReportMessage;
        } else {
            reportMessage = objectMapper.readValue(fixMeMessage.get(CONTENT).asText()
                    .replace("{placeholder}", rawReportMessage.get(CONTENT).asText()), ObjectNode.class);
        }
        Set<Symbol> symbols = payload.stream().map(DataManager.SymbolPayload::getSymbol).collect(Collectors.toSet());
        ObjectNode contextMessage = context.deepCopy().put(CONTENT, context.get(CONTENT).asText().replace("{date}",
                ZonedDateTime.now(NY_ZONE).format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH))));

        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, symbols);
        try {
            client.getRateLimiter().acquirePermission();
            rateLimiters.get(client.getConfig().apiKey()).acquirePermission();
            rateLimiters.get(AI_PER_SECOND_RATE_LIMITER).acquirePermission();
            logger.info(PROMPTING_MODEL_INFO, client.getConfig().name());
            ObjectNode usedSystemMessage = useOldNews ? systemLongTermMessage : systemMessage;
            OrderedIndexedSet<JsonNode> prompt = OrderedIndexedSet.of(usedSystemMessage, contextMessage, fewShotMessage, reportMessage);
            return mapper.mapAll(payload,
                    objectMapper.readTree(client.complete(prompt)),
                    prompt.stream().map(p -> sanitizeHtml(p.get(CONTENT).asText())).collect(Collectors.joining("\n\n\n")), client.getConfig().name());
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
