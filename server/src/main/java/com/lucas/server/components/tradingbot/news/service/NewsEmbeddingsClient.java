package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NewsEmbeddingsClient {

    private final AzureOpenAiEmbeddingModel client;
    private static final Logger logger = LoggerFactory.getLogger(NewsEmbeddingsClient.class);

    public NewsEmbeddingsClient(AzureOpenAiEmbeddingModel client) {
        this.client = client;
    }

    public News embed(News news) throws ClientException {
        logger.info(Constants.GENERATING_EMBEDDINGS_INFO, news);
        String contentToEmbed = news.getHeadline();
        if (news.getSummary() != null && !news.getSummary().isEmpty()) {
            contentToEmbed += " " + news.getSummary();
        }

        try {
            float[] embeddings = client.embed(contentToEmbed);
            if (embeddings.length > 0) {
                Thread.sleep(4000);
            }

            return news.setEmbeddings(embeddings);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return news;
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public List<News> embed(List<News> newsList) throws ClientException {
        List<News> embeddedNews = new ArrayList<>(newsList.size());
        for (News news : newsList) {
            embeddedNews.add(embed(news));
        }
        return embeddedNews;
    }
}
