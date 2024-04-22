package ru.itmo.zavar.carriagecontroller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import ru.itmo.zavar.carriagecontroller.ui.MainController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.ResourceBundle;

public final class CarriageControllerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ResourceBundle langBundle = ResourceBundle.getBundle("ru/itmo/zavar/carriagecontroller/lang/controller", Locale.getDefault());
        FXMLLoader fxmlLoader = new FXMLLoader(CarriageControllerApplication.class.getResource("fxml/main-view.fxml"));
        fxmlLoader.setResources(langBundle);
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

        MainController mainController = fxmlLoader.getController();
        mainController.setPrimaryStage(stage);
        mainController.setupOnClose();

        stage.setTitle(langBundle.getString("title"));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void showWarningDialog(ResourceBundle resourceBundle, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(resourceBundle.getString("dialog.warning.title"));
        alert.setHeaderText(resourceBundle.getString("dialog.warning.headerText"));
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showErrorDialog(ResourceBundle resourceBundle, Throwable e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resourceBundle.getString("dialog.error.title"));
            alert.setHeaderText(resourceBundle.getString("dialog.error.headerText"));
            alert.setContentText(e.getMessage());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label(resourceBundle.getString("dialog.error.stacktrace"));

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
            alert.showAndWait();
        });
    }
}