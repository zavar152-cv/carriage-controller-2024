package ru.itmo.zavar.carriagecontroller.ui.dialogs;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.validators.DoubleRangeValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import ru.itmo.zavar.carriagecontroller.carriage.actions.GoToCarriageAction;
import ru.itmo.zavar.carriagecontroller.ui.data.CarriagePoint;

import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;

public final class GoToCreatorDialog extends Dialog<GoToCarriageAction> {

    private final SimpleObjectProperty<String> pointObjectListProperty = new SimpleObjectProperty<>();
    private final SimpleDoubleProperty speedDoubleProperty = new SimpleDoubleProperty();

    public GoToCreatorDialog(ResourceBundle resourceBundle, HashMap<String, CarriagePoint> carriagePoints) {
        super.setTitle(resourceBundle.getString("dialog.goTo.title"));
        super.setHeaderText(resourceBundle.getString("dialog.goTo.headerText"));
        super.setGraphic(new ImageView(Objects.requireNonNull(GoToCreatorDialog.class.getResource("/ru/itmo/zavar/carriagecontroller/img/goTo.png")).toString()));
        ButtonType addButtonType = new ButtonType(resourceBundle.getString("dialog.goTo.add"), ButtonBar.ButtonData.OK_DONE);
        super.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        Node node = super.getDialogPane().lookupButton(addButtonType);
        node.setDisable(true);
        SimpleListProperty<String> pointListProperty = new SimpleListProperty<>(FXCollections.observableArrayList(carriagePoints.keySet().stream().toList()));
        Form form = Form.of(Group.of(
                Field.ofSingleSelectionType(pointListProperty, pointObjectListProperty)
                        .required(true)
                        .tooltip(resourceBundle.getString("dialog.goTo.pointTooltip"))
                        .label(resourceBundle.getString("dialog.goTo.point")),
                Field.ofDoubleType(60)
                        .bind(speedDoubleProperty)
                        .validate(DoubleRangeValidator.between(0, 160, resourceBundle.getString("dialog.goTo.boundsError").formatted(0, 160)))
                        .required(true)
                        .label(resourceBundle.getString("dialog.goTo.speed"))
        ));
        node.disableProperty().bind(form.validProperty().not());
        super.getDialogPane().setPrefWidth(600);
        super.getDialogPane().setContent(new FormRenderer(form));

        super.setResultConverter(buttonType -> {
            if (buttonType.equals(addButtonType)) {
                form.persist();
                CarriagePoint carriagePoint = carriagePoints.get(pointObjectListProperty.get());
                return new GoToCarriageAction(new Float[]{(float) carriagePoint.x(), (float) speedDoubleProperty.get()});
            }
            return null;
        });
    }
}
