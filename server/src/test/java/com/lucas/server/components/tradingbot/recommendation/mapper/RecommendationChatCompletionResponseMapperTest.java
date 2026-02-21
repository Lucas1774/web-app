package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.utils.exception.MappingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationChatCompletionResponseMapperTest {

    private final RecommendationChatCompletionResponseMapper mapper = new RecommendationChatCompletionResponseMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapBuyRecommendation() throws Exception {
        // given
        String json = "{\"action\": \"BUY\", \"confidence\": \"0.85\", \"rationale\": \"Strong growth potential\"}";
        JsonNode node = objectMapper.readTree(json);

        // when
        Recommendation result = mapper.map(node);

        // then
        assertThat(result.getDate()).isEqualTo(LocalDate.now());
        assertThat(result.getAction()).isEqualTo("BUY");
        assertThat(result.getConfidence()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(result.getRationale()).isEqualTo("Strong growth potential");
    }

    @Test
    void mapHoldRecommendation() throws Exception {
        // given
        String json = "{\"action\": \"HOLD\", \"confidence\": \"0.60\", \"rationale\": \"Wait for better entry\"}";
        JsonNode node = objectMapper.readTree(json);

        // when
        Recommendation result = mapper.map(node);

        // then
        assertThat(result.getAction()).isEqualTo("HOLD");
        assertThat(result.getConfidence()).isEqualByComparingTo(new BigDecimal("0.60"));
    }

    @Test
    void mapSellRecommendation() throws Exception {
        // given
        String json = "{\"action\": \"SELL\", \"confidence\": \"0.90\", \"rationale\": \"Overvalued\"}";
        JsonNode node = objectMapper.readTree(json);

        // when
        Recommendation result = mapper.map(node);

        // then
        assertThat(result.getAction()).isEqualTo("SELL");
        assertThat(result.getConfidence()).isEqualByComparingTo(new BigDecimal("0.90"));
    }

    @Test
    void mapTruncatesLongRationale() throws Exception {
        // given
        String longRationale = "a".repeat(2000);
        String json = String.format("{\"action\": \"BUY\", \"confidence\": \"0.75\", \"rationale\": \"%s\"}", longRationale);
        JsonNode node = objectMapper.readTree(json);

        // when
        Recommendation result = mapper.map(node);

        // then
        assertThat(result.getRationale()).hasSize(1024);
    }

    @Test
    void mapInvalidJson() throws Exception {
        // given
        String json = "{}";
        JsonNode node = objectMapper.readTree(json);

        // when & then
        assertThatThrownBy(() -> mapper.map(node))
                .isInstanceOf(MappingException.class);
    }

    @Test
    void mapMissingConfidence() throws Exception {
        // given
        String json = "{\"action\": \"BUY\", \"rationale\": \"Test\"}";
        JsonNode node = objectMapper.readTree(json);

        // when & then
        assertThatThrownBy(() -> mapper.map(node))
                .isInstanceOf(MappingException.class);
    }
}
