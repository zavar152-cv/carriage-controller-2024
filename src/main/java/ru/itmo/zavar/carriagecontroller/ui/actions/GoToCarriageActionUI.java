package ru.itmo.zavar.carriagecontroller.ui.actions;

import javafx.scene.Node;
import javafx.scene.control.Button;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GoToCarriageAction;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.ActionUIComponent;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageActionUI;
import ru.itmo.zavar.carriagecontroller.ui.actions.base.CarriageApplicationEnvironment;
import ru.itmo.zavar.carriagecontroller.ui.dialogs.GoToCreatorDialog;

import java.util.Optional;
import java.util.ResourceBundle;

@ActionUIComponent
public final class GoToCarriageActionUI implements CarriageActionUI {

    private final Button actionButton = new Button();

    @Override
    public void applyActionEventHandler(CarriageApplicationEnvironment environment) {
        this.actionButton.setOnMouseClicked(mouseEvent -> {
            GoToCreatorDialog goToCreatorDialog = new GoToCreatorDialog(environment.getResourceBundle(), environment.getCarriagePoints());
            Optional<GoToCarriageAction> goToCarriageAction = goToCreatorDialog.showAndWait();
            goToCarriageAction.ifPresent(action -> environment.getActionsTable().getItems().add(action));
        });
    }

    @Override
    public Node getActionNode(ResourceBundle resourceBundle) {
        this.actionButton.setText(resourceBundle.getString("actions.goto"));
        return this.actionButton;
    }
}
