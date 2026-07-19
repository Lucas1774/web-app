package com.lucas.server.common;

import com.lucas.server.common.exception.ClientException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import tools.jackson.databind.JsonNode;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HttpRequestClient {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(1);

    private final WebClient webClient;
    private final DocumentBuilderFactory documentBuilderFactory;

    public JsonNode get(String url, boolean mockUserAgent) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (mockUserAgent) {
            mockUserAgent(headers);
        }
        try {
            return webClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(RESPONSE_TIMEOUT)
                    .block();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public Document get(String url) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
        mockUserAgent(headers);
        try {
            return documentBuilderFactory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(Objects.requireNonNull(webClient.get()
                            .uri(url)
                            .headers(h -> h.addAll(headers))
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(RESPONSE_TIMEOUT)
                            .block()).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public JsonNode post(String url, String body) throws ClientException {
        try {
            return webClient.post()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(RESPONSE_TIMEOUT)
                    .block();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public JsonNode post(String url, @Nullable String apiKey, JsonNode body, boolean mockUserAgent)
            throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (null != apiKey && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
        if (mockUserAgent) {
            mockUserAgent(headers);
        }
        try {
            return webClient.post()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(RESPONSE_TIMEOUT)
                    .block();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    private static void mockUserAgent(HttpHeaders headers) {
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/115.0.0.0 Safari/537.36");
    }
}
