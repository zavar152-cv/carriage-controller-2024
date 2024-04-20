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
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public final class ConnectionDialog extends Dialog<CarriageAsyncClient> {

    private final SimpleStringProperty connectedStringProperty = new SimpleStringProperty();
    private final SimpleStringProperty addressStringProperty = new SimpleStringProperty();
    private CarriageAsyncClient newClient;

    public ConnectionDialog(ExecutorService executorService, ResourceBundle resourceBundle, CarriageAsyncClient client) {
        super.setTitle(resourceBundle.getString("dialog.connection.title"));
        super.setHeaderText(resourceBundle.getString("dialog.connection.headerText"));
        super.setGraphic(new ImageView(Objects.requireNonNull(AddPointDialog.class.getResource("/ru/itmo/zavar/carriagecontroller/img/connect.png")).toString()));
        //ButtonType connectButtonType = new ButtonType(resourceBundle.getString("dialog.connection.connect"), ButtonBar.ButtonData.APPLY);
        ButtonType saveButtonType = new ButtonType(resourceBundle.getString("dialog.connection.save"), ButtonBar.ButtonData.OK_DONE);
        super.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button connectButton = new Button(resourceBundle.getString("dialog.connection.connect"));
        connectButton.setDisable(true);
        Button disconnectButton = new Button(resourceBundle.getString("dialog.connection.disconnect"));
        disconnectButton.setDisable(true);
        Node saveNode = super.getDialogPane().lookupButton(saveButtonType);
        saveNode.setDisable(true);

        connectedStringProperty.set((client != null && client.isConnected()) ?
                resourceBundle.getString("dialog.connection.connected") :
                resourceBundle.getString("dialog.connection.not_connected"));

        addressStringProperty.set(client != null ? client.getBrokerUrl() : "");

        Form form = Form.of(Group.of(
                Field.ofStringType(connectedStringProperty)
                        .bind(connectedStringProperty)
                        .editable(false)
                        .label(resourceBundle.getString("dialog.connection.status")),
                Field.ofStringType(addressStringProperty)
                        .bind(addressStringProperty)
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
                Task<CarriageAsyncClient> connectTask = createNewTask(form, pi);

                connectTask.setOnSucceeded(workerStateEvent -> {
                    this.newClient = connectTask.getValue();
                    saveNode.setDisable(false);
                    disconnectButton.setDisable(false);
                    connectButton.disableProperty().unbind();
                    connectButton.setDisable(true);
                    pi.setVisible(false);
                    connectedStringProperty.set(resourceBundle.getString("dialog.connection.connected"));
                });

                connectTask.setOnFailed(workerStateEvent -> {
                    workerStateEvent.getSource().getException().printStackTrace();
                    pi.setVisible(false);
                    //TODO show error dialog
                });

                connectTask.setOnRunning(workerStateEvent -> {
                    pi.setVisible(true);
                });

                executorService.submit(connectTask);
            }
        });

        disconnectButton.setOnMouseClicked(mouseEvent -> {
            if (this.newClient != null && this.newClient.isConnected()) {
                try {
                    this.newClient.close();
                    disconnectButton.setDisable(true);
                    connectButton.setDisable(false);
                    saveNode.setDisable(true);
                    connectButton.disableProperty().bind(form.validProperty().not());
                    connectedStringProperty.set(resourceBundle.getString("dialog.connection.not_connected"));
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        super.setResultConverter(buttonType -> {
            if (buttonType.equals(saveButtonType)) {
                return newClient;
            }
            return null;
        });
    }


    private Task<CarriageAsyncClient> createNewTask(Form form, ProgressIndicator pi) {
        return new Task<>() {
            @Override
            protected CarriageAsyncClient call() throws Exception {
                form.persist();
                try {
                    CarriageAsyncClient carriageAsyncClient = new CarriageAsyncClient(addressStringProperty.get(), "CC-app", "carriage/commands", "carriage/info");
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