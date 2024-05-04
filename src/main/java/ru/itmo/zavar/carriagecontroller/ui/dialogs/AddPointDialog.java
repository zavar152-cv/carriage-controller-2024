package ru.itmo.zavar.carriagecontroller.ui.dialogs;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.DoubleRangeValidator;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import ru.itmo.zavar.carriagecontroller.CarriageControllerApplication;
import ru.itmo.zavar.carriagecontroller.ui.data.CarriagePoint;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

public final class AddPointDialog extends Dialog<CarriagePoint> {

    private final SimpleDoubleProperty xDoubleProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty yDoubleProperty = new SimpleDoubleProperty();
    private final SimpleStringProperty nameStringProperty = new SimpleStringProperty();

    public AddPointDialog(ResourceBundle resourceBundle, double minX, double maxX, double minY, double maxY, Set<String> pointsName) {
        super.setTitle(resourceBundle.getString("dialog.addPoint.title"));
        super.setHeaderText(resourceBundle.getString("dialog.addPoint.headerText"));
        super.setGraphic(new ImageView(Objects.requireNonNull(AddPointDialog.class.getResource("/ru/itmo/zavar/carriagecontroller/img/point.png")).toString()));
        Stage stage = (Stage) super.getDialogPane().getScene().getWindow();
        stage.getIcons().add(CarriageControllerApplication.getAppIcon());
        ButtonType addButtonType = new ButtonType(resourceBundle.getString("dialog.addPoint.add"), ButtonBar.ButtonData.OK_DONE);
        super.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        Node node = super.getDialogPane().lookupButton(addButtonType);
        node.setDisable(true);

        Form form = Form.of(Group.of(
                Field.ofStringType("")
                        .bind(nameStringProperty)
                        .validate(StringLengthValidator.atLeast(1, resourceBundle.getString("dialog.addPoint.nameError")))
                        .validate(CustomValidator.forPredicate(s -> !pointsName.contains(s), resourceBundle.getString("dialog.addPoint.exists")))
                        .required(true)
                        .label(resourceBundle.getString("dialog.addPoint.name")),
                Field.ofDoubleType(0)
                        .bind(xDoubleProperty)
                        .validate(DoubleRangeValidator.between(minX, maxX, resourceBundle.getString("dialog.addPoint.boundsError").formatted(minX, maxX)))
                        .required(true)
                        .label(resourceBundle.getString("dialog.addPoint.x")),
                Field.ofDoubleType(0)
                        .bind(yDoubleProperty)
                        .validate(DoubleRangeValidator.between(minY, maxY, resourceBundle.getString("dialog.addPoint.boundsError").formatted(minY, maxY)))
                        .required(true)
                        .label(resourceBundle.getString("dialog.addPoint.y"))
        ));
        node.disableProperty().bind(form.validProperty().not());
        super.getDialogPane().setPrefWidth(600);
        super.getDialogPane().setContent(new FormRenderer(form));

        super.setResultConverter(buttonType -> {
            if (buttonType.equals(addButtonType)) {
                form.persist();
                return new CarriagePoint(this.nameStringProperty.get(), this.xDoubleProperty.get(), this.yDoubleProperty.get());
            }
            return null;
        });
    }
}
