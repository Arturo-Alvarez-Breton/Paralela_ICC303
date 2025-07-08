package app.ui;

import app.enums.DirectionEnum;
import app.model.Intersection;
import app.enums.VehicleTypeEnum;

import app.model.Vehicle;
import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 * Panel principal de la intersección con controles para agregar vehículos
 */
public class IntersectionView extends BorderPane {
    private static final int SIZE = LaunchView.HEIGHT;
    private static final int ROAD_WIDTH = SIZE / 4;
    private static final int HALF_ROAD_WIDTH = ROAD_WIDTH / 2;
    private static final int QUARTER_ROAD_WIDTH = HALF_ROAD_WIDTH / 2;
    private static final double LINE_WIDTH = SIZE * 0.008;
    private static final double LINE_LENGTH = SIZE * 0.08;

    private static final double CENTER = SIZE / 2.0;
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

    private final Pane intersectionPane;
    private final Intersection logicalIntersection;
    private TextArea logArea;

    private static int vehicleCounter = 1;

    public IntersectionView() {
        this.logicalIntersection = new Intersection("case-1", true);

        intersectionPane = new Pane();
        drawBackground();
        drawRoads();
        drawCenterLines();
        drawStopSigns();

        SquarePane square = new SquarePane();
        square.getChildren().add(intersectionPane);
        square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setLeft(square);
        BorderPane.setMargin(square, new Insets(10));

        // Área de log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(12);
        logArea.setPrefColumnCount(40);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");
        logArea.setWrapText(true);

        Label normalLabel = new Label("Vehículos normales");
        Label emergencyLabel = new Label("Vehículos de emergencia");

        VBox rightBox = new VBox(
                10,
                normalLabel,
                createControlGrid(VehicleTypeEnum.NORMAL),
                emergencyLabel,
                createControlGrid(VehicleTypeEnum.EMERGENCY),
                logArea
        );
        setRight(rightBox);
        BorderPane.setMargin(rightBox, new Insets(10));
    }

    private void drawBackground() {
        Rectangle background = new Rectangle(SIZE, SIZE);
        background.setFill(Color.GREEN);
        intersectionPane.getChildren().add(background);
    }

    private void drawRoads() {
        Rectangle horizontalRoad = new Rectangle(0, CENTER - HALF_ROAD_WIDTH, SIZE, ROAD_WIDTH);
        horizontalRoad.setFill(Color.BLACK);
        intersectionPane.getChildren().add(horizontalRoad);

        Rectangle verticalRoad = new Rectangle((CENTER) - (HALF_ROAD_WIDTH), 0, ROAD_WIDTH, SIZE);
        verticalRoad.setFill(Color.BLACK);
        intersectionPane.getChildren().add(verticalRoad);
    }

    private void drawCenterLines() {

        // Líneas amarillas horizontales
        for (int x = 0; x < SIZE; x += 2 * (int) LINE_LENGTH) {
            int endX = x + (int) LINE_LENGTH;
            if (!(endX > CENTER - HALF_ROAD_WIDTH && x < CENTER + HALF_ROAD_WIDTH)) {
                Line dash = new Line(x, CENTER, endX, CENTER);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth((int) LINE_WIDTH);
                intersectionPane.getChildren().add(dash);
            }
        }

        // Líneas amarillas verticales
        for (int y = 0; y < SIZE; y += 2 * (int) LINE_LENGTH) {
            int endY = y + (int) LINE_LENGTH;
            if (!(endY > CENTER - HALF_ROAD_WIDTH && y < CENTER + HALF_ROAD_WIDTH)) {
                Line dash = new Line(CENTER, y, CENTER, endY);
                dash.setStroke(Color.YELLOW);
                dash.setStrokeWidth(LINE_WIDTH);
                intersectionPane.getChildren().add(dash);
            }
        }
    }

    private void drawStopSigns() {
        StopSignView northStop = new StopSignView(CENTER - CAR_OFFSET + STOP_SIGN_OFFSET,
                                                    CENTER - CAR_OFFSET - STOP_SIGN_OFFSET);

        StopSignView southStop = new StopSignView(CENTER + CAR_OFFSET - STOP_SIGN_OFFSET,
                                                    CENTER + CAR_OFFSET + STOP_SIGN_OFFSET);

        StopSignView eastStop = new StopSignView(CENTER + CAR_OFFSET + STOP_SIGN_OFFSET,
                                                    CENTER - CAR_OFFSET + STOP_SIGN_OFFSET);

        StopSignView westStop = new StopSignView(CENTER - CAR_OFFSET - STOP_SIGN_OFFSET,
                                                    CENTER + CAR_OFFSET - STOP_SIGN_OFFSET);

        intersectionPane.getChildren().addAll(northStop, southStop, eastStop, westStop);
    }

    // Llamar este método para mostrar mensajes en el log
    private void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
        });
    }

    /**
     * Crea un vehículo en el borde especificado y lo anima hacia el centro.
     */
    /**
     * Crea un vehículo en el borde especificado y lo anima hacia el centro.
     * Ahora acepta tipo y muestra feedback visual.
     */
    private void addVehicleFrom(String entryPoint, DirectionEnum turn, VehicleTypeEnum type) {
        Color fillColor, strokeColor;
        double startX = CENTER, startY = CENTER, endX = CENTER, endY = CENTER;

        switch (entryPoint) {
            case "norte":
                fillColor = FILL_COLOR_NORTH;
                strokeColor = STROKE_COLOR_NORTH;
                startX = CENTER - QUARTER_ROAD_WIDTH;
                startY = 0;
                endX = CENTER - QUARTER_ROAD_WIDTH;
                endY = getCenterCarPosition(turn, 1);
                break;
            case "sur":
                fillColor = FILL_COLOR_SOUTH;
                strokeColor = STROKE_COLOR_SOUTH;
                startX = CENTER + QUARTER_ROAD_WIDTH;
                startY = SIZE;
                endX = CENTER + QUARTER_ROAD_WIDTH;
                endY = getCenterCarPosition(turn, -1);
                break;
            case "este":
                fillColor = FILL_COLOR_EAST;
                strokeColor = STROKE_COLOR_EAST;
                startX = SIZE;
                startY = CENTER - QUARTER_ROAD_WIDTH;
                endX = getCenterCarPosition(turn, -1);
                endY = CENTER - QUARTER_ROAD_WIDTH;
                break;
            case "oeste":
                fillColor = FILL_COLOR_WEST;
                strokeColor = STROKE_COLOR_WEST;
                startX = 0;
                startY = CENTER + QUARTER_ROAD_WIDTH;
                endX = getCenterCarPosition(turn, 1);
                endY = CENTER + QUARTER_ROAD_WIDTH;
                break;
            default:
                fillColor = Color.GRAY;
                strokeColor = Color.BLACK;
        }

        String vehicleId = "v" + (vehicleCounter++);

        Vehicle logicalVehicle = new Vehicle(vehicleId, type, turn);
        logicalIntersection.addVehicle(logicalVehicle);

        VehicleView vehicle = new VehicleView(startX, startY, fillColor, strokeColor, type, turn);
        intersectionPane.getChildren().add(vehicle);

        Line path = new Line(startX, startY, endX, endY);
        final double fxStartX = startX, fxStartY = startY, fxEndX = endX, fxEndY = endY;

        javafx.animation.PathTransition transition = new javafx.animation.PathTransition(Duration.seconds(3), path, vehicle);
        transition.setCycleCount(1);
        transition.setOnFinished(e -> {
            logicalVehicle.setInTersection(true);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(fxStartX, fxStartY, fxEndX, fxEndY);
            log("Vehículo " + logicalVehicle.getId() + " (" + logicalVehicle.getType() + ") cruzó (" + turn + ")");
        });

        log("Vehículo " + logicalVehicle.getId() + " (" + logicalVehicle.getType() + ") ingresando desde " + entryPoint + " (" + turn + ")");
        transition.play();
    }

    // Dibuja un rectángulo semitransparente en el carril ocupado durante 0.5s
    private void showLaneFeedback(double startX, double startY, double endX, double endY) {
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double w = Math.abs(endX - startX) > 1 ? Math.abs(endX - startX) : 20;
        double h = Math.abs(endY - startY) > 1 ? Math.abs(endY - startY) : 20;

        Rectangle lane = new Rectangle(x, y, w, h);
        lane.setFill(Color.ORANGE.deriveColor(1, 1, 1, 0.35));
        lane.setBlendMode(BlendMode.SRC_OVER);
        intersectionPane.getChildren().add(lane);

        // Quitar tras un breve tiempo
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> intersectionPane.getChildren().remove(lane));
        }).start();
    }

    private int getCenterCarPosition(DirectionEnum turn, int offsetSign) {
        int maxPos = (int) CENTER;
        switch (turn) {
            case LEFT -> maxPos = (int) CENTER + QUARTER_ROAD_WIDTH * offsetSign;
            case RIGHT -> maxPos = (int) CENTER - QUARTER_ROAD_WIDTH * offsetSign;
            case U_TURN -> maxPos = (int) CENTER - (int) LINE_WIDTH * offsetSign;
        }

        return maxPos;
    }

    private Color getStrokeColorByDirection(DirectionEnum direction) {
        // TODO: Asignar color de stroke en base a la direccion a la que se dirige el carro
        return null;
    }

    private GridPane createControlGrid(VehicleTypeEnum type) {
        String[] cardinals = {"Norte", "Este", "Sur", "Oeste"};
        String[] actionNames = {"STRAIGHT", "LEFT", "RIGHT", "U_TURN"};

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: #f0f0f0;");

        for (int c = 0; c < cardinals.length; c++) {
            Label lbl = new Label(cardinals[c]);
            GridPane.setHalignment(lbl, HPos.CENTER);
            grid.add(lbl, c, 0);
        }
        for (int r = 0; r < actionNames.length; r++) {
            for (int c = 0; c < cardinals.length; c++) {
                String dir = cardinals[c].toLowerCase();
                String action = actionNames[r];
                Button btn = new Button(action.replace('_', '\u2011'));
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setStyle(
                        "-fx-border-color: black; " +
                                "-fx-border-radius: 8; " +
                                "-fx-background-radius: 8;"
                );
                btn.setOnAction(e -> addVehicleFrom(dir, DirectionEnum.valueOf(action), type));
                grid.add(btn, c, r + 1);
            }
        }
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
