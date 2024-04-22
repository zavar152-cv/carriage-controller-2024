package ru.itmo.zavar.carriagecontroller.ui.actions;

import javafx.scene.Node;
import javafx.scene.control.Button;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.ActionUIComponent;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageActionUI;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageApplicationEnvironment;

import java.util.ResourceBundle;

@ActionUIComponent
public final class TestActionUI implements CarriageActionUI {

    private final Button actionButton = new Button("Test");

    @Override
    public void applyActionEventHandler(CarriageApplicationEnvironment carriageApplicationEnvironment) {
        this.actionButton.setOnMouseClicked(mouseEvent -> {
            System.out.println(carriageApplicationEnvironment.getClient());
        });
    }

    @Override
    public Node getActionNode(ResourceBundle resourceBundle) {
        return this.actionButton;
    }
}
