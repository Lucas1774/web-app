package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.mapper.FinbertResponseMapper;
import com.lucas.utils.exception.MappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import static com.lucas.server.common.Constants.ANALYZE;
import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.SENTIMENT;

@Component
@Slf4j
public class NewsSentimentClient {

    private final FinbertResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final String url;

    public NewsSentimentClient(FinbertResponseMapper mapper,
                               HttpRequestClient httpRequestClient,
                               @Value("${sentiment.url}") String url) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.url = url;
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public NewsDomain generateSentiment(NewsDomain news) throws ClientException, MappingException {
        log.info(RETRIEVING_DATA_INFO, SENTIMENT, news);
        return mapper.map(httpRequestClient.fetchFromString(url + ANALYZE,
                news.getHeadline() + " [SEP] " + news.getSummary()), news);
    }
}
