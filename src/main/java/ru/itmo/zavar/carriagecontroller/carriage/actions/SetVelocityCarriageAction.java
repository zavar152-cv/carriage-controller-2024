package ru.itmo.zavar.carriagecontroller.carriage.actions;

import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;
import java.util.Objects;

public final class SetVelocityCarriageAction extends CarriageAction<Float> {

    public SetVelocityCarriageAction(Float actionArgument) {
        super("SetVelocity", "...", actionArgument, CarriageCommand.getEmptyCommand());
    }

    @Override
    public ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo) {
        ArrayList<CarriageCommand<?>> commands = new ArrayList<>();
        CarriageCommand<Float> targetSpeedCommand = new CarriageCommand<>("target_speed", (byte) 0);
        targetSpeedCommand.setArgument(getActionArgument());
        commands.add(targetSpeedCommand);
        return commands;
    }

    @Override
    public void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete) {
        infoReceiver.addCarriageInfoChangeListener(newValue -> {
            if(newValue.getTargetSpeed().equals(getActionArgument())) {
                infoReceiver.removeCarriageInfoChangeListener("SetVelocityListener");
                onActionComplete.doComplete();
            }
        }, "SetVelocityListener");
    }

    @Override
    public String getArgumentAsReadableString() {
        return "S: " + getActionArgument();
    }
}
