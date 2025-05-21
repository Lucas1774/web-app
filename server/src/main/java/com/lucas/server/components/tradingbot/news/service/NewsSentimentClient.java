package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NewsSentimentClient {

    private final RetryableNewsSentimentClientComponent retryableClient;

    public NewsSentimentClient(RetryableNewsSentimentClientComponent retryableClient) {
        this.retryableClient = retryableClient;
    }

    public List<News> generateSentiment(List<News> newsList) throws ClientException, JsonProcessingException {
        List<News> res = new ArrayList<>(newsList.size());
        for (News news : newsList) {
            if (news.getSentiment() != null && news.getSentimentConfidence() != null) {
                continue;
            }
            res.add(retryableClient.generateSentiment(news));
        }
        return res;
    }
}
