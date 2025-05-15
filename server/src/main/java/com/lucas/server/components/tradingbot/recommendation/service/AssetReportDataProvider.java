package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class AssetReportDataProvider {

    private final MarketDataKpiGenerator kpiGenerator;

    public AssetReportDataProvider(MarketDataKpiGenerator kpiGenerator) {
        this.kpiGenerator = kpiGenerator;
    }

    public AssetReportRaw provide(Symbol symbol, List<MarketData> mdHistory, List<News> articles, PortfolioBase portfolio) {
        List<PricePointRaw> priceHistory = mdHistory.stream()
                .map(md -> new PricePointRaw(md.getDate(), md.getOpen(), md.getHigh(), md.getLow(), md.getPrice(), md.getVolume()))
                .toList();

        BigDecimal ma5 = kpiGenerator.computeMovingAverage(mdHistory);
        BigDecimal rsi14 = kpiGenerator.computeRsi(mdHistory);
        BigDecimal atr14 = kpiGenerator.computeAtr(mdHistory);
        BigDecimal volatility = kpiGenerator.computeVolatility(mdHistory);

        List<NewsItemRaw> news = articles.stream()
                .map(a -> new NewsItemRaw(a.getHeadline(), a.getSentiment(), a.getSentimentConfidence(), a.getSummary(), a.getDate()))
                .toList();

        BigDecimal quantity = portfolio.getQuantity();
        BigDecimal averageCost = portfolio.getAverageCost();
        BigDecimal positionValue;
        BigDecimal pnL;
        if (null != quantity && null != averageCost) {
            positionValue = quantity.multiply(averageCost).setScale(4, RoundingMode.HALF_UP);
            pnL = mdHistory.getFirst().getPrice().subtract(averageCost).multiply(quantity).setScale(4, RoundingMode.HALF_UP);
        } else {
            positionValue = null;
            pnL = null;
        }
        return new AssetReportRaw(symbol.getName(), quantity, positionValue, averageCost, priceHistory.size(), priceHistory,
                ma5, rsi14, atr14, volatility, pnL, news.size(), news);
    }
}
