package app.ui;


import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class VehicleView extends Circle {
    private static final double RADIUS = 10; // Radio del vehículo

    public VehicleView(double centerX, double centerY, Color colorFill, Color colorStroke) {
        super(centerX, centerY, RADIUS);
        setFill(colorFill);
        setStroke(colorStroke);
        setStrokeWidth(2);
    }
}