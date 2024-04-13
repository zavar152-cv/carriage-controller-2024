package ru.itmo.zavar.carriagecontroller.carriage.actions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;

@AllArgsConstructor
@Getter
public abstract class CarriageAction<T> {
    private final String actionName;
    private final String actionDescription;
    private final T actionArgument;

    public abstract ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo);
    public abstract void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete);

    @FunctionalInterface
    public interface OnActionComplete {
        void doComplete() throws InterruptedException;
    }
}
