package ru.itmo.zavar.carriagecontroller.carriage;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public final class ActionRunner {
    private final InfoReceiver infoReceiver;
    private final CommandSender commandSender;
    private final LinkedList<CarriageAction<?>> actions;
    private final ConcurrentHashMap<String, OnEventListener> onEventListeners;
    @Getter
    private boolean stepModeEnabled = false;
    private ActionEvent previousEvent;
    @Getter
    private CarriageAction<?> currentAction;

    public ActionRunner(InfoReceiver infoReceiver, CommandSender commandSender, LinkedList<CarriageAction<?>> actions) {
        this.infoReceiver = infoReceiver;
        this.commandSender = commandSender;
        this.actions = actions;
        this.onEventListeners = new ConcurrentHashMap<>();
        this.previousEvent = ActionEvent.IDLE;
    }

    public void enableStepMode() {
        this.stepModeEnabled = true;
    }

    public void disableStepMode() {
        this.stepModeEnabled = false;
    }

    public void step() {
        if(!this.stepModeEnabled)
            throw new UnsupportedOperationException("Step mode is disabled");

        this.nextAction(this.actions, this.commandSender);
    }

    public void runAllActions() {
        if(!this.previousEvent.equals(ActionEvent.IDLE))
            throw new UnsupportedOperationException("Runner is not in IDLE mode");

        this.nextAction(this.actions, this.commandSender);
    }

    private void nextAction(LinkedList<CarriageAction<?>> actions, CommandSender commandSender) {
        CarriageAction<?> popped = actions.pop();
        this.currentAction = popped;
        this.onEventListeners.forEach((s, onEventListener) -> {
            onEventListener.onEvent(ActionEvent.NEXT_ACTION);
        });
        this.previousEvent = ActionEvent.NEXT_ACTION;
        log.info("Starting next action {} with argument {}", popped.getActionName(), popped.getActionArgument());
        popped.setOnActionComplete(this.infoReceiver, () -> {
            try {
                CarriageCommand<?> resetCommand = popped.getResetCommand();
                if(!CarriageCommand.isEmpty(resetCommand))
                    commandSender.send(resetCommand);
                if(actions.isEmpty())
                    this.disableStepMode();
                this.onEventListeners.forEach((s, onEventListener) -> {
                    onEventListener.onEvent(ActionEvent.ACTION_COMPLETE);
                });
                this.previousEvent = ActionEvent.ACTION_COMPLETE;
                log.info("Action {} with argument {} completed", popped.getActionName(), popped.getActionArgument());
                Thread.sleep(500);
                if (!actions.isEmpty()) {
                    if(!this.stepModeEnabled)
                        this.nextAction(actions, commandSender);
                } else {
                    this.onEventListeners.forEach((s, onEventListener) -> {
                        onEventListener.onEvent(ActionEvent.TASK_COMPLETE);
                    });
                    this.previousEvent = ActionEvent.TASK_COMPLETE;
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

    public void addEventListener(OnEventListener onEventListener, String name) {
        this.onEventListeners.put(name, onEventListener);
    }

    public void removeEventListener(String name) {
        this.onEventListeners.remove(name);
    }


    public enum ActionEvent {
        IDLE,
        NEXT_ACTION,
        ACTION_COMPLETE,
        TASK_COMPLETE
    }

    @FunctionalInterface
    public interface OnEventListener {
        void onEvent(ActionEvent e);
    }

}
