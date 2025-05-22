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

import java.util.List;

import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;

@Component
public class HttpRequestClient {

    private final RestTemplate restTemplate;

    public HttpRequestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public JsonNode fetch(String url) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
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
}
