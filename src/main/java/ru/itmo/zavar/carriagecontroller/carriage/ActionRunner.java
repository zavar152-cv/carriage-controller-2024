package ru.itmo.zavar.carriagecontroller.carriage;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;

import java.util.ArrayList;
import java.util.LinkedList;

@Log4j2
public final class ActionRunner {
    private final InfoReceiver infoReceiver;
    private final CommandSender commandSender;
    private final LinkedList<CarriageAction<?>> actions;
    @Setter
    private OnEventListener onEventListener;

    public ActionRunner(InfoReceiver infoReceiver, CommandSender commandSender, LinkedList<CarriageAction<?>> actions) {
        this.infoReceiver = infoReceiver;
        this.commandSender = commandSender;
        this.actions = actions;
        this.onEventListener = e -> {
        };
    }

    public void runAllActions() {
        nextAction(this.actions, this.commandSender);
    }

    private void nextAction(LinkedList<CarriageAction<?>> actions, CommandSender commandSender) {
        CarriageAction<?> popped = actions.pop();
        this.onEventListener.onEvent(ActionEvent.NEXT_ACTION);
        log.info("Starting next action {} with argument {}", popped.getActionName(), popped.getActionArgument());
        popped.setOnActionComplete(this.infoReceiver, () -> {
            try {
                commandSender.send(popped.getResetCommand());
                this.onEventListener.onEvent(ActionEvent.ACTION_COMPLETE);
                log.info("Action {} with argument {} completed", popped.getActionName(), popped.getActionArgument());
                Thread.sleep(500);
                if (!actions.isEmpty())
                    nextAction(actions, commandSender);
                else {
                    this.onEventListener.onEvent(ActionEvent.TASK_COMPLETE);
                    log.info("All actions completed");
                }
            } catch (JsonProcessingException | MqttException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        ArrayList<CarriageCommand<?>> commands = popped.toCommands(this.infoReceiver.getCurrentCarriageInfo());
        commands.forEach(carriageCommand -> {
            try {
                log.info("Send command {} with argument {}", carriageCommand.getCommand(), carriageCommand.getArgument());
                commandSender.send(carriageCommand);
            } catch (JsonProcessingException | MqttException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public enum ActionEvent {
        NEXT_ACTION,
        ACTION_COMPLETE,
        TASK_COMPLETE
    }

    @FunctionalInterface
    public interface OnEventListener {
        void onEvent(ActionEvent e);
    }

}
