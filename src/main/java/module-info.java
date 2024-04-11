module ru.itmo.zavar.carriagecontroller {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires org.eclipse.paho.client.mqttv3;

    opens ru.itmo.zavar.carriagecontroller to javafx.fxml;
    exports ru.itmo.zavar.carriagecontroller;
    exports ru.itmo.zavar.carriagecontroller.ui;
    opens ru.itmo.zavar.carriagecontroller.ui to javafx.fxml;
}