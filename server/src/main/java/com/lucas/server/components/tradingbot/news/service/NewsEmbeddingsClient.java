package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.lucas.server.common.Constants.*;

@Component
public class NewsEmbeddingsClient {

    private final AzureOpenAiEmbeddingModel client;
    private static final Logger logger = LoggerFactory.getLogger(NewsEmbeddingsClient.class);

    public NewsEmbeddingsClient(AzureOpenAiEmbeddingModel client) {
        this.client = client;
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public News embed(News news) throws ClientException {
        logger.info(GENERATING_EMBEDDINGS_INFO, news);
        String contentToEmbed = news.getHeadline();
        if (news.getSummary() != null && !news.getSummary().isEmpty()) {
            contentToEmbed += " " + news.getSummary();
        }

        try {
            float[] embeddings = client.embed(contentToEmbed);
            if (embeddings.length > 0) {
                backOff(EMBEDDINGS_BACKOFF_MILLIS);
            }

            return news.setEmbeddings(embeddings);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public List<News> embed(List<News> newsList) throws ClientException {
        List<News> embeddedNews = new ArrayList<>(newsList.size());
        for (News news : newsList) {
            embeddedNews.add(embed(news));
        }
        return embeddedNews;
    }
}
