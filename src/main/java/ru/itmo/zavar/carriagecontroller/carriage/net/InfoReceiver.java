package ru.itmo.zavar.carriagecontroller.carriage.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class InfoReceiver {
    private final CarriageAsyncClient carriageAsyncClient;
    private final ObjectMapper objectMapper;
    private CarriageInfo previuosCarriageInfo;
    @Getter
    private CarriageInfo currentCarriageInfo;

    private ConcurrentHashMap<String, OnInfoChangeListener<CarriageInfo>> carriageInfoChangeListeners;
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

    private final CountDownLatch firstInfoArrived = new CountDownLatch(1);

    public InfoReceiver(CarriageAsyncClient carriageAsyncClient) throws MqttException, InterruptedException {
        this.carriageAsyncClient = carriageAsyncClient;
        this.objectMapper = new ObjectMapper();
        this.carriageInfoChangeListeners = new ConcurrentHashMap<>();
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

        this.carriageAsyncClient.setOnMessageArrived((s, mqttMessage) -> {
            currentCarriageInfo = objectMapper.readValue(mqttMessage.getPayload(), CarriageInfo.class);
            if (previuosCarriageInfo == null) {
                firstInfoArrived.countDown();
                previuosCarriageInfo = currentCarriageInfo;
            }
            if (!currentCarriageInfo.equals(previuosCarriageInfo)) {
                carriageInfoChangeListeners.forEach((s1, listener) -> {
                    try {
                        listener.onChange(currentCarriageInfo);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
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
        firstInfoArrived.await();
    }

    public void addCarriageInfoChangeListener(OnInfoChangeListener<CarriageInfo> listener, String name) {
        carriageInfoChangeListeners.put(name, listener);
    }

    public void removeCarriageInfoChangeListener(String name) {
        carriageInfoChangeListeners.remove(name);
    }

    @FunctionalInterface
    public interface OnInfoChangeListener<T> {
        void onChange(T newValue) throws InterruptedException;
    }

}
