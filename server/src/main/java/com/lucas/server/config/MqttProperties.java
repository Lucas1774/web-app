package com.lucas.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String broker;
    private String username;
    private String password;
    private Map<String, String> topics;
}
