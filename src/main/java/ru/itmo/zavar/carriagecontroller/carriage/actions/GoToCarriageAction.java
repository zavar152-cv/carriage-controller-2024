package ru.itmo.zavar.carriagecontroller.carriage.actions;

import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;

public final class GoToCarriageAction extends CarriageAction<Float[]> {

    public GoToCarriageAction(Float[] actionArgument) {
        super("GoToPosition", "...", actionArgument, new CarriageCommand<Void>("reset_status", (byte) 0));
    }

    @Override
    public ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo) {
        ArrayList<CarriageCommand<?>> commands = new ArrayList<>();
        Float currentPosition = carriageInfo.getCurrentPosition();
        CarriageCommand<Byte> directionCommand = new CarriageCommand<>("direction", (byte) 0);
        if (getActionArgument()[0] >= currentPosition) {
            directionCommand.setArgument((byte) 0);
        } else {
            directionCommand.setArgument((byte) 1);
        }
        commands.add(directionCommand);
        CarriageCommand<Float> targetSpeedCommand = new CarriageCommand<>("target_speed", (byte) 0);
        targetSpeedCommand.setArgument(getActionArgument()[1]);
        commands.add(targetSpeedCommand);
        CarriageCommand<Float> targetPositionCommand = new CarriageCommand<>("target_position", (byte) 0);
        targetPositionCommand.setArgument(getActionArgument()[0]);
        commands.add(targetPositionCommand);
        CarriageCommand<Float> enableMotorsCommand = new CarriageCommand<>("enable_motors", (byte) 0);
        commands.add(enableMotorsCommand);
        return commands;
    }

    @Override
    public void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete) {
        infoReceiver.addCarriageInfoChangeListener(newValue -> {
            if(newValue.getCurrentStatus() == 3 && newValue.getTargetSpeed() == 0
                    && newValue.getCurrentBSpeed() == 0
                    && newValue.getCurrentASpeed() == 0) {
                infoReceiver.removeCarriageInfoChangeListener("GoToListener");
                onActionComplete.doComplete();
            }
        }, "GoToListener");
    }

    @Override
    public String getArgumentAsReadableString() {
        return "X: " + getActionArgument()[0] +
                ", S: " +
                getActionArgument()[1];
    }

}
