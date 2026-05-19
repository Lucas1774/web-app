package com.lucas.server.components.tradingbot.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.ConfigurationException;
import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationChatCompletionResponseMapper;
import com.lucas.utils.Interrupts;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.CLIENT_ROTATION_DEBOUNCE_MS;
import static com.lucas.server.common.Constants.CONTENT;
import static com.lucas.server.common.Constants.NY_ZONE;
import static com.lucas.server.common.Constants.PROMPTING_MODEL_INFO;
import static com.lucas.server.common.Constants.RECOMMENDATION;
import static com.lucas.server.common.Constants.RECOMMENDATION_COMPLETION_ERROR;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.sanitizeHtml;

@Component
@Slf4j
public class RecommendationChatCompletionClient {

    private final ObjectNode systemMessage;
    private final ObjectNode systemLongTermMessage;
    private final ObjectNode context;
    private final ObjectNode fewShotMessage;
    private final ObjectNode fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final ObjectMapper objectMapper;
    private final RecommendationChatCompletionResponseMapper mapper;

    public RecommendationChatCompletionClient(AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper,
                                              ObjectMapper objectMapper,
                                              RecommendationChatCompletionResponseMapper mapper) {
        try (Reader contextReader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                "/prompt/context.json")), StandardCharsets.UTF_8);
             Reader systemReader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                     "/prompt/system.json")), StandardCharsets.UTF_8);
             Reader systemLongTermReader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                     "/prompt/system-long-term.json")), StandardCharsets.UTF_8);
             Reader fewShotReader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                     "/prompt/few-shot.json")), StandardCharsets.UTF_8);
             Reader fixMeReader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                     "/prompt/fix-me.json")), StandardCharsets.UTF_8)) {
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
    }

    /**
     * Retrieves recommendations for the given symbol payloads using the provided AI clients.
     * Cycles through all clients indefinitely until one successfully acquires rate limit permits.
     *
     * @param payload    the symbol payloads to generate recommendations for
     * @param clients    the AI clients to use (must share the same chunk size)
     * @param useOldNews whether to use old news or filter by recent dates
     * @return set of recommendations
     * @throws ClientException         if the AI client request fails
     * @throws JsonProcessingException if JSON processing fails
     * @throws MappingException        if mapping the response fails
     */
    public Set<RecommendationDomain> getRecommendations(Set<DataManager.SymbolPayload> payload,
                                                        Set<AiClient> clients,
                                                        boolean useOldNews)
            throws ClientException, JsonProcessingException, MappingException {
        Set<AssetReportRaw> reports =
                payload.stream().map(assertReportDataProvider::provide).collect(Collectors.toUnmodifiableSet());
        ObjectNode rawReportMessage =
                objectMapper.readValue(assetReportToMustacheMapper.map(reports), ObjectNode.class);
        Set<SymbolDomain> symbols =
                payload.stream().map(DataManager.SymbolPayload::getSymbol).collect(Collectors.toUnmodifiableSet());
        ObjectNode contextMessage = context.deepCopy()
                .put(CONTENT,
                        context.get(CONTENT)
                                .asText()
                                .replace("{date}",
                                        ZonedDateTime.now(NY_ZONE)
                                                .format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss z",
                                                        Locale.ENGLISH))));
        ObjectNode usedSystemMessage = useOldNews ? systemLongTermMessage : systemMessage;

        log.info(RETRIEVING_DATA_INFO, RECOMMENDATION, symbols);
        AtomicReference<String> completion = new AtomicReference<>();

        while (true) {
            for (AiClient client : clients) {
                try {
                    ObjectNode reportMessage;
                    if (!client.getConfig().fixMe()) {
                        reportMessage = rawReportMessage;
                    } else {
                        reportMessage = objectMapper.readValue(fixMeMessage.get(CONTENT)
                                .asText()
                                .replace("{placeholder}", rawReportMessage.get(CONTENT).asText()), ObjectNode.class);
                    }
                    OrderedIndexedSet<JsonNode> prompt =
                            OrderedIndexedSet.of(usedSystemMessage, contextMessage, fewShotMessage, reportMessage);
                    Optional<Set<RecommendationDomain>> res =
                            client.getRateLimiter().tryCall(() -> client.getConcurrentRequestsRateLimiter().call(() -> {
                                client.getApiKeyRateLimiter().acquirePermission();
                                log.info(PROMPTING_MODEL_INFO, client.getConfig().name());
                                completion.set(client.complete(prompt));
                                return mapper.mapAll(payload,
                                        objectMapper.readTree(completion.get()),
                                        prompt.stream()
                                                .map(p -> sanitizeHtml(p.get(CONTENT).asText()))
                                                .collect(Collectors.joining("\n\n\n")),
                                        client.getConfig().name());
                            }));
                    if (res.isPresent()) {
                        return res.get();
                    }

                    Interrupts.runOrSwallow(() -> Thread.sleep(CLIENT_ROTATION_DEBOUNCE_MS),
                            e -> log.error(e.getMessage(), e));
                } catch (MappingException | JsonProcessingException e) {
                    throw new ClientException(MessageFormat.format(RECOMMENDATION_COMPLETION_ERROR, completion.get()),
                            e);
                } catch (ClientException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }
        }
    }
}
