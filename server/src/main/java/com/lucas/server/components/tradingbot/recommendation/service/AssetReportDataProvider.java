package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.lucas.server.common.Constants.HISTORY_DAYS_COUNT;
import static com.lucas.server.common.Constants.NEWS_SERIALIZATION_WARN;
import static java.lang.Math.min;

@Component
public class AssetReportDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(AssetReportDataProvider.class);
    private final MarketDataKpiGenerator kpiGenerator;
    private final PortfolioManager portfolioManager;

    public AssetReportDataProvider(MarketDataKpiGenerator kpiGenerator, PortfolioManager portfolioManager) {
        this.kpiGenerator = kpiGenerator;
        this.portfolioManager = portfolioManager;
    }

    public AssetReportRaw provide(DataManager.SymbolPayload payload) {
        OrderedIndexedSet<MarketDataDomain> mdHistory = payload.getMarketData();

        BigDecimal ema20 = kpiGenerator.computeEma(mdHistory, 20).orElse(null);
        BigDecimal macdLine1226 = kpiGenerator.computeMacdLine(mdHistory, 12, 26).orElse(null);
        BigDecimal macdSignalLine9 = kpiGenerator.computeSignalLine(mdHistory, 9, 12, 26).orElse(null);
        MarketDataDomain current = mdHistory.getFirst();
        BigDecimal rsi14 = kpiGenerator.computeRsi(current);
        BigDecimal atr14 = kpiGenerator.computeRelativeAtr(current);
        BigDecimal obv20 = kpiGenerator.computeObv(mdHistory, 20).orElse(null);

        MarketSnapshotDomain pm = payload.getPremarket();
        PricePointRaw premarket = null;
        if (null != pm) {
            MarketDataDomain pmmd = MarketDataDomain.from(pm);
            kpiGenerator.computeChange(pmmd, current.getPrice());
            premarket = new PricePointRaw(null, pmmd.getOpen(), pmmd.getHigh(), pmmd.getLow(), pmmd.getPrice(), null, pmmd.getChangePercent());
        }
        OrderedIndexedSet<PricePointRaw> priceHistory = mdHistory.subList(0, min(mdHistory.size(), HISTORY_DAYS_COUNT)).stream()
                .map(md -> new PricePointRaw(md.getDate(), md.getOpen(), md.getHigh(), md.getLow(), md.getPrice(), md.getVolume(), null))
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
        OrderedIndexedSet<NewsItemRaw> news = payload.getNews().stream()
                .map(a -> new NewsItemRaw(a.getHeadline(), a.getSentiment(), a.getSentimentConfidence(), a.getSummary(), a.getDate()))
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
        if (news.size() != payload.getNews().size()) {
            logger.warn(NEWS_SERIALIZATION_WARN, payload.getSymbol().getName());
        }

        PortfolioManager.SymbolStand stand = portfolioManager.computeStand(payload.getPortfolio(), current);
        return new AssetReportRaw(payload.getSymbol().getName(), stand.quantity(), stand.positionValue(), stand.averageCost(), stand.pnL(), stand.percentPnl(),
                priceHistory.size(), premarket, priceHistory, ema20, macdLine1226, macdSignalLine9, rsi14, atr14, obv20, news.size(), news);
    }
}
