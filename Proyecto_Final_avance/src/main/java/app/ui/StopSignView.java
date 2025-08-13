package app.ui;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class StopSignView extends Polygon {
    private static final double RADIUS = 20;
    private static final Color COLOR = Color.RED;

    public StopSignView(double centerX, double centerY) {
        super();

        // Create 8-sided octagon
        for (int i = 0; i < 8; i++) {
            double angleDeg = 45 * i - 22.5;
            double angleRad = Math.toRadians(angleDeg);
            double x = centerX + RADIUS * Math.cos(angleRad);
            double y = centerY + RADIUS * Math.sin(angleRad);
            getPoints().addAll(x, y);
        }

        setFill(COLOR);
        setStroke(Color.WHITE);
        setStrokeWidth(3);
    }
}
