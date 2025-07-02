package app.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class IntersectionView extends Pane {
    // Alto y ancho de la pantalla
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    // Ancho de las calles y líneas
    private static final int ROAD_WIDTH = 150;
    private static final int LINE_WIDTH = 4;
    private static final int DASH_LENGTH = 15;

    public IntersectionView() {
        setPrefSize(WIDTH, HEIGHT);
        drawBackground();
        drawRoads();
        drawCenterLines();
    }

    private void drawBackground() {
        Rectangle background = new Rectangle(WIDTH, HEIGHT);
        background.setFill(Color.SADDLEBROWN);
        getChildren().add(background);
    }

    private void drawRoads() {
        Rectangle horizontalRoad = new Rectangle(0, ((double) HEIGHT / 2) - ((double) ROAD_WIDTH / 2), WIDTH, ROAD_WIDTH);
        horizontalRoad.setFill(Color.LIGHTGRAY);
        getChildren().add(horizontalRoad);

        Rectangle verticalRoad = new Rectangle((double) WIDTH / 2 - (double) ROAD_WIDTH / 2, 0, ROAD_WIDTH, HEIGHT);
        verticalRoad.setFill(Color.LIGHTGRAY);
        getChildren().add(verticalRoad);
    }

    private void drawCenterLines() {
        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;

        // Líneas amarillas horizontales
        for(int x = 0; x < WIDTH; x += 2 * DASH_LENGTH) {
            int startX = x;
            int endX = x + DASH_LENGTH;

            // Evitar zona central
            if (!(endX > centerX - ROAD_WIDTH / 2 && startX < centerX + ROAD_WIDTH / 2)) {
                Line dash = new Line(startX, centerY, endX, centerY);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth(LINE_WIDTH);
                getChildren().add(dash);
            }
        }

        // Líneas amarillas verticales
        for(int y = 0; y < HEIGHT; y += 2 * DASH_LENGTH) {
            int startY = y;
            int endY = y + DASH_LENGTH;

            // Evitar zona central
            if (!(endY > centerY - ROAD_WIDTH / 2 && startY < centerY + ROAD_WIDTH / 2)) {
                Line dash = new Line(centerX, startY, centerX, endY);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth(LINE_WIDTH);
                getChildren().add(dash);
            }
        }
    }

}
