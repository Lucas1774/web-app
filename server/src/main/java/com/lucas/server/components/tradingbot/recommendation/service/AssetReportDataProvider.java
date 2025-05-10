package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItem;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AssetReportDataProvider {

    private final MarketDataJpaService marketDataService;
    private final NewsJpaService newsService;
    private final SymbolJpaService symbolService;
    private final MarketDataKpiGenerator kpiGenerator;

    public AssetReportDataProvider(MarketDataJpaService marketDataService, NewsJpaService newsService,
                                   SymbolJpaService symbolService, MarketDataKpiGenerator kpiGenerator) {
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.symbolService = symbolService;
        this.kpiGenerator = kpiGenerator;
    }

    @Transactional(rollbackOn = IllegalStateException.class)
    public AssetReportRaw provide(String symbol) throws IllegalStateException {
        Symbol symbolObject = symbolService.findByName(symbol)
                .orElseGet(() -> symbolService.save(new Symbol().setName(symbol)).orElseThrow());
        List<MarketData> mdHistory = marketDataService.getTopForSymbolId(symbolObject.getId(), Constants.HISTORY_DAYS_COUNT);

        List<PricePointRaw> priceHistory = mdHistory.stream()
                .map(md -> new PricePointRaw(md.getDate(), md.getOpen(), md.getHigh(), md.getLow(), md.getPrice(), md.getVolume()))
                .toList();

        BigDecimal ma5 = kpiGenerator.computeMovingAverage(mdHistory);
        BigDecimal rsi14 = kpiGenerator.computeRsi(mdHistory);
        BigDecimal atr14 = kpiGenerator.computeAtr(mdHistory);
        BigDecimal volatility = kpiGenerator.computeVolatility(mdHistory);

        List<News> articles = newsService.getTopForSymbolId(symbolObject.getId(), Constants.NEWS_COUNT);
        List<NewsItem> newsItems = articles.stream()
                .map(a -> new NewsItem(a.getHeadline(), null, a.getSummary()))
                .toList();

        // TODO: generate missing attributes
        return new AssetReportRaw(symbol, null, null, null, priceHistory.size(), priceHistory,
                ma5, rsi14, atr14, volatility, null, newsItems.size(), newsItems);
    }
}
