package com.lucas.server.components.tradingbot.common;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatRequestMessage;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

public class AIClient {

    private final ChatCompletionsClient client;
    private final Function<List<ChatRequestMessage>, ChatCompletionsOptions> optionsProvider;
    @Getter
    private final String modelName;

    public AIClient(ChatCompletionsClient client, Function<List<ChatRequestMessage>, ChatCompletionsOptions> optionsProvider, String modelName) {
        this.client = client;
        this.optionsProvider = optionsProvider;
        this.modelName = modelName;
    }

    public ChatCompletions complete(List<ChatRequestMessage> prompt) {
        return client.complete(optionsProvider.apply(prompt));
    }
}
