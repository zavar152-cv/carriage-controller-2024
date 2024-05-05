package ru.itmo.zavar.carriagecontroller.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import ru.itmo.zavar.carriagecontroller.CarriageControllerApplication;
import ru.itmo.zavar.carriagecontroller.carriage.ActionRunner;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.mqtt.pojo.CarriageInfo;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.ActionUIComponent;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageActionUI;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageApplicationEnvironment;
import ru.itmo.zavar.carriagecontroller.ui.data.*;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.AddPointDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.BoundsDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.ConnectionDialog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

@Log4j2
public final class MainController implements Initializable {
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
    private MenuItem saveMenuItem, openMenuItem, exitMenuItem, launchMenuItem, connectionMenuItem;
    @FXML
    private RadioMenuItem stepRadioMenuItem;
    @FXML
    private Button boundsButton, addPointButton, nextStepButton;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private TableView<CarriageAction<?>> actionsTable;
    @FXML
    private Circle circleStatus;
    @FXML
    private Label labelStatus, currentActionLabel, directionLabel, targetSpeedLabel,
            targetPositionLabel, currentASpeedLabel, currentBSpeedLabel, currentPositionLabel,
            currentStatusLabel, externalModuleStatusLabel;

    private double minXCoordinate, maxXCoordinate, minYCoordinate, maxYCoordinate;
    private final HashMap<String, CarriagePoint> carriagePoints;
    private final HashMap<String, Circle> drewPoints;
    private final HashMap<String, Tooltip> tooltips;
    private CarriageAsyncClient client;
    private final ExecutorService executorService;
    @Setter
    private Stage primaryStage;
    private volatile long lastMessageArrivedTime;
    private final ScheduledExecutorService scheduledExecutorService;
    private InfoReceiver infoReceiver;
    private CommandSender commandSender;
    private Properties properties;
    private final Set<BeanDefinition> actionsBeanDefinition;
    private CarriageApplicationEnvironment environment;
    private final FileChooser fileDialog = new FileChooser();

    public MainController() throws IOException {
        this.carriagePoints = new HashMap<>();
        this.drewPoints = new HashMap<>();
        this.tooltips = new HashMap<>();
        this.minXCoordinate = 0;
        this.maxXCoordinate = 3000;
        this.minYCoordinate = 0;
        this.maxYCoordinate = 100;
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.properties = new Properties();
        this.properties.load(Objects.requireNonNull(MainController.class.getResource("/ru/itmo/zavar/carriagecontroller/settings.properties")).openStream());
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(ActionUIComponent.class));
        this.actionsBeanDefinition = provider.findCandidateComponents("ru.itmo.zavar.carriagecontroller.ui.actions");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.actionsTable.setPlaceholder(new Label(resourceBundle.getString("table.placeholder")));
        this.labelStatus.setText(resourceBundle.getString("status.offline"));
        this.circleStatus.setFill(Color.RED);
        this.scheduledExecutorService.scheduleAtFixedRate(timeoutChecker(resourceBundle), 0, Long.parseLong(this.properties.getProperty("info.timeout")), TimeUnit.MILLISECONDS);
        this.setCarriageRectanglePosition(0, this.minXCoordinate, this.maxXCoordinate);
        this.carriageRectangle.setVisible(false);
        this.launchMenuItem.setDisable(true);
        this.createStartAndEndPoints();
        this.addPointButton.setOnMouseClicked(this.onAddPointButtonClicked(resourceBundle));
        this.boundsButton.setOnMouseClicked(this.onBoundsButtonClicked(resourceBundle));
        this.connectionMenuItem.setOnAction(this.onConnectButtonClicked(resourceBundle));

        TableColumn<CarriageAction<?>, String> nameColumn = new TableColumn<>(resourceBundle.getString("table.actions"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("actionName"));
        nameColumn.prefWidthProperty().bind(this.actionsTable.widthProperty().multiply(0.5));
        this.actionsTable.getColumns().add(nameColumn);

        TableColumn<CarriageAction<?>, String> argumentColumn = new TableColumn<>(resourceBundle.getString("table.argument"));
        argumentColumn.setCellValueFactory(carriageActionStringCellDataFeatures ->
                new SimpleStringProperty(carriageActionStringCellDataFeatures.getValue().getArgumentAsReadableString()));
        argumentColumn.prefWidthProperty().bind(this.actionsTable.widthProperty().multiply(0.5));
        this.actionsTable.getColumns().add(argumentColumn);

        this.launchMenuItem.setOnAction(actionEvent -> {
            if (!this.actionsTable.getItems().isEmpty()) {
                this.executorService.submit(this.programCreatorAndRunner());
            } else {
                CarriageControllerApplication.showWarningDialog(resourceBundle, resourceBundle.getString("dialog.warning.emptyTable"));
            }
        });

        this.environment = new CarriageApplicationEnvironment(this.actionsTable, this.carriagePoints, this.executorService, this.client,
                this.infoReceiver, this.commandSender, this.properties, resourceBundle);

        this.actionsBeanDefinition.forEach(beanDefinition -> {
            Class<?> c;
            try {
                c = Class.forName(beanDefinition.getBeanClassName());
                Constructor<?> cons = c.getConstructor();
                CarriageActionUI actionUi = (CarriageActionUI) cons.newInstance();
                Node actionNode = actionUi.getActionNode(resourceBundle);
                this.actionsTilePane.getChildren().add(actionNode);
                actionUi.applyActionEventHandler(this.environment);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        this.saveMenuItem.setOnAction(this.onSaveMenuItem());
        this.openMenuItem.setOnAction(this.onOpenMenuItem());
        this.exitMenuItem.setOnAction(actionEvent -> {
            this.primaryStage.fireEvent(
                    new WindowEvent(this.primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        this.ropeLine.setOnMouseEntered(mouseEvent -> {
            this.tooltips.forEach((s, tooltip) -> {
                Circle circle = this.drewPoints.get(s);
                tooltip.show(circle, this.primaryStage.getX() + circle.getCenterX(), this.primaryStage.getY() + circle.getCenterY());
            });
        });

        this.ropeLine.setOnMouseExited(mouseEvent -> {
            this.tooltips.forEach((s, tooltip) -> {
                tooltip.hide();
            });
        });
    }

    private EventHandler<ActionEvent> onSaveMenuItem() {
        return actionEvent -> {
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("ru.itmo.zavar.carriagecontroller.carriage.actions")
                    .allowIfSubType("java.util.LinkedList")
                    .allowIfSubType("java.util.HashMap")
                    .build();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
            String s;
            try {
                s = objectMapper.writeValueAsString(new MapData(this.carriagePoints, this.minXCoordinate,
                        this.maxXCoordinate, this.minYCoordinate,
                        this.maxYCoordinate, new LinkedList<>(this.actionsTable.getItems())));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            this.fileDialog.setInitialFileName("mapData.json");
            this.fileDialog.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json*"));
            File file = this.fileDialog.showSaveDialog(this.primaryStage);
            if (file == null)
                return;
            try {
                Files.write(file.toPath(), s.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private EventHandler<ActionEvent> onOpenMenuItem() {
        return actionEvent -> {
            this.fileDialog.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json*"));
            File file = this.fileDialog.showOpenDialog(this.primaryStage);
            if (file == null)
                return;

            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("ru.itmo.zavar.carriagecontroller.carriage.actions")
                    .allowIfSubType("java.util.LinkedList")
                    .allowIfSubType("java.util.HashMap")
                    .build();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
            try {
                MapData mapData = objectMapper.readValue(file, MapData.class);
                this.minXCoordinate = mapData.getMinXCoordinate();
                this.maxXCoordinate = mapData.getMaxXCoordinate();
                this.minYCoordinate = mapData.getMinYCoordinate();
                this.maxYCoordinate = mapData.getMaxYCoordinate();
                this.actionsTable.getItems().setAll(mapData.getActions());
                this.drewPoints.forEach((s1, circle) -> {
                    this.anchorPane.getChildren().remove(circle);
                });
                this.drewPoints.clear();
                this.carriagePoints.clear();
                this.createStartAndEndPoints();
                mapData.getCarriagePoints().forEach((s, carriagePoint) -> {
                    if (!s.equals("Start") && !s.equals("End")) {
                        this.carriagePoints.put(s, carriagePoint);
                        this.drawPoint(s, carriagePoint.x(), Color.VIOLET);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
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
                    this.drewPoints.forEach((s1, circle) -> {
                        this.anchorPane.getChildren().remove(circle);
                    });
                    this.createStartAndEndPoints();
                    drewPoints.forEach((s, circle) -> {
                        if (!s.equals("Start") && !s.equals("End")) {
                            this.drawPoint(s, carriagePoints.get(s).x(), Color.VIOLET);
                        }
                    });
                });
            }
        };
    }

    private EventHandler<ActionEvent> onConnectButtonClicked(ResourceBundle resourceBundle) {
        return actionEvent -> {
            ConnectionDialog connectionDialog = new ConnectionDialog(this.executorService, resourceBundle, this.client, this.properties);
            Optional<CarriageAsyncClient> carriageAsyncClient = connectionDialog.showAndWait();
            carriageAsyncClient.ifPresent(newClient -> {
                if (!newClient.isConnected()) {
                    this.labelStatus.setText(resourceBundle.getString("status.offline"));
                    this.circleStatus.setFill(Color.RED);
                    this.infoReceiver.clearAllListeners();
                    this.carriageRectangle.setVisible(false);
                    this.launchMenuItem.setDisable(true);
                    return;
                }
                this.client = newClient;
                this.environment.setClient(this.client);
                this.client.addOnMessageArrived(this.onCarriageMessageArrived(), "MainController");
                try {
                    if (this.infoReceiver != null)
                        this.infoReceiver.clearAllListeners();
                    this.infoReceiver = new InfoReceiver(this.client);
                    this.environment.setInfoReceiver(this.infoReceiver);

                    Task<Void> task = this.firstInfoArrived();

                    task.setOnRunning(workerStateEvent -> {
                        this.labelStatus.setText(resourceBundle.getString("status.pinging"));
                        this.circleStatus.setFill(Color.YELLOW);
                    });

                    task.setOnSucceeded(workerStateEvent -> {
                        this.infoReceiver.addCurrentPositionChangeListener(this.onCarriagePositionChange(), "MainPositionListener");
                        this.commandSender = new CommandSender(this.client);
                        this.environment.setCommandSender(this.commandSender);
                        Float currentPosition = this.infoReceiver.getCurrentCarriageInfo().getCurrentPosition();
                        this.setCarriageRectanglePosition(currentPosition, this.minXCoordinate, this.maxXCoordinate);
                        this.carriageRectangle.setVisible(true);
                        this.launchMenuItem.setDisable(false);
                        this.labelStatus.setText(resourceBundle.getString("status.online"));
                        this.circleStatus.setFill(Color.GREEN);
                        this.updateCarriageInfoLabels(this.infoReceiver.getCurrentCarriageInfo());
                        this.infoReceiver.addCarriageInfoChangeListener(newValue ->
                                        Platform.runLater(() -> this.updateCarriageInfoLabels(newValue)),
                                "MainInfoListenerLabels");
                        this.actionsTable.setMouseTransparent(false);
                        this.currentActionLabel.setText("");
                        this.actionsTable.getSelectionModel().clearSelection();
                    });

                    task.setOnFailed(workerStateEvent -> carriageIsOffline(resourceBundle));

                    this.executorService.submit(task);

                } catch (MqttException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        };
    }

    private void updateCarriageInfoLabels(CarriageInfo carriageInfo) {
        this.directionLabel.setText("Direction: " + (carriageInfo.getDirection() ? "<<<" : ">>>"));
        this.targetSpeedLabel.setText("Target speed: " + carriageInfo.getTargetSpeed().toString());
        this.targetPositionLabel.setText("Target position: " + carriageInfo.getTargetPosition().toString());
        this.currentASpeedLabel.setText("Current A speed: " + carriageInfo.getCurrentASpeed().toString());
        this.currentBSpeedLabel.setText("Current B speed: " + carriageInfo.getCurrentBSpeed().toString());
        this.currentPositionLabel.setText("Current position: " + carriageInfo.getCurrentPosition().toString());
        this.currentStatusLabel.setText("Current status: " + CarriageStatus.values()[carriageInfo.getCurrentStatus()]);
        this.externalModuleStatusLabel.setText("External module status: " + ExternalModuleStatus.values()[carriageInfo.getExternalModuleStatus()]);
    }

    private Task<Void> firstInfoArrived() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean waited = infoReceiver.waitForFirstResult();
                if (!waited)
                    throw new TimeoutException();
                return null;
            }
        };
    }

    private IMqttMessageListener onCarriageMessageArrived() {
        return (s, mqttMessage) -> {
            this.lastMessageArrivedTime = System.currentTimeMillis();
        };
    }

    private Runnable timeoutChecker(ResourceBundle resourceBundle) {
        return () -> {
            if (System.currentTimeMillis() - this.lastMessageArrivedTime > 5000 && isClientConnected() && this.infoReceiver.isReady()) {
                this.lastMessageArrivedTime = 0;
                Platform.runLater(() -> this.carriageIsOffline(resourceBundle));
            }
        };
    }

    private Runnable programCreatorAndRunner() {
        return () -> {
            LinkedList<CarriageAction<?>> actions = new LinkedList<>(this.actionsTable.getItems());
            ActionRunner actionRunner = new ActionRunner(this.infoReceiver, this.commandSender, actions);
            this.actionsTable.setMouseTransparent(true);
            if (this.stepRadioMenuItem.isSelected()) {
                this.stepRadioMenuItem.setDisable(true);
                this.nextStepButton.setDisable(true);
                this.nextStepButton.setVisible(true);
                actionRunner.enableStepMode();
                actionRunner.addEventListener(e -> {
                    if (e.equals(ActionRunner.ActionEvent.ACTION_COMPLETE))
                        this.nextStepButton.setDisable(false);
                    if (e.equals(ActionRunner.ActionEvent.NEXT_ACTION)) {
                        this.nextStepButton.setDisable(true);
                    }
                    if (e.equals(ActionRunner.ActionEvent.TASK_COMPLETE)) {
                        this.nextStepButton.setDisable(true);
                        this.nextStepButton.setVisible(false);
                        this.stepRadioMenuItem.setDisable(false);
                        actionRunner.removeEventListener("MainListenerStep");
                    }
                }, "MainListenerStep");
                this.nextStepButton.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                        actionRunner.step();
                    }
                });
            }


            actionRunner.addEventListener(e -> {
                if (e.equals(ActionRunner.ActionEvent.NEXT_ACTION)) {
                    Platform.runLater(() -> {
                        this.actionsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                        this.actionsTable.requestFocus();
                        this.actionsTable.getSelectionModel().select(actionRunner.getCurrentAction());
                        this.actionsTable.getFocusModel().focus(this.actionsTable.getSelectionModel().getSelectedIndex());
                        this.currentActionLabel.setText(actionRunner.getCurrentAction().getActionName()
                                + " " + actionRunner.getCurrentAction().getArgumentAsReadableString());
                    });
                }
                if (e.equals(ActionRunner.ActionEvent.TASK_COMPLETE)) {
                    Platform.runLater(() -> {
                        this.actionsTable.setMouseTransparent(false);
                        this.launchMenuItem.setDisable(false);
                        this.currentActionLabel.setText("");
                        this.actionsTable.getSelectionModel().clearSelection();
                        actionRunner.removeEventListener("MainListener");
                    });
                }
            }, "MainListener");

            this.launchMenuItem.setDisable(true);
            actionRunner.runAllActions();
        };
    }

    private void carriageIsOffline(ResourceBundle resourceBundle) {
        this.labelStatus.setText(resourceBundle.getString("status.offline"));
        this.circleStatus.setFill(Color.RED);
        this.disconnectClient();
        CarriageControllerApplication.showErrorDialog(resourceBundle, new IllegalStateException(resourceBundle.getString("dialog.error.carriageOffline")));
    }

    private InfoReceiver.OnInfoChangeListener<Float> onCarriagePositionChange() {
        return newValue -> {
            log.info("Position: {}", newValue);
            this.setCarriageRectanglePosition(newValue, this.minXCoordinate, this.maxXCoordinate);
        };
    }

    private void createStartAndEndPoints() {
        CarriagePoint start = new CarriagePoint("Start", this.minXCoordinate, this.minYCoordinate);
        this.carriagePoints.put(start.name(), start);
        this.drawPoint(start.name(), start.x(), Color.ORANGE);

        CarriagePoint end = new CarriagePoint("End", this.maxXCoordinate, this.minYCoordinate);
        this.carriagePoints.put(end.name(), end);
        this.drawPoint(end.name(), end.x(), Color.ORANGE);
    }

    private EventHandler<MouseEvent> onAddPointButtonClicked(ResourceBundle resourceBundle) {
        return mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                AddPointDialog addPointDialog = new AddPointDialog(resourceBundle, this.minXCoordinate,
                        this.maxXCoordinate, this.minYCoordinate, this.maxYCoordinate, this.carriagePoints.keySet());
                Optional<CarriagePoint> carriagePoint = addPointDialog.showAndWait();
                carriagePoint.ifPresent(value -> {
                    this.carriagePoints.put(value.name(), value);
                    this.drawPoint(value.name(), value.x(), Color.VIOLET);
                });
            }
        };
    }

    private synchronized boolean isClientConnected() {
        return this.client != null && this.client.isConnected();
    }

    private void setCarriageRectanglePosition(double position, double minBounds, double maxBounds) {
        double mapped = this.map(position, minBounds, maxBounds, this.ropeLine.getStartX(), this.ropeLine.getEndX());
        this.carriageRectangle.setLayoutY(this.ropeLine.getLayoutY());
        this.carriageRectangle.setLayoutX(mapped + this.ropeLine.getLayoutX() - this.carriageRectangle.getWidth() / 2);
    }

    private void drawPoint(String name, double x) {
        this.drawPoint(name, x, Color.BLACK);
    }

    private void drawPoint(String name, double x, Paint fill) {
        double mapped = this.map(x, this.minXCoordinate, this.maxXCoordinate, this.ropeLine.getStartX(), this.ropeLine.getEndX());
        Circle point = new Circle(5, fill);
        point.setStrokeWidth(1);
        point.setStroke(Paint.valueOf("black"));
        point.setCenterY(this.ropeLine.getLayoutY());
        point.setCenterX(mapped + this.ropeLine.getLayoutX());
        Tooltip tooltip = new Tooltip("%s (%s)".formatted(name, x));

        point.setOnMousePressed(mouseEvent -> {
            tooltip.show(point, this.primaryStage.getX() + point.getCenterX(), this.primaryStage.getY() + point.getCenterY());
        });

        point.setOnMouseReleased(mouseEvent -> {
            tooltip.hide();
        });

        this.tooltips.put(name, tooltip);
        this.anchorPane.getChildren().add(point);
        this.drewPoints.put(name, point);
    }

    private void clearAllPoints() {
        this.drewPoints.forEach((s, circle) -> {
            this.anchorPane.getChildren().remove(circle);
        });
        this.drewPoints.clear();
        this.carriagePoints.clear();
        this.tooltips.clear();
    }

    private void clearPoint(String name) {
        Circle circle = this.drewPoints.get(name);
        this.anchorPane.getChildren().remove(circle);
        this.drewPoints.remove(name);
        this.carriagePoints.remove(name);
        this.tooltips.remove(name);
    }

    private double map(double x, double inMin, double inMax, double outMin, double outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    private void disconnectClient() {
        if (this.isClientConnected()) {
            try {
                this.client.removeOnMessageArrived("MainController");
                this.infoReceiver.removeCurrentPositionChangeListener("MainPositionListener");
                this.infoReceiver.removeCarriageInfoChangeListener("MainInfoListenerLabels");
                this.client.close();
                this.launchMenuItem.setDisable(true);
                log.info("Disconnected from {}...", this.client.getBrokerUrl());
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setupOnClose() {
        this.primaryStage.setOnCloseRequest(windowEvent -> {
            this.disconnectClient();
            this.executorService.shutdownNow();
            this.scheduledExecutorService.shutdownNow();
            Platform.exit();
            System.exit(0);
        });
    }
}