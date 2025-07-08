package app.ui;

import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.Group;

public class VehicleView extends Group {
    private static final double RADIUS = 10; // Radio del vehículo
    private Circle circle;
    private Text directionText;
    private VehicleTypeEnum type;

    public VehicleView(double centerX, double centerY, Color colorFill, Color colorStroke,
                       VehicleTypeEnum type, DirectionEnum direction) {
        this.type = type;
        circle = new Circle(centerX, centerY, RADIUS);
        circle.setFill(colorFill);
        // Si es emergencia, borde blanco, si no el color estándar
        if (type == VehicleTypeEnum.EMERGENCY) {
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(4);
        } else {
            circle.setStroke(colorStroke);
            circle.setStrokeWidth(2);
        }

        // Dirección visual como texto sobre el círculo
        String dirSymbol = switch (direction) {
            case STRAIGHT -> "↑";
            case LEFT -> "↩";
            case RIGHT -> "→";
            case U_TURN -> "⟲";
        };
        directionText = new Text(centerX - 7, centerY + 5, dirSymbol);
        directionText.setFont(Font.font("Arial", 16));
        directionText.setFill(type == VehicleTypeEnum.EMERGENCY ? Color.WHITE : Color.BLACK);

        this.getChildren().addAll(circle, directionText);
    }

    public VehicleTypeEnum getType() {
        return type;
    }

    public void setPosition(double x, double y) {
        circle.setCenterX(x);
        circle.setCenterY(y);
        directionText.setX(x - 7);
        directionText.setY(y + 5);
    }
}