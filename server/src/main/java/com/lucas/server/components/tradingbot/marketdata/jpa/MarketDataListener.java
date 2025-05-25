package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class MarketDataListener implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;
    private MarketDataKpiGenerator kpiGenerator;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @PrePersist
    @PreUpdate
    public void computeDerivedFields(MarketData md) {
        if (null == kpiGenerator) {
            kpiGenerator = beanFactory.getBean(MarketDataKpiGenerator.class);
        }
        kpiGenerator.computeDerivedFields(md);
    }
}
