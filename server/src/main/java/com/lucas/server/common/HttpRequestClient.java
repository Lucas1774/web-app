package com.lucas.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.exception.ClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;

@Component
public class HttpRequestClient {

    private final RestTemplate restTemplate;
    private final DocumentBuilderFactory documentBuilderFactory;

    public HttpRequestClient(RestTemplate restTemplate, DocumentBuilderFactory documentBuilderFactory) {
        this.restTemplate = restTemplate;
        this.documentBuilderFactory = documentBuilderFactory;
    }

    private static void mockUserAgent(HttpHeaders headers) {
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/115.0.0.0 Safari/537.36"
        );
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public JsonNode fetch(String url, boolean mockUserAgent) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (mockUserAgent) mockUserAgent(headers);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class).getBody();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public JsonNode fetch(String url, String body) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class).getBody();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public JsonNode fetch(String url, String apiKey, JsonNode body) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<JsonNode> request = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class).getBody();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public Document fetchXml(String url) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
        mockUserAgent(headers);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            return documentBuilderFactory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(Objects.requireNonNull(
                            restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody()
                    ).getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
