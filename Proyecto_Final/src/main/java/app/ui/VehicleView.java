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
                       VehicleTypeEnum type, DirectionEnum direction, String entryPoint) {
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
        String dirSymbol = getDirectionSymbol(direction, entryPoint);

        directionText = new Text(centerX - 7, centerY + 5, dirSymbol);
        directionText.setFont(Font.font("Arial", 16));
        directionText.setFill(type == VehicleTypeEnum.EMERGENCY ? Color.WHITE : Color.BLACK);

        this.getChildren().addAll(circle, directionText);
    }

    private String getDirectionSymbol(DirectionEnum direction, String entryPoint) {
        String[][] symbols = {
            {"↓", "→", "←", "↑"}, // Norte
            {"←", "↓", "↑", "→"}, // Este
            {"↑", "←", "→", "↓"}, // Sur
            {"→", "↑", "↓", "←"}  // Oeste
        };

        int entryIndex = switch (entryPoint) {
            case "norte" -> 0;
            case "este" -> 1;
            case "sur" -> 2;
            case "oeste" -> 3;
            default -> throw new IllegalArgumentException("Invalid entry point: " + entryPoint);
        };

        int tunrnIndex = switch (direction) {
            case STRAIGHT -> 0;
            case LEFT -> 1;
            case RIGHT -> 2;
            case U_TURN -> 3;
        };

        return symbols[entryIndex][tunrnIndex];
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