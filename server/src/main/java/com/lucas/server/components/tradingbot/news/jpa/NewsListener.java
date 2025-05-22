package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static com.lucas.server.common.Constants.EMBEDDING_GENERATION_FAILED_WARN;

public class NewsListener implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;
    private NewsEmbeddingsClient newsEmbeddingsClient;
    private static boolean active = true;
    private static final Logger logger = LoggerFactory.getLogger(NewsListener.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @PrePersist
    @PreUpdate
    public void computeDerivedFields(News news) {
        if (!active) {
            return;
        }
        if (null == newsEmbeddingsClient) {
            newsEmbeddingsClient = beanFactory.getBean(NewsEmbeddingsClient.class);
        }
        try {
            newsEmbeddingsClient.embed(news);
        } catch (ClientException e) {
            logger.warn(EMBEDDING_GENERATION_FAILED_WARN, news, e);
        }
    }

    public static void setActive(boolean active) {
        NewsListener.active = active;
    }
}
