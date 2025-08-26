package com.lucas.server.components.tradingbot.news.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.news.jpa.News;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FinnhubNewsResponseMapperTest {

    @Autowired
    private SymbolJpaService symbolService;

    @Autowired
    private FinnhubNewsResponseMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
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
                    assertThat(n.getSymbols()).isEmpty();
                });
    }

    @Test
    @Transactional
    void whenMapAllValidArray_thenReturnNewsList() throws Exception {
        // given
        long now = 1745463072L;
        String jsonArray = String.format("""
                [
                  { "id": %d, "datetime": %d, "headline": "H1", "summary": "S1", "url": "u1", "source": "src1", "category": "cat1", "image": "i1" },
                  { "id": %d, "datetime": %d, "headline": "H2", "summary": "S2", "url": "u2", "source": "src2", "category": "cat2", "image": "i2" }
                ]
                """, now, now, now + 60, now + 60);
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();

        JsonNode arrayNode = objectMapper.readTree(jsonArray);

        // when
        List<News> list = mapper.mapAll(arrayNode, symbol);

        // then
        assertThat(list)
                .isNotNull()
                .hasSize(2)
                .allSatisfy(n -> assertThat(n.getSymbols().stream().map(Symbol::getName))
                        .hasSize(1)
                        .containsExactly(symbol.getName()))
                .extracting(News::getExternalId, News::getHeadline)
                .containsExactly(
                        tuple(now, "H1"),
                        tuple(now + 60, "H2")
                );
    }

    @Test
    @Transactional
    void whenMapAllEmptyOrNonArray_thenReturnEmptyList() throws Exception {
        // given
        JsonNode emptyArray = objectMapper.createArrayNode();
        JsonNode objNode = objectMapper.createObjectNode();
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();

        // when & then
        assertThat(mapper.mapAll(emptyArray, symbol)).isEmpty();
        assertThat(mapper.mapAll(objNode, symbol)).isEmpty();
    }

    @Test
    @Transactional
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
        assertThatThrownBy(() -> mapper.mapAll(arrayNode, symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow()))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(MAPPING_ERROR, "news"));
    }
}
