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
    private static final double LINE_WIDTH = HEIGHT * 0.008;
    private static final double LINE_LENGTH = HEIGHT * 0.08;

    private static final double CENTER_X = WIDTH / 2.0;
    private static final double CENTER_Y = HEIGHT / 2.0;
    private static final double CAR_OFFSET = ROAD_WIDTH / 2.0;
    private static final double STOP_SIGN_OFFSET = 25;

    private static final Color FILL_COLOR_NORTH = Color.RED;
    private static final Color FILL_COLOR_SOUTH = Color.YELLOW;
    private static final Color FILL_COLOR_EAST = Color.BLUE;
    private static final Color FILL_COLOR_WEST = Color.PURPLE;
    private static final Color STROKE_COLOR_NORTH = Color.DARKRED;
    private static final Color STROKE_COLOR_SOUTH = Color.YELLOW;
    private static final Color STROKE_COLOR_EAST = Color.DARKBLUE;
    private static final Color STROKE_COLOR_WEST = Color.DARKMAGENTA;

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

        // Líneas amarillas horizontales
        for (int x = 0; x < WIDTH; x += 2 * (int) LINE_LENGTH) {
            int endX = x + (int) LINE_LENGTH;
            if (!(endX > CENTER_X - ROAD_WIDTH / 2 && x < CENTER_X + ROAD_WIDTH / 2)) {
                Line dash = new Line(x, CENTER_Y, endX, CENTER_Y);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth((int) LINE_WIDTH);
                intersectionPane.getChildren().add(dash);
            }
        }

        // Líneas amarillas verticales
        for (int y = 0; y < HEIGHT; y += 2 * LINE_LENGTH) {
            int endY = y + (int) LINE_LENGTH;
            if (!(endY > CENTER_Y - ROAD_WIDTH / 2 && y < CENTER_Y + ROAD_WIDTH / 2)) {
                Line dash = new Line(CENTER_X, y, CENTER_X, endY);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth(LINE_WIDTH);
                intersectionPane.getChildren().add(dash);
            }
        }
    }

    private void drawStopSigns() {
        double offset = 25;

        StopSignView northStop = new StopSignView(CENTER_X - CAR_OFFSET + STOP_SIGN_OFFSET,
                                                    CENTER_Y - CAR_OFFSET - STOP_SIGN_OFFSET);

        StopSignView southStop = new StopSignView(CENTER_X + CAR_OFFSET - STOP_SIGN_OFFSET,
                                                    CENTER_Y + CAR_OFFSET + STOP_SIGN_OFFSET);

        StopSignView eastStop = new StopSignView(CENTER_X + CAR_OFFSET + STOP_SIGN_OFFSET,
                                                    CENTER_Y - CAR_OFFSET + STOP_SIGN_OFFSET);

        StopSignView westStop = new StopSignView(CENTER_X - CAR_OFFSET - STOP_SIGN_OFFSET,
                                                    CENTER_Y + CAR_OFFSET - STOP_SIGN_OFFSET);

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
        Color fillColor = Color.GRAY;
        Color strokeColor = Color.BLACK;
        double quarterRoad = ROAD_WIDTH / 4.0;
        double startX = CENTER_X, startY = CENTER_Y;
        double endX = CENTER_X, endY = CENTER_Y;

        switch (direction) {
            case "north":
                fillColor = FILL_COLOR_NORTH;
                strokeColor = STROKE_COLOR_NORTH;
                endX = startX = CENTER_X - quarterRoad;
                startY = 0;
                endY = HEIGHT;
                break;
            case "south":
                fillColor = FILL_COLOR_SOUTH;
                strokeColor = STROKE_COLOR_SOUTH;
                endX = startX = CENTER_X + quarterRoad;
                startY = HEIGHT;
                endY = -HEIGHT;
                break;
            case "east":
                fillColor = FILL_COLOR_EAST;
                strokeColor = STROKE_COLOR_EAST;
                startY = endY = CENTER_Y - quarterRoad;
                startX = WIDTH;
                endX = -WIDTH;
                break;
            case "west":
                fillColor = FILL_COLOR_WEST;
                strokeColor = STROKE_COLOR_WEST;
                startY = endY = CENTER_Y + quarterRoad;
                startX = 0;
                endX = WIDTH;
                break;
            default:
                break;
        }

        // Crear y añadir la vista del vehículo
        VehicleView vehicle = new VehicleView(startX, startY, fillColor, strokeColor);
        intersectionPane.getChildren().add(vehicle);

        // Definir trayectoria y transición
        Line path = new Line(startX, startY, endX, endY);
        PathTransition transition = new PathTransition(Duration.seconds(3), path, vehicle);
        transition.setCycleCount(1);
        transition.setOnFinished(e -> intersectionPane.getChildren().remove(vehicle));
        transition.play();
    }
}
