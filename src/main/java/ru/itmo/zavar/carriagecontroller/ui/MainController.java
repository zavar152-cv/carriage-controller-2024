package ru.itmo.zavar.carriagecontroller.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import ru.itmo.zavar.carriagecontroller.carriage.ActionRunner;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GoToCarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.ui.data.CarriagePoint;
import ru.itmo.zavar.carriagecontroller.ui.data.CoordinateBounds;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.AddPointDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.BoundsDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.ConnectionDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.GoToCreatorDialog;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class MainController implements Initializable {
    @FXML
    private Rectangle carriageRectangle;
    @FXML
    private Line ropeLine;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private TilePane actionsTilePane;
    @FXML
    private MenuBar menuBar;
    @FXML
    private Button boundsButton, addPointButton, goToButton, launchButton, connectButton;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private TableView<CarriageAction<?>> actionsTable;

    private double minXCoordinate, maxXCoordinate, minYCoordinate, maxYCoordinate;
    private final HashMap<String, CarriagePoint> carriagePoints;
    private final HashMap<String, Circle> drewPoints;
    private CarriageAsyncClient client;
    private final ExecutorService executorService;
    @Setter
    private Stage primaryStage;

    public MainController() {
        this.carriagePoints = new HashMap<>();
        this.drewPoints = new HashMap<>();
        this.minXCoordinate = 0;
        this.maxXCoordinate = 3000;
        this.minYCoordinate = 0;
        this.maxYCoordinate = 100;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.setCarriageRectanglePosition(0, this.minXCoordinate, this.maxXCoordinate);
        this.createStartAndEndPoints();
        this.addPointButton.setOnMouseClicked(this.onAddPointButtonClicked(resourceBundle));
        this.boundsButton.setOnMouseClicked(this.onBoundsButtonClicked(resourceBundle));
        this.goToButton.setOnMouseClicked(mouseEvent -> { //TODO move to external class
            GoToCreatorDialog goToCreatorDialog = new GoToCreatorDialog(resourceBundle, carriagePoints);
            Optional<GoToCarriageAction> goToCarriageAction = goToCreatorDialog.showAndWait();
            goToCarriageAction.ifPresent(action -> this.actionsTable.getItems().add(action));
        });
        this.connectButton.setOnMouseClicked(this.onConnectButtonClicked(resourceBundle));

        TableColumn<CarriageAction<?>, String> nameColumn = new TableColumn<>(resourceBundle.getString("table.actions"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("actionName"));
        this.actionsTable.getColumns().add(nameColumn);

        TableColumn<CarriageAction<?>, String> argumentColumn = new TableColumn<>(resourceBundle.getString("table.argument"));
        argumentColumn.setCellValueFactory(carriageActionStringCellDataFeatures ->
                new SimpleStringProperty(carriageActionStringCellDataFeatures.getValue().getArgumentAsReadableString()));
        this.actionsTable.getColumns().add(argumentColumn);

        launchButton.setOnMouseClicked(mouseEvent -> {
            new Thread(() -> {
                try (CarriageAsyncClient client = new CarriageAsyncClient("tcp://localhost:25565", "CC-app", "carriage/commands", "carriage/info")) {
                    IMqttToken mqttToken = client.connect();
                    client.addEventListener(e -> {
                        if (e.equals(CarriageAsyncClient.ClientEvent.CONNECTION_LOST)) {
                            log.error("Connection lost: {}, {}", client.getLastConnectionLostThrowable().getMessage(),
                                    client.getLastConnectionLostThrowable().getCause());
                        } else if (e.equals(CarriageAsyncClient.ClientEvent.CONNECT_COMPLETE)) {
                            log.info("Connection complete");
                        }
                    }, "MainListener");
                    mqttToken.waitForCompletion();
                    InfoReceiver infoReceiver = new InfoReceiver(client);
                    infoReceiver.addCurrentPositionChangeListener(newValue -> {
                        log.info("Position: {}", newValue);
                        this.setCarriageRectanglePosition(newValue, minXCoordinate, maxXCoordinate);
                    }, "MainPositionListener");
                    CommandSender commandSender = new CommandSender(client);
                    LinkedList<CarriageAction<?>> actions = new LinkedList<>(actionsTable.getItems());
                    ActionRunner actionRunner = new ActionRunner(infoReceiver, commandSender, actions);
                    actionRunner.enableStepMode();
                    actionRunner.addEventListener(e -> {
                        if (e.equals(ActionRunner.ActionEvent.ACTION_COMPLETE) && actionRunner.isStepModeEnabled())
                            actionRunner.step();
                    }, "MainListener");
                    actionRunner.runAllActions();
                    while (true) ;
                } catch (MqttException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });
    }

    private EventHandler<MouseEvent> onBoundsButtonClicked(ResourceBundle resourceBundle) {
        return mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                BoundsDialog boundsDialog = new BoundsDialog(resourceBundle, this.minXCoordinate,
                        this.maxXCoordinate, this.minYCoordinate, this.maxYCoordinate);
                Optional<CoordinateBounds> carriagePoint = boundsDialog.showAndWait();
                carriagePoint.ifPresent(value -> {
                    this.minXCoordinate = value.minXCoordinate();
                    this.maxXCoordinate = value.maxXCoordinate();
                    this.minYCoordinate = value.minYCoordinate();
                    this.maxYCoordinate = value.maxYCoordinate();
                });
            }
        };
    }

    private EventHandler<MouseEvent> onConnectButtonClicked(ResourceBundle resourceBundle) {
        return mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                ConnectionDialog connectionDialog = new ConnectionDialog(executorService, resourceBundle, this.client);
                Optional<CarriageAsyncClient> carriageAsyncClient = connectionDialog.showAndWait();
                carriageAsyncClient.ifPresent(newClient -> {
                    if (isClientConnected())
                        this.disconnectClient();
                    this.client = newClient;
                });
            }
        };
    }

    private void createStartAndEndPoints() {
        CarriagePoint start = new CarriagePoint("Start", this.minXCoordinate, this.minYCoordinate);
        this.carriagePoints.put(start.name(), start);
        this.drawPoint(start.name(), start.x(), Paint.valueOf("green"));

        CarriagePoint end = new CarriagePoint("End", this.maxXCoordinate, this.minYCoordinate);
        this.carriagePoints.put(end.name(), end);
        this.drawPoint(end.name(), end.x(), Paint.valueOf("green"));
    }

    private EventHandler<MouseEvent> onAddPointButtonClicked(ResourceBundle resourceBundle) {
        return mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                AddPointDialog addPointDialog = new AddPointDialog(resourceBundle, this.minXCoordinate,
                        this.maxXCoordinate, this.minYCoordinate, this.maxYCoordinate, this.carriagePoints.keySet());
                Optional<CarriagePoint> carriagePoint = addPointDialog.showAndWait();
                carriagePoint.ifPresent(value -> {
                    this.carriagePoints.put(value.name(), value);
                    this.drawPoint(value.name(), value.x(), Paint.valueOf("green"));
                });
            }
        };
    }

    private boolean isClientConnected() {
        return this.client != null && this.client.isConnected();
    }

    private void setCarriageRectanglePosition(double position, double minBounds, double maxBounds) {
        double mapped = this.map(position, minBounds, maxBounds, this.ropeLine.getStartX(), this.ropeLine.getEndX());
        this.carriageRectangle.setLayoutY(this.ropeLine.getLayoutY());
        this.carriageRectangle.setLayoutX(mapped + this.ropeLine.getLayoutX() - this.carriageRectangle.getWidth() / 2);
    }

    private void drawPoint(String name, double x) {
        drawPoint(name, x, Paint.valueOf("black"));
    }

    private void drawPoint(String name, double x, Paint fill) {
        double mapped = this.map(x, minXCoordinate, maxXCoordinate, this.ropeLine.getStartX(), this.ropeLine.getEndX());
        Circle point = new Circle(5, fill);
        point.setStrokeWidth(1);
        point.setStroke(Paint.valueOf("black"));
        point.setCenterY(this.ropeLine.getLayoutY());
        point.setCenterX(mapped + this.ropeLine.getLayoutX());
        this.anchorPane.getChildren().add(point);
        this.drewPoints.put(name, point);
    }

    private double map(double x, double inMin, double inMax, double outMin, double outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    private LinkedList<CarriageAction<?>> getCarriageActions() {
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

    private void disconnectClient() {
        if (this.isClientConnected()) {
            try {
                this.client.close();
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setupOnClose() {
        primaryStage.setOnCloseRequest(windowEvent -> {
            this.disconnectClient();
            this.executorService.shutdownNow();
            Platform.exit();
            System.exit(0);
        });
    }
}