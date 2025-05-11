package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItem;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AssetReportDataProvider {

    private final MarketDataKpiGenerator kpiGenerator;

    public AssetReportDataProvider(MarketDataKpiGenerator kpiGenerator) {
        this.kpiGenerator = kpiGenerator;
    }

    public AssetReportRaw provide(Symbol symbol, List<MarketData> mdHistory, List<News> articles) throws IllegalStateException {
        List<PricePointRaw> priceHistory = mdHistory.stream()
                .map(md -> new PricePointRaw(md.getDate(), md.getOpen(), md.getHigh(), md.getLow(), md.getPrice(), md.getVolume()))
                .toList();

        BigDecimal ma5 = kpiGenerator.computeMovingAverage(mdHistory);
        BigDecimal rsi14 = kpiGenerator.computeRsi(mdHistory);
        BigDecimal atr14 = kpiGenerator.computeAtr(mdHistory);
        BigDecimal volatility = kpiGenerator.computeVolatility(mdHistory);

        List<NewsItem> newsItems = articles.stream()
                .map(a -> new NewsItem(a.getHeadline(), null, a.getSummary()))
                .toList();

        // TODO: generate missing attributes
        return new AssetReportRaw(symbol.getName(), null, null, null, priceHistory.size(), priceHistory,
                ma5, rsi14, atr14, volatility, null, newsItems.size(), newsItems);
    }
}
