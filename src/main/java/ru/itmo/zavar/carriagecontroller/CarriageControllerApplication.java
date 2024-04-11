package ru.itmo.zavar.carriagecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.carriage.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageCommand;

import java.io.IOException;
import java.util.Random;
import java.util.random.RandomGenerator;

public class CarriageControllerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CarriageControllerApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws MqttException {
        //launch();
        try (CarriageAsyncClient client = new CarriageAsyncClient("tcp://localhost:25565", "CC-app", "carriage/commands", "carriage/info")) {
            IMqttToken mqttToken = client.connect();
            mqttToken.waitForCompletion();
            InfoReceiver infoReceiver = new InfoReceiver(client);
            infoReceiver.setTargetSpeedChangeListener(System.out::println);
            CommandSender commandSender = new CommandSender(client);
            CarriageCommand<Float> targetSpeed = new CarriageCommand<>("target_speed");
            RandomGenerator generator = RandomGenerator.getDefault();
            while (true) {
                targetSpeed.setArgument(60.0f + generator.nextFloat());
                Thread.sleep(100);
                commandSender.send(targetSpeed);
            }

            //client.sendMessage("{\"type\":0, \"command\":\"target_position\",\"argument\":800.00}");
        } catch (MqttException | JsonProcessingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}