package ru.itmo.zavar.carriagecontroller.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.carriage.ActionRunner;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GoToCarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;

import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

@Log4j2
public class HelloController implements Initializable {
    @FXML
    private Rectangle rectangle;

    @FXML
    private Line line;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        double mapped = map(0, 0, 4000, line.getStartX(), line.getEndX());
        rectangle.setLayoutY(line.getLayoutY());
        rectangle.setLayoutX(mapped + line.getLayoutX() - rectangle.getWidth() / 2);
        new Thread(() -> {
            try (CarriageAsyncClient client = new CarriageAsyncClient("tcp://localhost:25565", "CC-app", "carriage/commands", "carriage/info")) {
                IMqttToken mqttToken = client.connect();
                client.setOnEventListener(e -> {
                    if(e.equals(CarriageAsyncClient.ClientEvent.CONNECTION_LOST)) {
                        log.error("Connection lost: {}, {}", client.getLastConnectionLostThrowable().getMessage(),
                                client.getLastConnectionLostThrowable().getCause());
                    } else if(e.equals(CarriageAsyncClient.ClientEvent.CONNECT_COMPLETE)) {
                        log.info("Connection complete");
                    }
                });
                mqttToken.waitForCompletion();
                InfoReceiver infoReceiver = new InfoReceiver(client);
                infoReceiver.addCurrentPositionChangeListener(newValue -> {
                    log.info("Position: {}", newValue);
                    double mapped2 = map(newValue, 0, 4000, line.getStartX(), line.getEndX());
                    rectangle.setLayoutY(line.getLayoutY());
                    rectangle.setLayoutX(mapped2 + line.getLayoutX() - rectangle.getWidth() / 2);
                }, "MainPositionListener");
                CommandSender commandSender = new CommandSender(client);
                LinkedList<CarriageAction<?>> actions = getCarriageActions();
                ActionRunner actionRunner = new ActionRunner(infoReceiver, commandSender, actions);
                actionRunner.enableStepMode();
                actionRunner.setOnEventListener(e -> {
                    if(e.equals(ActionRunner.ActionEvent.ACTION_COMPLETE) && actionRunner.isStepModeEnabled())
                        actionRunner.step();
                });
                actionRunner.runAllActions();
                while (true);
            } catch (MqttException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private double map(double x, double in_min, double in_max, double out_min, double out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    private static LinkedList<CarriageAction<?>> getCarriageActions() {
        LinkedList<CarriageAction<?>> actions = new LinkedList<>();
        GoToCarriageAction goToCarriageAction1 = new GoToCarriageAction(new Float[]{1000.0f, 60.0f});
        GoToCarriageAction goToCarriageAction2 = new GoToCarriageAction(new Float[]{0000.0f, 60.0f});
        GoToCarriageAction goToCarriageAction3 = new GoToCarriageAction(new Float[]{2000.0f, 60.0f});
        GoToCarriageAction goToCarriageAction4 = new GoToCarriageAction(new Float[]{1500.0f, 60.0f});
        GoToCarriageAction goToCarriageAction5 = new GoToCarriageAction(new Float[]{0000.0f, 80.0f});
        GoToCarriageAction goToCarriageAction6 = new GoToCarriageAction(new Float[]{3000.0f, 80.0f});
        actions.add(goToCarriageAction1);
        actions.add(goToCarriageAction2);
        actions.add(goToCarriageAction3);
        actions.add(goToCarriageAction4);
        actions.add(goToCarriageAction5);
        actions.add(goToCarriageAction6);
        return actions;
    }
}