package ru.itmo.zavar.carriagecontroller.carriage.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;

public final class CommandSender {
    private final CarriageAsyncClient carriageAsyncClient;
    private final ObjectMapper objectMapper;

    public CommandSender(CarriageAsyncClient carriageAsyncClient) {
        this.carriageAsyncClient = carriageAsyncClient;
        this.objectMapper = new ObjectMapper();
    }

    public void send(CarriageCommand<?> command) throws JsonProcessingException, MqttException {
        String value = objectMapper.writeValueAsString(command);
        carriageAsyncClient.sendMessage(value);
    }

}
