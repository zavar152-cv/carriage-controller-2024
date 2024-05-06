package ru.itmo.zavar.carriagecontroller.carriage.actions;

import lombok.NoArgsConstructor;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;

@NoArgsConstructor
public class ModeCarriageAction extends CarriageAction<Boolean> {
    public ModeCarriageAction(Boolean actionArgument) {
        super("Mode", "...", actionArgument, CarriageCommand.getEmptyCommand());
    }

    @Override
    public ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo) {
        ArrayList<CarriageCommand<?>> commands = new ArrayList<>();
        CarriageCommand<Boolean> modeCommand = new CarriageCommand<>("mode", (byte) 0);
        modeCommand.setArgument(getActionArgument());
        commands.add(modeCommand);
        CarriageCommand<Boolean> moduleModeCommand = new CarriageCommand<>("module_mode", (byte) 1);
        moduleModeCommand.setArgument(getActionArgument());
        commands.add(moduleModeCommand);
        return commands;
    }

    @Override
    public void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete) {
        infoReceiver.addCarriageInfoChangeListener(newValue -> {
            if (newValue.getExternalModuleStatus().equals((byte) 6) && newValue.getCurrentStatus().equals((byte) 8) && getActionArgument()) {
                infoReceiver.removeCarriageInfoChangeListener("ModeListener");
                onActionComplete.doComplete();
            } else if (newValue.getExternalModuleStatus().equals((byte) 1) && newValue.getCurrentStatus().equals((byte) 2) && !getActionArgument()) {
                infoReceiver.removeCarriageInfoChangeListener("ModeListener");
                onActionComplete.doComplete();
            }
        }, "ModeListener");
    }

    @Override
    public String getArgumentAsReadableString() {
        return "M: " + getActionArgument();
    }
}
