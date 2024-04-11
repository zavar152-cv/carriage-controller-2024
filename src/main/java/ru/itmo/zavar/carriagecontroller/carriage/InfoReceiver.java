package ru.itmo.zavar.carriagecontroller.carriage;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.Objects;

public final class InfoReceiver {
    private final CarriageAsyncClient carriageAsyncClient;
    private final ObjectMapper objectMapper;
    private CarriageInfo previuosCarriageInfo;

    @Setter
    private OnInfoChangeListener<CarriageInfo> carriageInfoChangeListener;
    @Setter
    private OnInfoChangeListener<Boolean> directionChangeListener;
    @Setter
    private OnInfoChangeListener<Float> targetSpeedChangeListener;
    @Setter
    private OnInfoChangeListener<Float> targetPositionChangeListener;
    @Setter
    private OnInfoChangeListener<Float> currentPositionChangeListener;
    @Setter
    private OnInfoChangeListener<Byte> currentStatusChangeListener;

    public InfoReceiver(CarriageAsyncClient carriageAsyncClient) throws MqttException {
        this.carriageAsyncClient = carriageAsyncClient;
        this.objectMapper = new ObjectMapper();
        this.carriageInfoChangeListener = (v) -> {
        };
        this.directionChangeListener = (v) -> {
        };
        this.targetSpeedChangeListener = (v) -> {
        };
        this.targetPositionChangeListener = (v) -> {
        };
        this.currentPositionChangeListener = (v) -> {
        };
        this.currentStatusChangeListener = (v) -> {
        };

        carriageAsyncClient.setOnMessageArrived((s, mqttMessage) -> {
            CarriageInfo currentCarriageInfo = objectMapper.readValue(mqttMessage.getPayload(), CarriageInfo.class);
            if (previuosCarriageInfo == null)
                previuosCarriageInfo = currentCarriageInfo;
            if (!currentCarriageInfo.equals(previuosCarriageInfo))
                carriageInfoChangeListener.onChange(currentCarriageInfo);
            if (!currentCarriageInfo.getDirection().equals(previuosCarriageInfo.getDirection()))
                directionChangeListener.onChange(currentCarriageInfo.getDirection());
            if (!currentCarriageInfo.getTargetSpeed().equals(previuosCarriageInfo.getTargetSpeed()))
                targetSpeedChangeListener.onChange(currentCarriageInfo.getTargetSpeed());
            if (!currentCarriageInfo.getTargetPosition().equals(previuosCarriageInfo.getTargetPosition()))
                targetPositionChangeListener.onChange(currentCarriageInfo.getTargetPosition());
            if (!currentCarriageInfo.getCurrentPosition().equals(previuosCarriageInfo.getCurrentPosition()))
                currentPositionChangeListener.onChange(currentCarriageInfo.getCurrentPosition());
            if (!currentCarriageInfo.getCurrentStatus().equals(previuosCarriageInfo.getCurrentStatus()))
                currentStatusChangeListener.onChange(currentCarriageInfo.getCurrentStatus());
            previuosCarriageInfo = currentCarriageInfo;
        });
    }

    @FunctionalInterface
    public interface OnInfoChangeListener<T> {
        void onChange(T newValue);
    }

}
