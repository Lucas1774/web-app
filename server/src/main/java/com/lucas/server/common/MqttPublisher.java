package com.lucas.server.common;

import com.lucas.server.common.exception.ConfigurationException;
import com.lucas.server.config.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class MqttPublisher implements AutoCloseable {

    private final Map<String, String> topics;
    private final MqttClient client;

    public MqttPublisher(MqttProperties props) {
        topics = props.getTopics();
        try {
            client = new MqttClient(props.getBroker(), MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(props.getUsername());
            options.setPassword(props.getPassword().toCharArray());
            client.connect(options);
            for (String t : topics.values()) {
                client.subscribe(t, 1);
                log.info("Subscribed to topic '{}'", t);
            }
        } catch (MqttException e) {
            throw new ConfigurationException(e);
        }
    }

    public void publish(String topic, String payload) {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        try {
            client.publish(topics.get(topic), message);
            log.info("Published message to topic: {}", topic);
        } catch (MqttException e) {
            log.error("Error publishing message to topic: {}", topic, e);
        }
    }

    @Override
    public void close() {
        if (null != client) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                log.warn("Error closing MQTT client", e);
            }
        }
    }
}
