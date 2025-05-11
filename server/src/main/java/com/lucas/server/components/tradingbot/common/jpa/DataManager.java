package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.AlphavantageMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.FinnhubMarketDataClient;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.jpa.NewsListener;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataManager {

    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final NewsJpaService newsService;
    private final RecommendationChatCompletionClient recommendationsClient;
    private final FinnhubNewsClient newsClient;
    private final FinnhubMarketDataClient finnhubMarketDataClient;
    private final AlphavantageMarketDataClient alphavantageMarketDataClient;

    public DataManager(SymbolJpaService symbolService, MarketDataJpaService marketDataService, NewsJpaService newsService,
                       RecommendationChatCompletionClient recommendationsClient, FinnhubNewsClient newsClient,
                       FinnhubMarketDataClient finnhubMarketDataClient, AlphavantageMarketDataClient alphavantageMarketDataClient) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.recommendationsClient = recommendationsClient;
        this.newsClient = newsClient;
        this.finnhubMarketDataClient = finnhubMarketDataClient;
        this.alphavantageMarketDataClient = alphavantageMarketDataClient;
    }

    public String getRecommendations(List<String> symbolNames) throws IllegalStateException, ClientException, IOException {
        List<Symbol> symbols = symbolNames.stream()
                .map(name ->
                        this.symbolService.findByName(name)
                                .orElseGet(() ->
                                        this.symbolService.save(new Symbol().setName(name))
                                                .orElseThrow())).toList();
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

    public List<News> retrieveLatestNews(String symbolName) throws ClientException, JsonProcessingException {
        Symbol symbol = this.symbolService.findByName(symbolName).orElseGet(
                () -> this.symbolService.save(new Symbol().setName(symbolName))
                        .orElseThrow());
        List<News> news = this.newsClient.retrieveLatestNews(symbol);
        NewsListener.setActive(true);
        this.newsService.saveAll(news);
        return news;
    }

    public List<News> retrieveNewsByDateRange(String symbolName, LocalDate from, LocalDate now) throws ClientException, JsonProcessingException {
        Symbol symbol = this.symbolService.findByName(symbolName).orElseGet(
                () -> this.symbolService.save(new Symbol().setName(symbolName))
                        .orElseThrow());
        List<News> news = this.newsClient.retrieveNewsByDateRange(symbol, from, now);
        NewsListener.setActive(true);
        this.newsService.saveAll(news);
        return news;
    }

    public MarketData retrieveMarketData(String symbolName) throws ClientException, JsonProcessingException {
        Symbol symbol = this.symbolService.findByName(symbolName).orElseGet(
                () -> this.symbolService.save(new Symbol().setName(symbolName))
                        .orElseThrow());
        MarketData md = this.finnhubMarketDataClient.retrieveMarketData(symbol);
        this.marketDataService.save(md);
        return md;
    }

    public List<MarketData> retrieveWeeklySeries(String symbolName) throws ClientException, JsonProcessingException {
        Symbol symbol = this.symbolService.findByName(symbolName).orElseGet(
                () -> this.symbolService.save(new Symbol().setName(symbolName))
                        .orElseThrow());
        List<MarketData> md = this.alphavantageMarketDataClient.retrieveWeeklySeries(symbol);
        this.marketDataService.saveAll(md);
        return md;
    }

    public List<MarketData> retrieveMarketData(List<String> symbolNames) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().map(name -> this.symbolService.findByName(name).orElseGet(
                () -> this.symbolService.save(new Symbol().setName(name))
                        .orElseThrow())).toList();
        List<MarketData> md = this.finnhubMarketDataClient.retrieveMarketData(symbols);
        this.marketDataService.saveAll(md);
        return md;
    }

    public List<News> retrieveLatestNews(List<String> symbolNames) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().map(name -> this.symbolService.findByName(name).orElseGet(
                () -> this.symbolService.save(new Symbol().setName(name))
                        .orElseThrow())).toList();
        List<News> news = this.newsClient.retrieveLatestNews(symbols);
        NewsListener.setActive(false);
        this.newsService.saveAll(news);
        return news;
    }
}
