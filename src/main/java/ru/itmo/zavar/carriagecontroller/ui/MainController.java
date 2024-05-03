package ru.itmo.zavar.carriagecontroller.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
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
import javafx.stage.Stage;
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
import ru.itmo.zavar.carriagecontroller.ui.actions.base.ActionUIComponent;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageActionUI;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageApplicationEnvironment;
import ru.itmo.zavar.carriagecontroller.ui.data.CarriagePoint;
import ru.itmo.zavar.carriagecontroller.ui.data.CoordinateBounds;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.AddPointDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.BoundsDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.ConnectionDialog;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
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
    private Button boundsButton, addPointButton, launchButton, connectButton;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private TableView<CarriageAction<?>> actionsTable;
    @FXML
    private Circle circleStatus;
    @FXML
    private Label labelStatus;

    private double minXCoordinate, maxXCoordinate, minYCoordinate, maxYCoordinate;
    private final HashMap<String, CarriagePoint> carriagePoints;
    private final HashMap<String, Circle> drewPoints;
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

    public MainController() throws IOException {
        this.carriagePoints = new HashMap<>();
        this.drewPoints = new HashMap<>();
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
        this.launchButton.setDisable(true);
        this.createStartAndEndPoints();
        this.addPointButton.setOnMouseClicked(this.onAddPointButtonClicked(resourceBundle));
        this.boundsButton.setOnMouseClicked(this.onBoundsButtonClicked(resourceBundle));
        this.connectButton.setOnMouseClicked(this.onConnectButtonClicked(resourceBundle));

        TableColumn<CarriageAction<?>, String> nameColumn = new TableColumn<>(resourceBundle.getString("table.actions"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("actionName"));
        nameColumn.prefWidthProperty().bind(this.actionsTable.widthProperty().multiply(0.5));
        this.actionsTable.getColumns().add(nameColumn);

        TableColumn<CarriageAction<?>, String> argumentColumn = new TableColumn<>(resourceBundle.getString("table.argument"));
        argumentColumn.setCellValueFactory(carriageActionStringCellDataFeatures ->
                new SimpleStringProperty(carriageActionStringCellDataFeatures.getValue().getArgumentAsReadableString()));
        argumentColumn.prefWidthProperty().bind(this.actionsTable.widthProperty().multiply(0.5));
        this.actionsTable.getColumns().add(argumentColumn);

        this.launchButton.setOnMouseClicked(mouseEvent -> {
            if (!this.actionsTable.getItems().isEmpty()) {
                this.executorService.submit(this.programCreatorAndRunner());
            } else {
                CarriageControllerApplication.showWarningDialog(resourceBundle, resourceBundle.getString("dialog.warning.emptyTable"));
            }
        });

        this.environment = new CarriageApplicationEnvironment(actionsTable, carriagePoints, executorService, client,
                infoReceiver, commandSender, properties, resourceBundle);

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
                ConnectionDialog connectionDialog = new ConnectionDialog(this.executorService, resourceBundle, this.client, this.properties);
                Optional<CarriageAsyncClient> carriageAsyncClient = connectionDialog.showAndWait();
                carriageAsyncClient.ifPresent(newClient -> {
                    if(!newClient.isConnected()) {
                        this.labelStatus.setText(resourceBundle.getString("status.offline"));
                        this.circleStatus.setFill(Color.RED);
                        this.infoReceiver.clearAllListeners();
                        this.carriageRectangle.setVisible(false);
                        this.launchButton.setDisable(true);
                        return;
                    }
                    if (this.isClientConnected())
                        this.disconnectClient();
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
                            this.launchButton.setDisable(false);
                            this.labelStatus.setText(resourceBundle.getString("status.online"));
                            this.circleStatus.setFill(Color.GREEN);
                        });

                        task.setOnFailed(workerStateEvent -> carriageIsOffline(resourceBundle));

                        this.executorService.submit(task);

                    } catch (MqttException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
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
//                actionRunner.enableStepMode();
//                actionRunner.addEventListener(e -> {
//                    if (e.equals(ActionRunner.ActionEvent.ACTION_COMPLETE) && actionRunner.isStepModeEnabled())
//                        actionRunner.step();
//                }, "MainListener");
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
        drawPoint(name, x, Color.BLACK);
    }

    private void drawPoint(String name, double x, Paint fill) {
        double mapped = this.map(x, this.minXCoordinate, this.maxXCoordinate, this.ropeLine.getStartX(), this.ropeLine.getEndX());
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

    private void disconnectClient() {
        if (this.isClientConnected()) {
            try {
                this.client.close();
                this.launchButton.setDisable(true);
                log.info("Disconnected from {}...", this.client.getBrokerUrl());
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setupOnClose() {
        primaryStage.setOnCloseRequest(windowEvent -> {
            this.disconnectClient();
            this.executorService.shutdownNow();
            this.scheduledExecutorService.shutdownNow();
            Platform.exit();
            System.exit(0);
        });
    }
}