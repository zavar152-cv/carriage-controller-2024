package ru.itmo.zavar.carriagecontroller.carriage.actions;

import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;

public final class GrapplePositionCarriageAction extends CarriageAction<Integer> {

    public GrapplePositionCarriageAction(Integer actionArgument) {
        super("GrapplePosition", "...", actionArgument, new CarriageCommand<Void>("reset_module_status", (byte) 1));
    }

    @Override
    public ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo) {
        ArrayList<CarriageCommand<?>> commands = new ArrayList<>();
        CarriageCommand<Integer> grapplePositionCommand = new CarriageCommand<>("grapple_position", (byte) 1);
        grapplePositionCommand.setArgument(getActionArgument());
        commands.add(grapplePositionCommand);
        return commands;
    }

    @Override
    public void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete) {
        infoReceiver.addExternalModuleStatusChangeListener(newValue -> {
            if(newValue.equals((byte) 3)) {
                infoReceiver.removeCarriageInfoChangeListener("GrapplePositionListener");
                onActionComplete.doComplete();
            }
        }, "GrapplePositionListener");
    }

    @Override
    public String getArgumentAsReadableString() {
        return "V: " + getActionArgument();
    }
}
