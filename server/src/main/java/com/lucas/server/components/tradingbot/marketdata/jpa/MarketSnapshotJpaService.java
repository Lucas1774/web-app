package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

@Service
public class MarketSnapshotJpaService implements JpaService<MarketSnapshot> {

    @Delegate
    private final GenericJpaServiceDelegate<MarketSnapshot, MarketSnapshotRepository> delegate;

    public MarketSnapshotJpaService(MarketSnapshotRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
    }
}
