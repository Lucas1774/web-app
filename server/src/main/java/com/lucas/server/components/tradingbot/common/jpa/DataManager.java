package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.AlphavantageMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.TwelveDataMarketDataClient;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.Granularity;
import static com.lucas.server.common.Constants.MAX_SYMBOLS_TO_TRIGGER_NEWS_EMBEDDINGS_GENERATION;

@Service
public class DataManager {

    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final NewsJpaService newsService;
    private final RecommendationChatCompletionClient recommendationsClient;
    private final FinnhubNewsClient newsClient;
    private final Map<Granularity, GranularityToMarketDataFunction> granularityToClientRunner;

    @FunctionalInterface
    private interface GranularityToMarketDataFunction {
        List<MarketData> apply(List<Symbol> symbols) throws ClientException, JsonProcessingException;
    }

    public DataManager(SymbolJpaService symbolService, MarketDataJpaService marketDataService, NewsJpaService newsService,
                       RecommendationChatCompletionClient recommendationsClient, FinnhubNewsClient newsClient,
                       TwelveDataMarketDataClient twelveDataMarketDataClient, AlphavantageMarketDataClient alphavantageMarketDataClient) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.recommendationsClient = recommendationsClient;
        this.newsClient = newsClient;
        this.granularityToClientRunner = new EnumMap<>(Map.of(
                Granularity.DAILY, twelveDataMarketDataClient::retrieveMarketData,
                Granularity.WEEKLY, symbols ->
                        alphavantageMarketDataClient.retrieveMarketData(symbols, Granularity.WEEKLY)
        ));
    }

    @Transactional(rollbackOn = {IllegalStateException.class, ClientException.class, IOException.class})
    public String getRecommendations(List<String> symbolNames) throws IllegalStateException, ClientException, IOException {
        List<Symbol> symbols = symbolNames.stream().map(this.symbolService::getOrCreateByName).toList();
        Map<Symbol, List<MarketData>> marketData = symbols.stream()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> this.marketDataService.getTopForSymbolId(symbol.getId(), Constants.HISTORY_DAYS_COUNT)
                ));
        Map<Symbol, List<News>> newsData = symbols.stream()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> this.newsService.getTopForSymbolId(symbol.getId(), Constants.NEWS_COUNT)
                ));

        return this.recommendationsClient.getRecommendations(marketData, newsData);
    }

    @Transactional
    public List<News> generateSentiment(List<String> symbolNames, LocalDate from, LocalDate to)
            throws IllegalStateException, ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().map(this.symbolService::getOrCreateByName).toList();
        return this.newsService.generateSentiment(symbols.stream().map(Symbol::getId).toList(), from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByDateRange(List<String> symbolNames, LocalDate from, LocalDate now) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        List<News> news = this.newsClient.retrieveNewsByDateRange(symbols, from, now);
        this.newsService.createOrUpdate(news, MAX_SYMBOLS_TO_TRIGGER_NEWS_EMBEDDINGS_GENERATION >= symbols.size());
        return news;
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<MarketData> retrieveMarketData(List<String> symbolNames, Granularity granularity) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        List<MarketData> mds = granularityToClientRunner.get(granularity).apply(symbols);
        this.marketDataService.createIgnoringDuplicates(mds);
        return mds;
    }
}
