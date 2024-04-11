module ru.itmo.zavar.carriagecontroller2024 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;

    opens ru.itmo.zavar.carriagecontroller2024 to javafx.fxml;
    exports ru.itmo.zavar.carriagecontroller2024;
}