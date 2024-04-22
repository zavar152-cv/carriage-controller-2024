package ru.itmo.zavar.carriagecontroller.ui.actions.base;

import javafx.scene.control.TableView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.net.CommandSender;
import ru.itmo.zavar.carriagecontroller.carriage.net.InfoReceiver;
import ru.itmo.zavar.carriagecontroller.mqtt.CarriageAsyncClient;
import ru.itmo.zavar.carriagecontroller.ui.data.CarriagePoint;

import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public final class CarriageApplicationEnvironment {
    private TableView<CarriageAction<?>> actionsTable;
    private HashMap<String, CarriagePoint> carriagePoints;
    private ExecutorService executorService;
    private CarriageAsyncClient client;
    private InfoReceiver infoReceiver;
    private CommandSender commandSender;
    private Properties properties;
    private ResourceBundle resourceBundle;
}
