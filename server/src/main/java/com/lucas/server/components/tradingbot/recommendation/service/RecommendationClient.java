package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface RecommendationClient {

    List<Recommendation> getRecommendations(Map<Symbol, List<MarketData>> marketData, Map<Symbol, List<News>> newsData,
                                            Map<Symbol, ? extends PortfolioBase> portfolioData, Boolean withFixmeRequest)
            throws ClientException, IOException;
}
