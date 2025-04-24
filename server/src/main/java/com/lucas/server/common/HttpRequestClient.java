package com.lucas.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class HttpRequestClient {

    private final RestTemplate restTemplate;

    public HttpRequestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode fetch(String url) throws ClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            return this.restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class).getBody();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
