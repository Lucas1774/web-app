package com.lucas.server.components.tradingbot.news.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.lucas.server.common.ClientException;
import com.lucas.server.components.tradingbot.news.jpa.News;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
public class NewsEmbeddingsClient {

    private final OpenAIClient client;
    private final String model;

    public NewsEmbeddingsClient(OpenAIClient client,
                                @Value("${spring.ai.azure.openai.embeddings.options.deployment-name}") String model) {
        this.client = client;
        this.model = model;
    }

    public News embed(News news) throws ClientException {
        String contentToEmbed = news.getHeadline();
        if (news.getSummary() != null && !news.getSummary().isEmpty()) {
            contentToEmbed += " " + news.getSummary();
        }
        EmbeddingsOptions options = new EmbeddingsOptions(List.of(contentToEmbed))
                .setModel(model);

        try {
            float[] embeddings = client.getEmbeddings(model, options)
                    .getData()
                    .stream()
                    .findFirst()
                    .map(item -> {
                        List<Float> vector = item.getEmbedding();
                        float[] array = new float[vector.size()];
                        IntStream.range(0, vector.size())
                                .forEach(i -> array[i] = vector.get(i));
                        return array;
                    })
                    .orElse(new float[0]);

            return news.setEmbeddings(embeddings);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
