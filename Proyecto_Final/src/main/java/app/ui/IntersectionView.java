package app.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;


public class IntersectionView extends Pane {
    // Alto y ancho de la pantalla
    private static final int WIDTH = LaunchView.WIDTH;
    private static final int HEIGHT = LaunchView.HEIGHT;

    // Ancho de las calles y líneas
    private static final int ROAD_WIDTH = HEIGHT/4;
    private static final int LINE_WIDTH = (int) (HEIGHT*0.008);
    private static final int DASH_LENGTH = (int) (HEIGHT*0.020);

    // Semaforos en las 4 esquinas
    private TrafficLightView northLight;
    private TrafficLightView eastLight;
    private TrafficLightView southLight;
    private TrafficLightView westLight;

    public IntersectionView() {
        setPrefSize(WIDTH, HEIGHT);
        drawBackground();
        drawRoads();
        drawCenterLines();
        drawStopSigns();
        //drawTrafficLights();
    }

    private void drawBackground() {
        Rectangle background = new Rectangle(WIDTH, HEIGHT);
        background.setFill(Color.GREEN);
        getChildren().add(background);
    }

    private void drawRoads() {
        Rectangle horizontalRoad = new Rectangle(0, ((double) HEIGHT / 2) - ((double) ROAD_WIDTH / 2), WIDTH, ROAD_WIDTH);
        horizontalRoad.setFill(Color.BLACK);
        getChildren().add(horizontalRoad);

        Rectangle verticalRoad = new Rectangle((double) WIDTH / 2 - (double) ROAD_WIDTH / 2, 0, ROAD_WIDTH, HEIGHT);
        verticalRoad.setFill(Color.BLACK);
        getChildren().add(verticalRoad);
    }

    private void drawCenterLines() {
        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;

        // Líneas amarillas horizontales
        for(int x = 0; x < WIDTH; x += 2 * DASH_LENGTH) {
            int endX = x + DASH_LENGTH;

            // Evitar zona central
            if (!(endX > centerX - ROAD_WIDTH / 2 && x < centerX + ROAD_WIDTH / 2)) {
                Line dash = new Line(x, centerY, endX, centerY);
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

    private void drawStopSigns() {
        double offset = 25;
        double centerX = WIDTH / 2;
        double centerY = HEIGHT / 2;
        double halfRoad = ROAD_WIDTH / 2;

        StopSignView northStop = new StopSignView(centerX - halfRoad + offset, centerY - halfRoad - offset);
        StopSignView southStop = new StopSignView(centerX + halfRoad - offset, centerY + halfRoad + offset);
        StopSignView eastStop = new StopSignView(centerX + halfRoad + offset, centerY - halfRoad + offset);
        StopSignView westStop = new StopSignView(centerX - halfRoad - offset, centerY + halfRoad - offset);

        getChildren().addAll(northStop, southStop, eastStop, westStop);
    }

    private void drawTrafficLights() {
        double offset = 20;
        double centerX = WIDTH / 2;
        double centerY = HEIGHT / 2;
        double halfRoad = ROAD_WIDTH / 2;

        // Semaforos
        northLight = new TrafficLightView(centerX - halfRoad + offset, centerY - halfRoad - offset, false);
        southLight = new TrafficLightView(centerX + halfRoad - offset, centerY + halfRoad + offset, false);
        eastLight = new TrafficLightView(centerX + halfRoad + offset, centerY - halfRoad + offset, false);
        westLight = new TrafficLightView(centerX - halfRoad - offset, centerY + halfRoad - offset, false);

        getChildren().addAll(northLight, southLight, eastLight, westLight);
    }

    // Métodos para acceder y actualizar los semáforos
    public TrafficLightView getNorthLight() { return northLight; }
    public TrafficLightView getSouthLight() { return southLight; }
    public TrafficLightView getEastLight() { return eastLight; }
    public TrafficLightView getWestLight() { return westLight; }
}
