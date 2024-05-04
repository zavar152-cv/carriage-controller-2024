package ru.itmo.zavar.carriagecontroller.ui.actions;

import javafx.scene.Node;
import javafx.scene.control.Button;
import ru.itmo.zavar.carriagecontroller.carriage.actions.ToggleGrappleCarriageAction;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.ActionUIComponent;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageActionUI;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageApplicationEnvironment;

import java.util.ResourceBundle;

@ActionUIComponent
public final class CloseGrappleCarriageActionUI implements CarriageActionUI {

    private final Button actionButton = new Button();

    @Override
    public void applyActionEventHandler(CarriageApplicationEnvironment environment) {
        this.actionButton.setOnMouseClicked(mouseEvent -> {
            environment.getActionsTable().getItems().add(new ToggleGrappleCarriageAction(false));
        });
    }

    @Override
    public Node getActionNode(ResourceBundle resourceBundle) {
        this.actionButton.setText(resourceBundle.getString("actions.closeGrapple"));
        return this.actionButton;
    }
}
