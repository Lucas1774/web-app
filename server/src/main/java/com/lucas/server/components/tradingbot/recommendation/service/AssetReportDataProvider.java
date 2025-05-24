package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

import static com.lucas.server.common.Constants.HISTORY_DAYS_COUNT;
import static java.lang.Math.min;

@Component
public class AssetReportDataProvider {

    private final MarketDataKpiGenerator kpiGenerator;
    private final PortfolioManager portfolioManager;

    public AssetReportDataProvider(MarketDataKpiGenerator kpiGenerator, PortfolioManager portfolioManager) {
        this.kpiGenerator = kpiGenerator;
        this.portfolioManager = portfolioManager;
    }

    public AssetReportRaw provide(Symbol symbol, List<MarketData> mdHistory, List<News> articles, PortfolioBase portfolio) {
        List<PricePointRaw> priceHistory = mdHistory.subList(0, min(mdHistory.size(), HISTORY_DAYS_COUNT)).stream()
                .map(md -> new PricePointRaw(md.getDate(), md.getOpen(), md.getHigh(), md.getLow(), md.getPrice(), md.getVolume()))
                .toList();

        BigDecimal ema20 = kpiGenerator.computeEma(mdHistory, 20).orElse(null);
        BigDecimal macdLine1226 = kpiGenerator.computeMacdLine(mdHistory, 12, 26).orElse(null);
        BigDecimal macdSignalLine9 = kpiGenerator.computeSignalLine(mdHistory, 9, 12, 26).orElse(null);
        MarketData current = mdHistory.getFirst();
        BigDecimal rsi14 = kpiGenerator.computeRsi(current);
        BigDecimal atr14 = current.getAtr();
        BigDecimal obv20 = kpiGenerator.computeObv(mdHistory, 20).orElse(null);

        List<NewsItemRaw> news = articles.stream()
                .map(a -> new NewsItemRaw(a.getHeadline(), a.getSentiment(), a.getSentimentConfidence(), a.getSummary(), a.getDate()))
                .toList();

        PortfolioManager.SymbolStand stand = portfolioManager.computeStand(portfolio, current);
        return new AssetReportRaw(symbol.getName(), stand.quantity(), stand.positionValue(), stand.averageCost(), stand.pnL(), stand.percentPnl(),
                priceHistory.size(), priceHistory, ema20, macdLine1226, macdSignalLine9, rsi14, atr14, obv20, news.size(), news);
    }
}
