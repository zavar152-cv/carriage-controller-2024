package ru.itmo.zavar.carriagecontroller.ui.dialogs;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import ru.itmo.zavar.carriagecontroller.ui.data.CoordinateBounds;

import java.util.Objects;
import java.util.ResourceBundle;

public final class GrapplePositionDialog extends Dialog<Integer> {

    private final SimpleIntegerProperty grapplePositionProperty = new SimpleIntegerProperty();

    public GrapplePositionDialog(ResourceBundle resourceBundle) {
        super.setTitle(resourceBundle.getString("dialog.grapplePosition.title"));
        super.setHeaderText(resourceBundle.getString("dialog.grapplePosition.headerText"));
        super.setGraphic(new ImageView(Objects.requireNonNull(BoundsDialog.class.getResource("/ru/itmo/zavar/carriagecontroller/img/bounds.png")).toString()));
        ButtonType setButtonType = new ButtonType(resourceBundle.getString("dialog.grapplePosition.set"), ButtonBar.ButtonData.OK_DONE);
        super.getDialogPane().getButtonTypes().addAll(setButtonType, ButtonType.CANCEL);
        Node node = super.getDialogPane().lookupButton(setButtonType);
        node.setDisable(true);

        Form form = Form.of(Group.of(
                Field.ofIntegerType(0)
                        .bind(grapplePositionProperty)
                        .required(true)
                        .label(resourceBundle.getString("dialog.grapplePosition.position"))
                ));

        node.disableProperty().bind(form.validProperty().not());
        FormRenderer formRenderer = new FormRenderer(form);
        super.getDialogPane().setContent(formRenderer);

        super.setResultConverter(buttonType -> {
            if (buttonType.equals(setButtonType)) {
                form.persist();
                return grapplePositionProperty.get();
            }
            return null;
        });

    }

}
