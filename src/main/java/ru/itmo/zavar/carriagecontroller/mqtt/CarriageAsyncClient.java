package ru.itmo.zavar.carriagecontroller.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.*;

public final class CarriageAsyncClient implements AutoCloseable {

    private final MqttAsyncClient mqttAsyncClient;
    private final MqttConnectOptions options;
    private final String commandsPublishTopic;
    private final String infoSubscribeTopic;
    @Setter
    private OnEventListener onEventListener;
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
        this.onEventListener = e -> {
        };
        this.mqttAsyncClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                onEventListener.onEvent(ClientEvent.CONNECT_COMPLETE);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                lastConnectionLostThrowable = throwable;
                onEventListener.onEvent(ClientEvent.CONNECTION_LOST);
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                onEventListener.onEvent(ClientEvent.MESSAGE_ARRIVED);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                onEventListener.onEvent(ClientEvent.DELIVERY_COMPLETE);
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
        mqttAsyncClient.publish(commandsPublishTopic, message);
    }

    public void setOnMessageArrived(IMqttMessageListener listener) throws MqttException {
        mqttAsyncClient.subscribe(infoSubscribeTopic, mqttQos, listener);
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
