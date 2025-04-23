package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.JpaService;
import org.springframework.stereotype.Service;

@Service
public class MarketDataJpaService implements JpaService<MarketData> {

    private final MarketDataRepository repository;

    public MarketDataJpaService(MarketDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public MarketData save(MarketData entity) {
        return this.repository.save(entity);
    }

}
