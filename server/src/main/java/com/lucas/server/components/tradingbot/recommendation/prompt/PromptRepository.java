package com.lucas.server.components.tradingbot.recommendation.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.exception.ConfigurationException;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Getter
@Repository
public class PromptRepository {

    private final ObjectNode context;
    private final ObjectNode system;
    private final ObjectNode fewShot;
    private final ObjectNode fixMeMessage;

    public PromptRepository(ObjectMapper objectMapper) {
        try (Reader contextReader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/prompt/context.json")),
                StandardCharsets.UTF_8);
             Reader systemReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/system.json")),
                     StandardCharsets.UTF_8);
             Reader fewShotReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/few-shot.json")),
                     StandardCharsets.UTF_8);
             Reader fixMeReader = new InputStreamReader(
                     Objects.requireNonNull(getClass().getResourceAsStream("/prompt/fix-me.json")),
                     StandardCharsets.UTF_8)) {
            this.context = objectMapper.readValue(contextReader, ObjectNode.class);
            this.system = objectMapper.readValue(systemReader, ObjectNode.class);
            this.fewShot = objectMapper.readValue(fewShotReader, ObjectNode.class);
            this.fixMeMessage = objectMapper.readValue(fixMeReader, ObjectNode.class);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }
}
