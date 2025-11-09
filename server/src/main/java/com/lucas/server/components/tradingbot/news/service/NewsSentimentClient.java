package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.mapper.FinbertResponseMapper;
import com.lucas.utils.exception.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Constants.*;

@Component
public class NewsSentimentClient {

    private static final Logger logger = LoggerFactory.getLogger(NewsSentimentClient.class);
    private final FinbertResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String url;

    public NewsSentimentClient(FinbertResponseMapper mapper, HttpRequestClient httpRequestClient,
                               @Value("${sentiment.url}") String url) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.url = url;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public News generateSentiment(News news) throws ClientException, MappingException {
        logger.info(RETRIEVING_DATA_INFO, SENTIMENT, news);
        return mapper.map(httpRequestClient.fetch(url + ANALYZE, news.getHeadline() + " [SEP] " + news.getSummary()), news);
    }
}
