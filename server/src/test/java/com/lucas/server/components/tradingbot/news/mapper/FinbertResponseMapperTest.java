package com.lucas.server.components.tradingbot.news.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.utils.exception.MappingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinbertResponseMapperTest {

    private final FinbertResponseMapper mapper = new FinbertResponseMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Stream<Arguments> getSentimentData() {
        return Stream.of(
                Arguments.of("positive", "0.95", "95"),
                Arguments.of("negative", "0.85", "85"),
                Arguments.of("neutral", "0.60", "60")
        );
    }

    private static Stream<Arguments> getInvalidJson() {
        return Stream.of(
                Arguments.of("{}"),
                Arguments.of("{\"label\": \"positive\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("getSentimentData")
    void mapSentiment(String label, String score, String expectedConfidence) throws Exception {
        // given
        String json = String.format("{\"label\": \"%s\", \"score\": \"%s\"}", label, score);
        JsonNode node = objectMapper.readTree(json);

        // when
        News result = mapper.map(node);

        // then
        assertThat(result.getSentiment()).isEqualTo(label);
        assertThat(result.getSentimentConfidence()).isEqualByComparingTo(new BigDecimal(expectedConfidence));
    }

    @Test
    void mapWithExistingNews() throws Exception {
        // given
        String json = "{\"label\": \"positive\", \"score\": \"0.90\"}";
        JsonNode node = objectMapper.readTree(json);
        News existing = new News().setHeadline("Test headline");

        // when
        News result = mapper.map(node, existing);

        // then
        assertThat(result.getHeadline()).isEqualTo("Test headline");
        assertThat(result.getSentiment()).isEqualTo("positive");
        assertThat(result.getSentimentConfidence()).isEqualByComparingTo(new BigDecimal("90"));
    }

    @ParameterizedTest
    @MethodSource("getInvalidJson")
    void mapInvalidJson(String json) throws Exception {
        // given
        JsonNode node = objectMapper.readTree(json);

        // when & then
        assertThatThrownBy(() -> mapper.map(node))
                .isInstanceOf(MappingException.class);
    }
}
