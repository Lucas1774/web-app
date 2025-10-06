package com.lucas.server.common;

import com.lucas.server.common.exception.ConfigurationException;
import com.lucas.server.config.MqttProperties;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MqttPublisher implements AutoCloseable {

    private final Map<String, String> topics;
    private final MqttClient client;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPublisher.class);

    public MqttPublisher(MqttProperties props) {
        this.topics = props.getTopics();
        try {
            client = new MqttClient(props.getBroker(), MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(props.getUsername());
            options.setPassword(props.getPassword().toCharArray());
            client.connect(options);
            for (String t : topics.values()) {
                client.subscribe(t, 1);
                logger.info("Subscribed to topic '{}'", t);
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
            logger.info("Published message to topic: {}", topic);
        } catch (MqttException e) {
            logger.error("Error publishing message to topic: {}", topic, e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() throws Exception {
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                logger.warn("Error closing MQTT client", e);
            }
        }
    }
}
