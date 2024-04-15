package ru.itmo.zavar.carriagecontroller.ui.dialogs;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.validators.DoubleRangeValidator;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import ru.itmo.zavar.carriagecontroller.ui.data.CarriagePoint;
import ru.itmo.zavar.carriagecontroller.ui.data.CoordinateBounds;

import java.util.Objects;
import java.util.ResourceBundle;

public class BoundsDialog extends Dialog<CoordinateBounds> {

    private final SimpleDoubleProperty xMaxDoubleProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty yMaxDoubleProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty xMinDoubleProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty yMinDoubleProperty = new SimpleDoubleProperty();

    public BoundsDialog(ResourceBundle resourceBundle, double minX, double maxX, double minY, double maxY) {
        super.setTitle(resourceBundle.getString("dialog.bounds.title"));
        super.setHeaderText(resourceBundle.getString("dialog.bounds.headerText"));
        super.setGraphic(new ImageView(Objects.requireNonNull(BoundsDialog.class.getResource("/ru/itmo/zavar/carriagecontroller/img/bounds.png")).toString()));
        ButtonType setButtonType = new ButtonType(resourceBundle.getString("dialog.bounds.set"), ButtonBar.ButtonData.OK_DONE);
        super.getDialogPane().getButtonTypes().addAll(setButtonType, ButtonType.CANCEL);
        Node node = super.getDialogPane().lookupButton(setButtonType);
        node.setDisable(true);

        Form form = Form.of(Group.of(
                Field.ofDoubleType(maxX)
                        .bind(xMaxDoubleProperty)
                        .required(true)
                        .label(resourceBundle.getString("dialog.bounds.xMax")),
                Field.ofDoubleType(maxY)
                        .bind(yMaxDoubleProperty)
                        .required(true)
                        .label(resourceBundle.getString("dialog.bounds.yMax")),
                Field.ofDoubleType(minX)
                        .bind(xMinDoubleProperty)
                        .required(true)
                        .label(resourceBundle.getString("dialog.bounds.xMin")),
                Field.ofDoubleType(minY)
                        .bind(yMinDoubleProperty)
                        .required(true)
                        .label(resourceBundle.getString("dialog.bounds.yMin"))
        ));
        node.disableProperty().bind(form.validProperty().not());
        super.getDialogPane().setPrefWidth(800);
        FormRenderer formRenderer = new FormRenderer(form);
        super.getDialogPane().setContent(formRenderer);

        super.setResultConverter(buttonType -> {
            if (buttonType.equals(setButtonType)) {
                form.persist();
                return new CoordinateBounds(xMinDoubleProperty.get(), xMaxDoubleProperty.get(), yMinDoubleProperty.get(), yMaxDoubleProperty.get());
            }
            return null;
        });
    }
}
