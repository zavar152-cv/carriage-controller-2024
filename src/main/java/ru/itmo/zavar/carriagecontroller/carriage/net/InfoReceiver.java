package ru.itmo.zavar.carriagecontroller.carriage.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class InfoReceiver {
    private final CarriageAsyncClient carriageAsyncClient;
    private final ObjectMapper objectMapper;
    private CarriageInfo previuosCarriageInfo;
    @Getter
    private CarriageInfo currentCarriageInfo;
    @Getter
    private boolean ready = false;

    private final ConcurrentHashMap<String, OnInfoChangeListener<CarriageInfo>> carriageInfoChangeListeners;
    private final ConcurrentHashMap<String, OnInfoChangeListener<Boolean>> directionChangeListeners;
    private final ConcurrentHashMap<String, OnInfoChangeListener<Float>> targetSpeedChangeListeners;
    private final ConcurrentHashMap<String, OnInfoChangeListener<Float>> targetPositionChangeListeners;
    private final ConcurrentHashMap<String, OnInfoChangeListener<Float>> currentPositionChangeListeners;
    private final ConcurrentHashMap<String, OnInfoChangeListener<Byte>> currentStatusChangeListeners;
    private final ConcurrentHashMap<String, OnInfoChangeListener<Byte>> externalModuleStatusChangeListeners;

    private final CountDownLatch firstInfoArrived = new CountDownLatch(1);

    public InfoReceiver(CarriageAsyncClient carriageAsyncClient) throws MqttException, InterruptedException {
        this.carriageAsyncClient = carriageAsyncClient;
        this.objectMapper = new ObjectMapper();
        this.carriageInfoChangeListeners = new ConcurrentHashMap<>();
        this.directionChangeListeners = new ConcurrentHashMap<>();
        this.targetSpeedChangeListeners = new ConcurrentHashMap<>();
        this.targetPositionChangeListeners = new ConcurrentHashMap<>();
        this.currentPositionChangeListeners = new ConcurrentHashMap<>();
        this.currentStatusChangeListeners = new ConcurrentHashMap<>();
        this.externalModuleStatusChangeListeners = new ConcurrentHashMap<>();

        this.carriageAsyncClient.addOnMessageArrived((s, mqttMessage) -> {
            currentCarriageInfo = objectMapper.readValue(mqttMessage.getPayload(), CarriageInfo.class);
            if (previuosCarriageInfo == null) {
                firstInfoArrived.countDown();
                previuosCarriageInfo = currentCarriageInfo;
            }
            if (!currentCarriageInfo.equals(previuosCarriageInfo))
                carriageInfoChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo));
            if (!currentCarriageInfo.getDirection().equals(previuosCarriageInfo.getDirection()))
                directionChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo.getDirection()));
            if (!currentCarriageInfo.getTargetSpeed().equals(previuosCarriageInfo.getTargetSpeed()))
                targetSpeedChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo.getTargetSpeed()));
            if (!currentCarriageInfo.getTargetPosition().equals(previuosCarriageInfo.getTargetPosition()))
                targetPositionChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo.getTargetPosition()));
            if (!currentCarriageInfo.getCurrentPosition().equals(previuosCarriageInfo.getCurrentPosition()))
                currentPositionChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo.getCurrentPosition()));
            if (!currentCarriageInfo.getCurrentStatus().equals(previuosCarriageInfo.getCurrentStatus()))
                currentStatusChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo.getCurrentStatus()));
            if (!currentCarriageInfo.getExternalModuleStatus().equals(previuosCarriageInfo.getExternalModuleStatus()))
                externalModuleStatusChangeListeners.forEach((s1, listener) -> listener.onChange(currentCarriageInfo.getExternalModuleStatus()));
            previuosCarriageInfo = currentCarriageInfo;
        }, "InfoReceiver");
    }

    public boolean waitForFirstResult() throws InterruptedException {
        boolean await = firstInfoArrived.await(2500, TimeUnit.MILLISECONDS);
        ready = true;
        return await;
    }

    public void clearAllListeners() {
        carriageInfoChangeListeners.clear();
        directionChangeListeners.clear();
        targetSpeedChangeListeners.clear();
        targetPositionChangeListeners.clear();
        currentPositionChangeListeners.clear();
        currentStatusChangeListeners.clear();
        externalModuleStatusChangeListeners.clear();
    }

    public void addCarriageInfoChangeListener(OnInfoChangeListener<CarriageInfo> listener, String name) {
        carriageInfoChangeListeners.put(name, listener);
    }

    public void removeCarriageInfoChangeListener(String name) {
        carriageInfoChangeListeners.remove(name);
    }

    public void addDirectionChangeListener(OnInfoChangeListener<Boolean> listener, String name) {
        directionChangeListeners.put(name, listener);
    }

    public void removeDirectionChangeListener(String name) {
        directionChangeListeners.remove(name);
    }

    public void addTargetSpeedChangeListener(OnInfoChangeListener<Float> listener, String name) {
        targetSpeedChangeListeners.put(name, listener);
    }

    public void removeTargetSpeedChangeListener(String name) {
        targetSpeedChangeListeners.remove(name);
    }

    public void addTargetPositionChangeListener(OnInfoChangeListener<Float> listener, String name) {
        targetPositionChangeListeners.put(name, listener);
    }

    public void removeTargetPositionChangeListener(String name) {
        targetPositionChangeListeners.remove(name);
    }

    public void addCurrentPositionChangeListener(OnInfoChangeListener<Float> listener, String name) {
        currentPositionChangeListeners.put(name, listener);
    }

    public void removeCurrentPositionChangeListener(String name) {
        currentPositionChangeListeners.remove(name);
    }

    public void addCurrentStatusChangeListener(OnInfoChangeListener<Byte> listener, String name) {
        currentStatusChangeListeners.put(name, listener);
    }

    public void removeCurrentStatusChangeListener(String name) {
        currentStatusChangeListeners.remove(name);
    }

    public void addExternalModuleStatusChangeListener(OnInfoChangeListener<Byte> listener, String name) {
        externalModuleStatusChangeListeners.put(name, listener);
    }

    public void removeExternalModuleStatusChangeListener(String name) {
        externalModuleStatusChangeListeners.remove(name);
    }

    @FunctionalInterface
    public interface OnInfoChangeListener<T> {
        void onChange(T newValue);
    }

}
