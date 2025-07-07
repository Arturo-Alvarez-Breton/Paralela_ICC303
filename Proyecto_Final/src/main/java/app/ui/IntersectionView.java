package app.ui;

import app.enums.DirectionEnum;

import app.enums.VehicleTypeEnum;
import javafx.animation.PathTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.*;

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
        // ya no fijamos prefSize al BorderPane, dejamos que el contenedor lo escale
        intersectionPane = new Pane();
        drawBackground();
        drawRoads();
        drawCenterLines();
        drawStopSigns();

        // 1) Construimos el contenedor que fuerza 1x1
        SquarePane square = new SquarePane();
        square.getChildren().add(intersectionPane);
        // para que crezca con el espacio disponible
        square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setLeft(square);
        BorderPane.setMargin(square, new Insets(10));

        // 2) Añadimos el grid de controles a la derecha
        setRight(createControlGrid());
        BorderPane.setMargin(getRight(), new Insets(10));
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

//    private HBox createControlPanel() {
//        Button btnNorth = new Button("Agregar Norte");
//        btnNorth.setOnAction(e -> addVehicleFrom("north"));
//
//        Button btnSouth = new Button("Agregar Sur");
//        btnSouth.setOnAction(e -> addVehicleFrom("south"));
//
//        Button btnEast = new Button("Agregar Este");
//        btnEast.setOnAction(e -> addVehicleFrom("east"));
//
//        Button btnWest = new Button("Agregar Oeste");
//        btnWest.setOnAction(e -> addVehicleFrom("west"));
//
//        HBox controls = new HBox(20, btnNorth, btnSouth, btnEast, btnWest);
//        controls.setAlignment(Pos.CENTER);
//        controls.setStyle("-fx-padding: 10; -fx-background-color: #dddddd;");
//        return controls;
//    }

    /**
     * Crea un vehículo en el borde especificado y lo anima hacia el centro.
     */
    private void addVehicleFrom(String entryPoint, DirectionEnum turn) {
        // por ahora mantenemos tu lógica de animación lineal
        // pero podrias guardarte el 'turn' en Vehicle y luego procesarlo.
        Color fillColor, strokeColor;
        double quarterRoad = ROAD_WIDTH / 4.0;
        double startX = CENTER_X, startY = CENTER_Y;
        double endX = CENTER_X, endY = CENTER_Y;

        switch (entryPoint) {
            case "norte":
                fillColor = FILL_COLOR_NORTH;
                strokeColor = STROKE_COLOR_NORTH;
                startX = CENTER_X - quarterRoad;
                startY = 0;
                endX = CENTER_X - quarterRoad;
                endY = HEIGHT;
                break;
            case "sur":
                fillColor = FILL_COLOR_SOUTH;
                strokeColor = STROKE_COLOR_SOUTH;
                startX = CENTER_X + quarterRoad;
                startY = HEIGHT;
                endX = CENTER_X + quarterRoad;
                endY = 0;
                break;
            case "este":
                fillColor = FILL_COLOR_EAST;
                strokeColor = STROKE_COLOR_EAST;
                startX = WIDTH;
                startY = CENTER_Y - quarterRoad;
                endX = 0;
                endY = CENTER_Y - quarterRoad;
                break;
            case "oeste":
                fillColor = FILL_COLOR_WEST;
                strokeColor = STROKE_COLOR_WEST;
                startX = 0;
                startY = CENTER_Y + quarterRoad;
                endX = WIDTH;
                endY = CENTER_Y + quarterRoad;
                break;
            default:
                fillColor = Color.GRAY;
                strokeColor = Color.BLACK;
        }

        VehicleView vehicle = new VehicleView(startX, startY, fillColor, strokeColor);
        intersectionPane.getChildren().add(vehicle);

        Line path = new Line(startX, startY, endX, endY);
        PathTransition transition = new PathTransition(Duration.seconds(3), path, vehicle);
        transition.setCycleCount(1);
        transition.setOnFinished(e -> intersectionPane.getChildren().remove(vehicle));
        transition.play();
    }

    private GridPane createControlGrid() {
        String[] cardinals = {"Norte", "Este", "Sur", "Oeste"};
        String[] actionNames = {"STRAIGHT", "LEFT", "RIGHT", "U_TURN"};

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #f0f0f0;");

        // Ya no usamos percentWidth ni Hgrow
        // Simplemente dejamos que cada columna se dimensione a su contenido

        // Cabeceras
        for (int c = 0; c < cardinals.length; c++) {
            Label lbl = new Label(cardinals[c]);
            GridPane.setHalignment(lbl, HPos.CENTER);
            grid.add(lbl, c, 0);
        }

        // Botones
        for (int r = 0; r < actionNames.length; r++) {
            for (int c = 0; c < cardinals.length; c++) {
                String dir = cardinals[c].toLowerCase();
                String action = actionNames[r];
                Button btn = new Button(action.replace('_','‑'));
                btn.setMaxWidth(Double.MAX_VALUE); // para que estiren dentro de su celda, pero no más allá
                btn.setStyle(
                        "-fx-border-color: black; " +
                                "-fx-border-radius: 8; " +
                                "-fx-background-radius: 8;"
                );
                btn.setOnAction(e -> addVehicleFrom(dir, DirectionEnum.valueOf(action)));
                grid.add(btn, c, r + 1);
            }
        }

        // Fijamos su tamaño al computado
        grid.setPrefHeight(ROAD_WIDTH);
        grid.setMaxHeight(ROAD_WIDTH);

        return grid;
    }

    /**
     * Un Pane que siempre es cuadrado (width == height) y centra su hijo.
     */
    private static class SquarePane extends Pane {
        @Override
        protected void layoutChildren() {
            double size = Math.min(getWidth(), getHeight());
            double offsetX = (getWidth() - size) / 2;
            double offsetY = (getHeight() - size) / 2;
            for (var child : getChildren()) {
                child.resizeRelocate(offsetX, offsetY, size, size);
            }
        }

        @Override
        protected double computePrefWidth(double height) {
            return height;
        }

        @Override
        protected double computePrefHeight(double width) {
            return width;
        }
    }
}
