package ru.itmo.zavar.carriagecontroller.ui.actions;

import javafx.scene.Node;
import javafx.scene.control.Button;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GoToCarriageAction;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GrapplePositionCarriageAction;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.ActionUIComponent;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageActionUI;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageApplicationEnvironment;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.GoToCreatorDialog;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.GrapplePositionDialog;

import java.util.Optional;
import java.util.ResourceBundle;

@ActionUIComponent
public final class GrapplePositionCarriageActionUI implements CarriageActionUI {

    private final Button actionButton = new Button();

    @Override
    public void applyActionEventHandler(CarriageApplicationEnvironment environment) {
        this.actionButton.setOnMouseClicked(mouseEvent -> {
            GrapplePositionDialog grapplePositionDialog = new GrapplePositionDialog(environment.getResourceBundle());
            Optional<Integer> grapplePosition = grapplePositionDialog.showAndWait();
            grapplePosition.ifPresent(value -> environment.getActionsTable().getItems().add(new GrapplePositionCarriageAction(value)));
        });
    }

    @Override
    public Node getActionNode(ResourceBundle resourceBundle) {
        this.actionButton.setText(resourceBundle.getString("actions.grapplePosition"));
        return this.actionButton;
    }
}
