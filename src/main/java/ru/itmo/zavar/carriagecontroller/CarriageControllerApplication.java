package ru.itmo.zavar.carriagecontroller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class CarriageControllerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ResourceBundle langBundle = ResourceBundle.getBundle("ru/itmo/zavar/carriagecontroller/lang/controller", Locale.getDefault());
        FXMLLoader fxmlLoader = new FXMLLoader(CarriageControllerApplication.class.getResource("fxml/main-view.fxml"));
        fxmlLoader.setResources(langBundle);
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);
        stage.setTitle(langBundle.getString("title"));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}