package com.lucas.server.components.tradingbot.news.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

class FinnhubNewsResponseMapperTest {

    private final FinnhubNewsResponseMapper mapper = new FinnhubNewsResponseMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void whenMapValidJson_thenReturnNewsEntity() throws Exception {
        // given
        long epoch = 1745463072L;
        String json = String.format("""
                {
                  "id": %d,
                  "datetime": %d,
                  "headline": "Test Headline",
                  "summary": "Test Summary",
                  "url": "https://example.com/article",
                  "source": "Finnhub",
                  "category": "company",
                  "image": "https://example.com/image.jpg"
                }
                """, epoch, epoch);

        JsonNode node = objectMapper.readTree(json);

        // when
        News news = mapper.map(node);

        // then
        LocalDateTime expectedDate = Instant.ofEpochSecond(epoch)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        assertThat(news)
                .isNotNull()
                .satisfies(n -> {
                    assertThat(n.getExternalId()).isEqualTo(epoch);
                    assertThat(n.getDate()).isEqualTo(expectedDate);
                    assertThat(n.getHeadline()).isEqualTo("Test Headline");
                    assertThat(n.getSummary()).isEqualTo("Test Summary");
                    assertThat(n.getUrl()).isEqualTo("https://example.com/article");
                    assertThat(n.getSource()).isEqualTo("Finnhub");
                    assertThat(n.getCategory()).isEqualTo("company");
                    assertThat(n.getImage()).isEqualTo("https://example.com/image.jpg");
                    assertThat(n.getSymbol()).isNull();
                });
    }

    @Test
    void whenMapAllValidArray_thenReturnNewsList() throws Exception {
        // given
        long now = 1745463072L;
        String jsonArray = String.format("""
                [
                  { "id": %d, "datetime": %d, "headline": "H1", "summary": "S1", "url": "u1", "source": "src1", "category": "cat1", "image": "i1" },
                  { "id": %d, "datetime": %d, "headline": "H2", "summary": "S2", "url": "u2", "source": "src2", "category": "cat2", "image": "i2" }
                ]
                """, now, now, now + 60, now + 60);

        JsonNode arrayNode = objectMapper.readTree(jsonArray);

        // when
        List<News> list = mapper.mapAll(arrayNode, "SYM");

        // then
        assertThat(list)
                .isNotNull()
                .hasSize(2)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactly(
                        tuple(now, "SYM", "H1"),
                        tuple(now + 60, "SYM", "H2")
                );
    }

    @Test
    void whenMapAllEmptyOrNonArray_thenReturnEmptyList() throws Exception {
        // given
        JsonNode emptyArray = objectMapper.createArrayNode();
        JsonNode objNode = objectMapper.createObjectNode();

        // when & then
        assertThat(mapper.mapAll(emptyArray, "SYM")).isEmpty();
        assertThat(mapper.mapAll(objNode, "SYM")).isEmpty();
    }

    @Test
    void whenMapAllMissingFields_thenThrowsException() throws Exception {
        // given
        String jsonArray = """
                [
                  { "datetime": 1, "headline": "H1", "summary": "S1", "url": "u1", "source": "src1", "category": "cat1", "image": "i1" },
                  { "datetime": 2, "headline": "H2", "summary": "S2", "url": "u2", "source": "src2", "category": "cat2", "image": "i2" }
                ]
                """;
        JsonNode arrayNode = objectMapper.readTree(jsonArray);

        // when & then
        assertThatThrownBy(() -> mapper.mapAll(arrayNode, "SYM"))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "news"));
    }
}
