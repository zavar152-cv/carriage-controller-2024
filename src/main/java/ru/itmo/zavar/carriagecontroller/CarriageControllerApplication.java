package ru.itmo.zavar.carriagecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GoToCarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class CarriageControllerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CarriageControllerApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    private static InfoReceiver infoReceiver;

    public static void main(String[] args) throws MqttException {
        //launch();
        try (CarriageAsyncClient client = new CarriageAsyncClient("tcp://localhost:25565", "CC-app", "carriage/commands", "carriage/info")) {
            IMqttToken mqttToken = client.connect();
            mqttToken.waitForCompletion();
            infoReceiver = new InfoReceiver(client);
            infoReceiver.addCarriageInfoChangeListener(newValue -> {
                System.out.println(newValue.getCurrentPosition());
            }, "MainPositionListener");
            CommandSender commandSender = new CommandSender(client);
            LinkedList<CarriageAction<?>> actions = getCarriageActions();
            nextAction(actions, commandSender);
            while (true) ;
        } catch (MqttException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    private static void nextAction(LinkedList<CarriageAction<?>> actions, CommandSender commandSender) {
        System.out.println("next action");
        CarriageAction<?> popped = actions.pop();
        popped.setOnActionComplete(infoReceiver, () -> {
            try {
                commandSender.send(new CarriageCommand<>("reset_status"));
            } catch (JsonProcessingException | MqttException e) {
                throw new RuntimeException(e);
            }
            System.out.println("on position");
            Thread.sleep(500);
            if(!actions.isEmpty())
                nextAction(actions, commandSender);
            else
                System.out.println("task complete");
        });

        ArrayList<CarriageCommand<?>> commands = popped.toCommands(infoReceiver.getCurrentCarriageInfo());
        commands.forEach(carriageCommand -> {
            try {
                System.out.println(carriageCommand.getCommand());
                commandSender.send(carriageCommand);
            } catch (JsonProcessingException | MqttException e) {
                throw new RuntimeException(e);
            }
        });
    }

}