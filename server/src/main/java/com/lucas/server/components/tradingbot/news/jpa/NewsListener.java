package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.service.NewsSentimentClient;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collections;

import static com.lucas.server.common.Constants.SENTIMENT_GENERATION_FAILED_WARN;

public class NewsListener implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;
    private NewsSentimentClient newsSentimentClient;
    private static final Logger logger = LoggerFactory.getLogger(NewsListener.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @PrePersist
    @PreUpdate
    public void computeDerivedFields(News news) {
        if (null == newsSentimentClient) {
            newsSentimentClient = beanFactory.getBean(NewsSentimentClient.class);
        }
        try {
            newsSentimentClient.generateSentiment(Collections.singletonList(news));
        } catch (ClientException | JsonProcessingException e) {
            logger.warn(SENTIMENT_GENERATION_FAILED_WARN, news, e);
        }
    }
}
