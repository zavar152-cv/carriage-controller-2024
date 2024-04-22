package ru.itmo.zavar.carriagecontroller.ui.actions.base;

import javafx.scene.Node;

import java.util.ResourceBundle;

public interface CarriageActionUI {
    void applyActionEventHandler(CarriageApplicationEnvironment carriageApplicationEnvironment);
    Node getActionNode(ResourceBundle resourceBundle);
}
