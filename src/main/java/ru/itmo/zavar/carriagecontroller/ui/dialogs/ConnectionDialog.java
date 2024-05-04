package ru.itmo.zavar.carriagecontroller.ui.dialogs;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.CarriageControllerApplication;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;

import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@Log4j2
public final class ConnectionDialog extends Dialog<CarriageAsyncClient> {

    private final SimpleStringProperty connectedStringProperty = new SimpleStringProperty();
    private final SimpleStringProperty addressStringProperty = new SimpleStringProperty(); //tcp://localhost:25565
    private CarriageAsyncClient newClient;

    public ConnectionDialog(ExecutorService executorService, ResourceBundle resourceBundle, CarriageAsyncClient client, Properties properties) {
        super.setTitle(resourceBundle.getString("dialog.connection.title"));
        super.setHeaderText(resourceBundle.getString("dialog.connection.headerText"));
        super.setGraphic(new ImageView(Objects.requireNonNull(AddPointDialog.class.getResource("/ru/itmo/zavar/carriagecontroller/img/connect.png")).toString()));
        Stage stage = (Stage) super.getDialogPane().getScene().getWindow();
        stage.getIcons().add(CarriageControllerApplication.getAppIcon());
        ButtonType saveButtonType = new ButtonType(resourceBundle.getString("dialog.connection.save"), ButtonBar.ButtonData.OK_DONE);
        super.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button connectButton = new Button(resourceBundle.getString("dialog.connection.connect"));
        connectButton.setDisable(true);
        Button disconnectButton = new Button(resourceBundle.getString("dialog.connection.disconnect"));
        disconnectButton.setDisable(true);
        Node saveNode = super.getDialogPane().lookupButton(saveButtonType);
        saveNode.setDisable(true);

        this.connectedStringProperty.set((client != null && client.isConnected()) ?
                resourceBundle.getString("dialog.connection.connected") :
                resourceBundle.getString("dialog.connection.notConnected"));

        this.addressStringProperty.set(client != null ? client.getBrokerUrl() : "");

        Form form = Form.of(Group.of(
                Field.ofStringType(this.connectedStringProperty)
                        .bind(this.connectedStringProperty)
                        .editable(false)
                        .label(resourceBundle.getString("dialog.connection.status")),
                Field.ofStringType(this.addressStringProperty)
                        .bind(this.addressStringProperty)
                        .validate(StringLengthValidator.atLeast(1, resourceBundle.getString("dialog.connection.addressError")))
                        .required(true)
                        .label(resourceBundle.getString("dialog.connection.address"))
        ));
        if (client != null && client.isConnected()) {
            this.newClient = client;
            disconnectButton.setDisable(false);
        } else {
            connectButton.disableProperty().bind(form.validProperty().not());
        }

        ProgressIndicator pi = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        pi.setVisible(false);
        VBox vBox = new VBox(new FormRenderer(form), connectButton, disconnectButton, pi);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(5);
        super.getDialogPane().setPrefWidth(600);
        super.getDialogPane().setContent(vBox);

        connectButton.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                Task<CarriageAsyncClient> connectTask = createNewConnectionTask(form, pi, properties);

                connectTask.setOnSucceeded(workerStateEvent -> {
                    log.info("Connected to {}", addressStringProperty.get());
                    this.newClient = connectTask.getValue();
                    saveNode.setDisable(false);
                    disconnectButton.setDisable(false);
                    connectButton.disableProperty().unbind();
                    connectButton.setDisable(true);
                    pi.setVisible(false);
                    this.connectedStringProperty.set(resourceBundle.getString("dialog.connection.connected"));
                });

                connectTask.setOnFailed(workerStateEvent -> {
                    log.error("Connection to {} failed", addressStringProperty.get());
                    pi.setVisible(false);
                    CarriageControllerApplication.showErrorDialog(resourceBundle, workerStateEvent.getSource().getException());
                });

                connectTask.setOnRunning(workerStateEvent -> {
                    log.info("Connecting...");
                    pi.setVisible(true);
                });

                executorService.submit(connectTask);
            }
        });

        disconnectButton.setOnMouseClicked(mouseEvent -> {
            if (this.newClient != null && this.newClient.isConnected()) {
                log.info("Disconnecting from {}...", this.addressStringProperty.get());
                try {
                    this.newClient.close();
                    disconnectButton.setDisable(true);
                    connectButton.setDisable(false);
                    saveNode.setDisable(false);
                    connectButton.disableProperty().bind(form.validProperty().not());
                    this.connectedStringProperty.set(resourceBundle.getString("dialog.connection.notConnected"));
                    log.info("Disconnected from {}...", this.addressStringProperty.get());
                } catch (MqttException e) {
                    CarriageControllerApplication.showErrorDialog(resourceBundle, e);
                    throw new RuntimeException(e);
                }
            }
        });

        super.setResultConverter(buttonType -> {
            if (buttonType.equals(saveButtonType)) {
                return this.newClient;
            } else if (buttonType.equals(ButtonType.CANCEL)) {
                if (this.newClient != null && this.newClient.isConnected()) {
                    try {
                        this.newClient.close();
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("Disconnected from {}...", addressStringProperty.get());
                }
                return null;
            }
            return null;
        });
    }


    private Task<CarriageAsyncClient> createNewConnectionTask(Form form, ProgressIndicator pi, Properties properties) {
        return new Task<>() {
            @Override
            protected CarriageAsyncClient call() throws Exception {
                form.persist();
                try {
                    CarriageAsyncClient carriageAsyncClient = new CarriageAsyncClient(addressStringProperty.get(),
                            properties.getProperty("mqtt.clientId"), properties.getProperty("mqtt.commandsPublishTopic"), properties.getProperty("mqtt.infoSubscribeTopic"));
                    IMqttToken mqttToken = carriageAsyncClient.connect();
                    pi.setDisable(false);
                    mqttToken.waitForCompletion();
                    return carriageAsyncClient;
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
