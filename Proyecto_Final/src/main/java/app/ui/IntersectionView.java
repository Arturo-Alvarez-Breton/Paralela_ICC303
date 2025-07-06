package app.ui;

import app.enums.VehicleTypeEnum;
import javafx.animation.PathTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Panel principal de la intersección con controles para agregar vehículos
 */
public class IntersectionView extends BorderPane {
    private static final int WIDTH = LaunchView.WIDTH;
    private static final int HEIGHT = LaunchView.HEIGHT;
    private static final int ROAD_WIDTH = HEIGHT / 4;

    private Pane intersectionPane;

    public IntersectionView() {
        setPrefSize(WIDTH, HEIGHT);

        // Pane central con la intersección
        intersectionPane = new Pane();
        intersectionPane.setPrefSize(WIDTH, HEIGHT);

        drawBackground();
        drawRoads();
        drawCenterLines();
        drawStopSigns();

        setCenter(intersectionPane);
        setBottom(createControlPanel());
    }

    private void drawBackground() {
        Rectangle background = new Rectangle(WIDTH, HEIGHT);
        background.setFill(Color.GREEN);
        intersectionPane.getChildren().add(background);
    }

    private void drawRoads() {
        Rectangle horizontalRoad = new Rectangle(0, (HEIGHT / 2.0) - (ROAD_WIDTH / 2.0), WIDTH, ROAD_WIDTH);
        horizontalRoad.setFill(Color.BLACK);
        intersectionPane.getChildren().add(horizontalRoad);

        Rectangle verticalRoad = new Rectangle((WIDTH / 2.0) - (ROAD_WIDTH / 2.0), 0, ROAD_WIDTH, HEIGHT);
        verticalRoad.setFill(Color.BLACK);
        intersectionPane.getChildren().add(verticalRoad);
    }

    private void drawCenterLines() {
        int centerX = WIDTH / 2;
        int centerY = HEIGHT / 2;
        int lineWidth = (int) (HEIGHT * 0.008);
        int dashLength = (int) (HEIGHT * 0.020);

        // Líneas amarillas horizontales
        for (int x = 0; x < WIDTH; x += 2 * dashLength) {
            int endX = x + dashLength;
            if (!(endX > centerX - ROAD_WIDTH / 2 && x < centerX + ROAD_WIDTH / 2)) {
                Line dash = new Line(x, centerY, endX, centerY);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth(lineWidth);
                intersectionPane.getChildren().add(dash);
            }
        }

        // Líneas amarillas verticales
        for (int y = 0; y < HEIGHT; y += 2 * dashLength) {
            int endY = y + dashLength;
            if (!(endY > centerY - ROAD_WIDTH / 2 && y < centerY + ROAD_WIDTH / 2)) {
                Line dash = new Line(centerX, y, centerX, endY);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth(lineWidth);
                intersectionPane.getChildren().add(dash);
            }
        }
    }

    private void drawStopSigns() {
        double offset = 25;
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;
        double halfRoad = ROAD_WIDTH / 2.0;

        StopSignView northStop = new StopSignView(centerX - halfRoad + offset, centerY - halfRoad - offset);
        StopSignView southStop = new StopSignView(centerX + halfRoad - offset, centerY + halfRoad + offset);
        StopSignView eastStop = new StopSignView(centerX + halfRoad + offset, centerY - halfRoad + offset);
        StopSignView westStop = new StopSignView(centerX - halfRoad - offset, centerY + halfRoad - offset);

        intersectionPane.getChildren().addAll(northStop, southStop, eastStop, westStop);
    }

    private HBox createControlPanel() {
        Button btnNorth = new Button("Agregar Norte");
        btnNorth.setOnAction(e -> addVehicleFrom("north"));

        Button btnSouth = new Button("Agregar Sur");
        btnSouth.setOnAction(e -> addVehicleFrom("south"));

        Button btnEast = new Button("Agregar Este");
        btnEast.setOnAction(e -> addVehicleFrom("east"));

        Button btnWest = new Button("Agregar Oeste");
        btnWest.setOnAction(e -> addVehicleFrom("west"));

        HBox controls = new HBox(20, btnNorth, btnSouth, btnEast, btnWest);
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-padding: 10; -fx-background-color: #dddddd;");
        return controls;
    }

    /**
     * Crea un vehículo en el borde especificado y lo anima hacia el centro.
     */
    private void addVehicleFrom(String direction) {
        Color vehicleColor = Color.GRAY;
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;
        double quarterRoad = ROAD_WIDTH / 4.0;
        double startX = centerX, startY = centerY;
        double endX = centerX, endY = centerY;

        switch (direction) {
            case "north":
                endX = startX = centerX - quarterRoad;
                startY = 0;
                endY = centerY;
                break;
            case "south":
                endX = startX = centerX + quarterRoad;
                startY = HEIGHT;
                endY = centerY;
                break;
            case "east":
                startY = endY = centerY - quarterRoad;
                startX = WIDTH;
                endX = centerX;
                break;
            case "west":
                startY = endY = centerY + quarterRoad;
                startX = 0;
                endX = centerX;
                break;
            default:
                break;
        }

        // Crear y añadir la vista del vehículo
        VehicleView vehicle = new VehicleView(startX, startY, Color.BLUE);
        intersectionPane.getChildren().add(vehicle);

        // Definir trayectoria y transición
        Line path = new Line(startX, startY, endX, endY);
        PathTransition transition = new PathTransition(Duration.seconds(3), path, vehicle);
        transition.setCycleCount(1);
        transition.setOnFinished(e -> intersectionPane.getChildren().remove(vehicle));
        transition.play();
    }
}
