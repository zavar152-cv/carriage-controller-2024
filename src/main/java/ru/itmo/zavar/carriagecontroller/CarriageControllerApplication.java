package ru.itmo.zavar.carriagecontroller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;

import java.io.IOException;

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
            client.setOnMessageArrived((s, mqttMessage) -> {
                System.out.println(mqttMessage);
            });
            client.sendMessage("{\"type\":0, \"command\":\"target_position\",\"argument\":800.00}");
            while (true);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
}