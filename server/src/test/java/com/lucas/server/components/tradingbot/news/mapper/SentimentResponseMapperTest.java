package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentimentResponseMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SentimentResponseMapper mapper = new SentimentResponseMapper();

    static Stream<Arguments> invalidResponses() {
        NewsDomain a = new NewsDomain().setExternalId(10L);
        NewsDomain b = new NewsDomain().setExternalId(20L);

        return Stream.of(Arguments.of(OrderedIndexedSet.of(a), """
                                                               [
                                                                 {"sentiment": "positive", "score": 0.91}
                                                               ]
                                                               """),
                Arguments.of(OrderedIndexedSet.of(a, b), """
                                                         [
                                                           {"id": 1, "sentiment": "positive", "score": 0.91}
                                                         ]
                                                         """),
                Arguments.of(OrderedIndexedSet.of(a), """
                                                      [
                                                        {"id": 1, "sentiment": "super-bullish", "score": 0.91}
                                                      ]
                                                      """),
                Arguments.of(OrderedIndexedSet.of(a), """
                                                      [
                                                        {"id": 1, "sentiment": null, "score": 0.91}
                                                      ]
                                                      """),
                Arguments.of(OrderedIndexedSet.of(a), """
                                                      [
                                                        {"id": 1, "sentiment": "positive", "score": 1.42}
                                                      ]
                                                      """));
    }

    @Test
    void mapAll_shouldUpdateEveryNewsByIndexAndNormalizeConfidence() throws MappingException {
        // given
        NewsDomain a = new NewsDomain().setExternalId(10L);
        NewsDomain b = new NewsDomain().setExternalId(20L);

        String json = """
                      [
                        {"id": 1, "sentiment": "positive", "score": 0.91},
                        {"id": 2, "sentiment": "negative", "score": 0.42}
                      ]
                      """;

        // when
        mapper.mapAll(OrderedIndexedSet.of(a, b), objectMapper.readTree(json));

        // then
        assertThat(a.getSentiment()).isEqualTo("positive");
        assertThat(a.getSentimentConfidence()).isEqualByComparingTo(new BigDecimal("91.00"));
        assertThat(b.getSentiment()).isEqualTo("negative");
        assertThat(b.getSentimentConfidence()).isEqualByComparingTo(new BigDecimal("42.00"));
    }

    @ParameterizedTest(name = "mapAll_shouldRejectInvalidResponse: {0}")
    @MethodSource("invalidResponses")
    void mapAll_shouldRejectInvalidResponse(OrderedIndexedSet<NewsDomain> news, String json) {
        assertThatThrownBy(() -> mapper.mapAll(news, objectMapper.readTree(json))).isInstanceOf(MappingException.class);
    }
}