package app.ui;

import app.model.enums.DirectionEnum;
import app.model.enums.VehicleTypeEnum;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.shape.Rectangle;

//TODO:  REFACTOR
public class VehicleView extends Group {
    private static final double RADIUS = 10; // Radio del vehículo
    private Circle circle;
    private Text directionText;
    private VehicleTypeEnum type;
    private Rectangle priorityHighlight; // Rectángulo para destacar prioridad

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

        // Crear el indicador de prioridad (inicialmente invisible)
        priorityHighlight = new Rectangle(centerX - RADIUS - 5, centerY - RADIUS - 5, 
                                         RADIUS * 2 + 10, RADIUS * 2 + 10);
        priorityHighlight.setFill(Color.TRANSPARENT);
        priorityHighlight.setStroke(Color.TRANSPARENT);
        priorityHighlight.setStrokeWidth(2);
        priorityHighlight.setArcWidth(10);
        priorityHighlight.setArcHeight(10);
        priorityHighlight.setOpacity(0.7);

        this.getChildren().addAll(priorityHighlight, circle, directionText);
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
        priorityHighlight.setX(x - RADIUS - 5);
        priorityHighlight.setY(y - RADIUS - 5);
    }
    
    /**
     * Establece la visualización de prioridad para el vehículo en la cola
     * @param isNext Si este vehículo es el siguiente en la cola
     * @param isEmergency Si este vehículo es de emergencia con prioridad
     */
    public void setNextInQueueVisual(boolean isNext, boolean isEmergency) {
        if (isNext) {
            // Destacar el vehículo como el siguiente en la cola
            if (isEmergency) {
                // Emergencia - destacado rojo brillante
                priorityHighlight.setStroke(Color.RED);
                priorityHighlight.setFill(Color.RED.deriveColor(1, 1, 1, 0.2));
                
                // Añadir efecto de brillo para emergencia
                Glow glow = new Glow();
                glow.setLevel(0.7);
                this.setEffect(glow);
            } else {
                // Normal - destacado verde
                priorityHighlight.setStroke(Color.LIME);
                priorityHighlight.setFill(Color.LIME.deriveColor(1, 1, 1, 0.2));
                this.setEffect(null);
            }
        } else {
            // No es el siguiente - sin destacado
            priorityHighlight.setFill(Color.TRANSPARENT);
            priorityHighlight.setStroke(Color.TRANSPARENT);
            this.setEffect(null);
        }
    }
}