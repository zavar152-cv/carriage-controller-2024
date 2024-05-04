package ru.itmo.zavar.carriagecontroller.carriage.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;

import java.util.ArrayList;

@NoArgsConstructor
public final class ToggleGrappleCarriageAction extends CarriageAction<Boolean> {

    public ToggleGrappleCarriageAction(Boolean actionArgument) {
        super("ToggleGrapple", "...", actionArgument, new CarriageCommand<Void>("reset_module_status", (byte) 1));
    }

    @Override
    public ArrayList<CarriageCommand<?>> toCommands(CarriageInfo carriageInfo) {
        ArrayList<CarriageCommand<?>> commands = new ArrayList<>();
        CarriageCommand<Boolean> toggleGrappleCommand = new CarriageCommand<>("toggle_grapple", (byte) 1);
        toggleGrappleCommand.setArgument(getActionArgument());
        commands.add(toggleGrappleCommand);
        return commands;
    }

    @Override
    public void setOnActionComplete(InfoReceiver infoReceiver, OnActionComplete onActionComplete) {
        infoReceiver.addExternalModuleStatusChangeListener(newValue -> {
            if(newValue.equals((byte) 5)) {
                infoReceiver.removeCarriageInfoChangeListener("ToggleGrappleListener");
                onActionComplete.doComplete();
            }
        }, "ToggleGrappleListener");
    }

    @Override
    public String getArgumentAsReadableString() {
        return "A: " + getActionArgument();
    }
}
