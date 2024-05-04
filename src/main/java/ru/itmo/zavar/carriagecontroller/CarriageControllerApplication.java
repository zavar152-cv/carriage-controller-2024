package ru.itmo.zavar.carriagecontroller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import ru.itmo.zavar.carriagecontroller.ui.MainController;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public final class CarriageControllerApplication extends Application {

    @Getter
    private static final Image appIcon;

    static {
        appIcon = new Image(Objects.requireNonNull(
                CarriageControllerApplication.class
                        .getResourceAsStream("/ru/itmo/zavar/carriagecontroller/img/icon.png")));
    }

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
        stage.getIcons().add(appIcon);
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
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(appIcon);
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
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(appIcon);
            alert.showAndWait();
        });
    }
}