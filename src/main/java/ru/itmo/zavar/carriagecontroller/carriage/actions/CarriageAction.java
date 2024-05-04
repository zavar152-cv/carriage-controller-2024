package ru.itmo.zavar.carriagecontroller.carriage.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public abstract class CarriageAction<T> {
    private String actionName;
    private String actionDescription;
    private T actionArgument;
    private CarriageCommand<?> resetCommand;

    public abstract ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo);
    public abstract void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete);
    @JsonIgnore
    public abstract String getArgumentAsReadableString();

    @FunctionalInterface
    public interface OnActionComplete {
        void doComplete();
    }
}
