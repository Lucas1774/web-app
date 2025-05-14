package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.mapper.FinbertResponseMapper;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NewsSentimentClient {

    private final FinbertResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String url;
    private static final Logger logger = LoggerFactory.getLogger(NewsSentimentClient.class);

    public NewsSentimentClient(FinbertResponseMapper mapper, HttpRequestClient httpRequestClient,
                               @Value("${sentiment.url}") String url) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.url = url;
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = Constants.REQUEST_MAX_ATTEMPTS)
    private News generateSentiment(News news) throws ClientException, JsonProcessingException {
        logger.info(Constants.GENERATING_SENTIMENT_INFO, news);
        return mapper.map(this.httpRequestClient.fetch(url + Constants.ANALYZE, news.getHeadline() + " [SEP] " + news.getSummary()), news);
    }

    public List<News> generateSentiment(List<News> newsList) throws ClientException, JsonProcessingException {
        List<News> res = new ArrayList<>(newsList.size());
        for (News news : newsList) {
            res.add(generateSentiment(news));
        }
        return res;
    }
}
