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
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    opens ru.itmo.zavar.carriagecontroller to javafx.fxml;
    exports ru.itmo.zavar.carriagecontroller;
    exports ru.itmo.zavar.carriagecontroller.ui;
    opens ru.itmo.zavar.carriagecontroller.ui to javafx.fxml;
    opens ru.itmo.zavar.carriagecontroller.mqtt.pojo to com.fasterxml.jackson.databind;
}