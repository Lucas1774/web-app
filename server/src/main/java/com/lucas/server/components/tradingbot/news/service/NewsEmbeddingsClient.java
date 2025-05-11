package com.lucas.server.components.tradingbot.news.service;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class NewsEmbeddingsClient {

    private final AzureOpenAiEmbeddingModel client;

    public NewsEmbeddingsClient(AzureOpenAiEmbeddingModel client) {
        this.client = client;
    }

    public News embed(News news) throws ClientException {
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
}
