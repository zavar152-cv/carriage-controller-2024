package ru.itmo.zavar.carriagecontroller.mqtt;

import lombok.Getter;
import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.ConcurrentHashMap;

public final class CarriageAsyncClient implements AutoCloseable {

    private final MqttAsyncClient mqttAsyncClient;
    private final MqttConnectOptions options;
    private final String commandsPublishTopic;
    private final String infoSubscribeTopic;
    private final ConcurrentHashMap<String, OnEventListener> onEventListeners;
    @Getter
    private final String brokerUrl;
    @Getter
    private Throwable lastConnectionLostThrowable;

    private static final int mqttQos = 2;

    public CarriageAsyncClient(String brokerUrl, String clientId, String commandsPublishTopic, String infoSubscribeTopic) throws MqttException {
        this.mqttAsyncClient = new MqttAsyncClient(brokerUrl, clientId);
        this.options = new MqttConnectOptions();
        this.options.setAutomaticReconnect(true);
        this.options.setCleanSession(true);
        this.options.setConnectionTimeout(10);
        this.commandsPublishTopic = commandsPublishTopic;
        this.infoSubscribeTopic = infoSubscribeTopic;
        this.onEventListeners = new ConcurrentHashMap<>();
        this.brokerUrl = brokerUrl;
        this.mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                onEventListeners.forEach((s1, onEventListener) -> onEventListener.onEvent(ClientEvent.CONNECT_COMPLETE));
            }

            @Override
            public void connectionLost(Throwable throwable) {
                lastConnectionLostThrowable = throwable;
                onEventListeners.forEach((s, onEventListener) -> onEventListener.onEvent(ClientEvent.CONNECTION_LOST));
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                onEventListeners.forEach((s1, onEventListener) -> onEventListener.onEvent(ClientEvent.MESSAGE_ARRIVED));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                onEventListeners.forEach((s, onEventListener) -> onEventListener.onEvent(ClientEvent.DELIVERY_COMPLETE));
            }
        });
    }

    public IMqttToken connect() throws UnsupportedOperationException, MqttException {
        if (!this.mqttAsyncClient.isConnected()) {
            return this.mqttAsyncClient.connect(this.options);
        } else {
            throw new UnsupportedOperationException("Client is already connected");
        }
    }

    public IMqttToken connect(MqttTopic willMqttTopic, byte[] willPayload) throws MqttException, UnsupportedOperationException {
        if (!this.mqttAsyncClient.isConnected()) {
            this.options.setWill(willMqttTopic, willPayload, mqttQos, true);
            return this.mqttAsyncClient.connect(this.options);
        } else {
            throw new UnsupportedOperationException("Client is already connected");
        }
    }

    public boolean isConnected() {
        return this.mqttAsyncClient.isConnected();
    }

    public void sendMessage(String messageText) throws MqttException {
        MqttMessage message = new MqttMessage(messageText.getBytes());
        message.setQos(mqttQos);
        this.mqttAsyncClient.publish(this.commandsPublishTopic, message);
    }

    public void setOnMessageArrived(IMqttMessageListener listener) throws MqttException {
        this.mqttAsyncClient.subscribe(this.infoSubscribeTopic, mqttQos, listener);
    }

    public void addEventListener(OnEventListener onEventListener, String name) {
        this.onEventListeners.put(name, onEventListener);
    }

    public void removeEventListener(String name) {
        this.onEventListeners.remove(name);
    }

    @Override
    public void close() throws MqttException {
        this.mqttAsyncClient.disconnect();
        this.mqttAsyncClient.close();
    }

    public enum ClientEvent {
        CONNECT_COMPLETE,
        CONNECTION_LOST,
        MESSAGE_ARRIVED,
        DELIVERY_COMPLETE
    }

    @FunctionalInterface
    public interface OnEventListener {
        void onEvent(ClientEvent e);
    }
}
