package app.ui;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Representa un semaforo
 */
public class TrafficLightView extends Circle {
    private boolean isGreen;

    public TrafficLightView(double x, double y, boolean initialGreen) {
        super(x, y, 10);
        isGreen = initialGreen;
        updateColor();
    }

    public void setGreen(boolean green) {
        this.isGreen = green;
        updateColor();
    }

    public void toggle (){
        this.isGreen = !this.isGreen;
        updateColor();
    }

    private void updateColor() {
        setFill(isGreen ? Color.GREEN : Color.RED);
        setStroke(Color.BLACK);
        setStrokeWidth(1);
    }
}