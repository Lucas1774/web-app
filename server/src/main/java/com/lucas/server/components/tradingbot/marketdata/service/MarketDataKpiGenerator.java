package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MarketDataKpiGenerator {

    private final MarketDataJpaService service;

    public MarketDataKpiGenerator(MarketDataJpaService service) {
        this.service = service;
    }

    public void computeDerivedFields(MarketData md) {
        this.service.findTopBySymbolAndDateBeforeOrderByDateDesc(md.getSymbol(), md.getDate())
                .ifPresentOrElse(previous -> {
                    BigDecimal previousPrice = previous.getPrice();
                    md.setPreviousClose(previousPrice);
                    BigDecimal change = md.getPrice().subtract(previousPrice);
                    md.setChange(change);
                    if (0 != previousPrice.compareTo(BigDecimal.ZERO)) {
                        String percentage = change.divide(previousPrice, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP) + "%";
                        md.setChangePercent(percentage);
                    }
                }, () -> {/* Do nothing */});
    }
}
