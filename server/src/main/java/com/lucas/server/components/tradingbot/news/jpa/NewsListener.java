package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class NewsListener implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;
    private NewsEmbeddingsClient newsEmbeddingsClient;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @PrePersist
    @PreUpdate
    public void computeDerivedFields(News news) {
        if (null == newsEmbeddingsClient) {
            newsEmbeddingsClient = beanFactory.getBean(NewsEmbeddingsClient.class);
        }
        newsEmbeddingsClient.embed(news);
    }
}
