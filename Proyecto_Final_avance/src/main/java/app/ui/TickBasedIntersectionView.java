package app.ui;

import app.controller.TickBasedTrafficController;
import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;
import app.model.*;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nueva vista de intersección que integra el sistema basado en ticks
 * Preserva EXACTAMENTE las coordenadas del sistema JavaFX original
 */
public class TickBasedIntersectionView extends BorderPane {
    
    // === COORDENADAS EXACTAS DEL SISTEMA ORIGINAL - NO MODIFICAR ===
    private static final int SIZE = LaunchView.HEIGHT;
    private static final int ROAD_WIDTH = SIZE / 4;
    private static final int HALF_ROAD_WIDTH = ROAD_WIDTH / 2;
    // private static final int QUARTER_ROAD_WIDTH = HALF_ROAD_WIDTH / 2; // Reservado para uso futuro
    private static final double LINE_WIDTH = SIZE * 0.008;
    private static final double LINE_LENGTH = SIZE * 0.02;

    private static final double CENTER = SIZE / 2.0;
    private static final double CAR_OFFSET = ROAD_WIDTH / 2.0;
    private static final double STOP_SIGN_OFFSET = 25;

    // === COLORES EXACTOS DEL SISTEMA ORIGINAL ===
    private static final Color FILL_COLOR_NORTH = Color.RED;
    private static final Color FILL_COLOR_SOUTH = Color.YELLOW;
    private static final Color FILL_COLOR_EAST = Color.BLUE;
    private static final Color FILL_COLOR_WEST = Color.PURPLE;
    private static final Color STROKE_COLOR_NORTH = Color.DARKRED;
    private static final Color STROKE_COLOR_SOUTH = Color.YELLOW;
    private static final Color STROKE_COLOR_EAST = Color.DARKBLUE;
    private static final Color STROKE_COLOR_WEST = Color.DARKMAGENTA;
    
    // === COMPONENTES DE LA UI ===
    private Pane intersectionPane;
    private TextArea logArea;
    private final TickBasedTrafficController trafficController;
    
    // === GESTIÓN DE VEHÍCULOS VISUALES ===
    private final Map<String, VehicleView> vehicleViews;
    
    // === ESTADÍSTICAS ===
    private Label statsLabel;
    
    public TickBasedIntersectionView() {
        // Inicializar controlador de tráfico basado en ticks
        this.trafficController = new TickBasedTrafficController();
        this.vehicleViews = new ConcurrentHashMap<>();
        
        // Configurar callbacks del controlador
        setupControllerCallbacks();
        
        // Crear interfaz gráfica
        setupUI();
        
        // Iniciar el sistema de tráfico
        trafficController.start();
        
        log("🚦 Sistema de tráfico basado en ticks iniciado");
    }
    
    /**
     * Configura los callbacks del controlador para actualizar la UI
     */
    private void setupControllerCallbacks() {
        // Callback para actualizar posiciones de vehículos
        trafficController.setVehicleUpdateCallback((vehicleId, position, state) -> {
            Platform.runLater(() -> {
                VehicleView vehicleView = vehicleViews.get(vehicleId);
                if (vehicleView != null) {
                    // Actualizar posición visual usando coordenadas exactas
                    vehicleView.setPosition(position.getX(), position.getY());
                    
                    // Actualizar visualización según el estado
                    updateVehicleVisualState(vehicleView, state);
                    
                    // Si el vehículo está completado, removerlo
                    if (state == VehicleState.COMPLETED) {
                        removeVehicleView(vehicleId);
                    }
                }
            });
        });
        
        // Callback para logging
        trafficController.setLogCallback(this::log);
    }
    
    /**
     * Configura la interfaz de usuario
     */
    private void setupUI() {
        // Panel principal de la intersección
        intersectionPane = new Pane();
        drawIntersectionBackground();
        
        // Envolver en SquarePane para mantener aspecto cuadrado
        SquarePane square = new SquarePane();
        square.getChildren().add(intersectionPane);
        square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setLeft(square);
        BorderPane.setMargin(square, new Insets(0));
        
        // Panel derecho con controles y logs
        setupRightPanel();
    }
    
    /**
     * Dibuja el fondo de la intersección con coordenadas exactas del sistema original
     */
    private void drawIntersectionBackground() {
        // Fondo
        Rectangle background = new Rectangle(SIZE, SIZE, Color.LIGHTGREEN);
        intersectionPane.getChildren().add(background);
        
        // Carreteras - COORDENADAS EXACTAS DEL SISTEMA ORIGINAL
        drawRoads();
        drawCenterLines();
        drawStopSigns();
    }
    
    /**
     * Dibuja las carreteras - COORDENADAS EXACTAS
     */
    private void drawRoads() {
        // Carretera vertical (Norte-Sur)
        Rectangle verticalRoad = new Rectangle(
            CENTER - HALF_ROAD_WIDTH, 0, 
            ROAD_WIDTH, SIZE
        );
        verticalRoad.setFill(Color.DARKGRAY);
        
        // Carretera horizontal (Este-Oeste)
        Rectangle horizontalRoad = new Rectangle(
            0, CENTER - HALF_ROAD_WIDTH, 
            SIZE, ROAD_WIDTH
        );
        horizontalRoad.setFill(Color.DARKGRAY);
        
        intersectionPane.getChildren().addAll(verticalRoad, horizontalRoad);
    }
    
    /**
     * Dibuja las líneas centrales - COORDENADAS EXACTAS
     */
    private void drawCenterLines() {
        // Línea central vertical
        for (double y = 0; y < SIZE; y += LINE_LENGTH * 2) {
            if (y + LINE_LENGTH < CENTER - HALF_ROAD_WIDTH || y > CENTER + HALF_ROAD_WIDTH) {
                Line line = new Line(CENTER, y, CENTER, y + LINE_LENGTH);
                line.setStroke(Color.WHITE);
                line.setStrokeWidth(LINE_WIDTH);
                intersectionPane.getChildren().add(line);
            }
        }
        
        // Línea central horizontal
        for (double x = 0; x < SIZE; x += LINE_LENGTH * 2) {
            if (x + LINE_LENGTH < CENTER - HALF_ROAD_WIDTH || x > CENTER + HALF_ROAD_WIDTH) {
                Line line = new Line(x, CENTER, x + LINE_LENGTH, CENTER);
                line.setStroke(Color.WHITE);
                line.setStrokeWidth(LINE_WIDTH);
                intersectionPane.getChildren().add(line);
            }
        }
    }
    
    /**
     * Dibuja las señales de PARE - COORDENADAS EXACTAS
     */
    private void drawStopSigns() {
        StopSignView northStop = new StopSignView(
            CENTER - CAR_OFFSET + STOP_SIGN_OFFSET,
            CENTER - CAR_OFFSET - STOP_SIGN_OFFSET
        );
        
        StopSignView southStop = new StopSignView(
            CENTER + CAR_OFFSET - STOP_SIGN_OFFSET,
            CENTER + CAR_OFFSET + STOP_SIGN_OFFSET
        );
        
        StopSignView eastStop = new StopSignView(
            CENTER + CAR_OFFSET + STOP_SIGN_OFFSET,
            CENTER - CAR_OFFSET + STOP_SIGN_OFFSET
        );
        
        StopSignView westStop = new StopSignView(
            CENTER - CAR_OFFSET - STOP_SIGN_OFFSET,
            CENTER + CAR_OFFSET - STOP_SIGN_OFFSET
        );
        
        intersectionPane.getChildren().addAll(northStop, southStop, eastStop, westStop);
    }
    
    /**
     * Configura el panel derecho con controles
     */
    private void setupRightPanel() {
        // Área de log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        logArea.setPrefColumnCount(40);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        logArea.setWrapText(true);
        
        // Estadísticas
        statsLabel = new Label("Estadísticas del sistema");
        statsLabel.setTextFill(Color.WHITE);
        updateStatistics();
        
        // Controles de vehículos
        Label normalLabel = new Label("Vehículos normales");
        normalLabel.setTextFill(Color.WHITE);
        Label emergencyLabel = new Label("Vehículos de emergencia");
        emergencyLabel.setTextFill(Color.WHITE);
        
        // Botones de control del sistema
        Button pauseButton = new Button("Pausar/Reanudar");
        pauseButton.setOnAction(e -> toggleSystem());
        
        Button clearButton = new Button("Limpiar Log");
        clearButton.setOnAction(e -> logArea.clear());
        
        VBox rightBox = new VBox(10,
            statsLabel,
            normalLabel,
            createControlGrid(VehicleTypeEnum.NORMAL),
            emergencyLabel,
            createControlGrid(VehicleTypeEnum.EMERGENCY),
            pauseButton,
            clearButton,
            logArea
        );
        
        setRight(rightBox);
        BorderPane.setMargin(rightBox, new Insets(10));
        
        // Actualizar estadísticas periódicamente
        startStatisticsUpdater();
    }
    
    /**
     * Crea una grilla de controles para agregar vehículos
     */
    private GridPane createControlGrid(VehicleTypeEnum type) {
        String[] cardinals = {"Norte", "Este", "Sur", "Oeste"};
        String[] actionNames = {"STRAIGHT", "LEFT", "RIGHT", "U_TURN"};
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: #f0f0f0;");
        
        // Headers
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
                Button btn = new Button(action.replace('_', '\u2011'));
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setStyle("-fx-border-color: black; -fx-border-radius: 8; -fx-background-radius: 8;");
                
                btn.setOnAction(e -> addVehicle(dir, DirectionEnum.valueOf(action), type));
                grid.add(btn, c, r + 1);
            }
        }
        
        grid.setPrefHeight(ROAD_WIDTH);
        grid.setMaxHeight(ROAD_WIDTH);
        return grid;
    }
    
    /**
     * Agrega un nuevo vehículo al sistema
     */
    private void addVehicle(String entryPoint, DirectionEnum direction, VehicleTypeEnum type) {
        String vehicleId = trafficController.addVehicle(entryPoint, direction, type);
        
        if (vehicleId != null) {
            // Crear vista visual del vehículo
            Color[] colors = getVehicleColors(entryPoint);
            VehicleView vehicleView = new VehicleView(0, 0, colors[0], colors[1], type, direction, entryPoint);
            
            // Posicionar en el punto de inicio con coordenadas exactas
            Position startPos = IntersectionCoordinates.getStartPosition(entryPoint);
            vehicleView.setPosition(startPos.getX(), startPos.getY());
            
            // Agregar a la vista y al mapa
            Platform.runLater(() -> {
                intersectionPane.getChildren().add(vehicleView);
                vehicleViews.put(vehicleId, vehicleView);
            });
        }
    }
    
    /**
     * Obtiene los colores del vehículo según el punto de entrada - EXACTOS DEL SISTEMA ORIGINAL
     */
    private Color[] getVehicleColors(String entryPoint) {
        return switch (entryPoint) {
            case "norte" -> new Color[]{FILL_COLOR_NORTH, STROKE_COLOR_NORTH};
            case "sur" -> new Color[]{FILL_COLOR_SOUTH, STROKE_COLOR_SOUTH};
            case "este" -> new Color[]{FILL_COLOR_EAST, STROKE_COLOR_EAST};
            case "oeste" -> new Color[]{FILL_COLOR_WEST, STROKE_COLOR_WEST};
            default -> new Color[]{Color.GRAY, Color.BLACK};
        };
    }
    
    /**
     * Actualiza la visualización de un vehículo según su estado
     */
    private void updateVehicleVisualState(VehicleView vehicleView, VehicleState state) {
        switch (state) {
            case APPROACHING -> vehicleView.setNextInQueueVisual(false, false);
            case WAITING -> vehicleView.setNextInQueueVisual(true, false);
            case CROSSING -> vehicleView.setNextInQueueVisual(false, true);
            case EXITING -> vehicleView.setNextInQueueVisual(false, false);
            case COMPLETED -> vehicleView.setNextInQueueVisual(false, false);
        }
    }
    
    /**
     * Remueve un vehículo de la vista
     */
    private void removeVehicleView(String vehicleId) {
        VehicleView vehicleView = vehicleViews.remove(vehicleId);
        if (vehicleView != null) {
            intersectionPane.getChildren().remove(vehicleView);
        }
    }
    
    /**
     * Alterna entre pausar y reanudar el sistema
     */
    private void toggleSystem() {
        if (trafficController.isRunning()) {
            trafficController.stop();
            log("⏸️ Sistema pausado");
        } else {
            trafficController.start();
            log("▶️ Sistema reanudado");
        }
    }
    
    /**
     * Actualiza las estadísticas del sistema
     */
    private void updateStatistics() {
        if (statsLabel != null) {
            Platform.runLater(() -> {
                String stats = String.format(
                    "Procesados: %d | Emergencias: %d | Tiempo promedio espera: %.1fs",
                    trafficController.getTotalVehiclesProcessed(),
                    trafficController.getEmergencyVehiclesProcessed(),
                    trafficController.getAverageWaitingTime()
                );
                statsLabel.setText(stats);
            });
        }
    }
    
    /**
     * Inicia el actualizador de estadísticas
     */
    private void startStatisticsUpdater() {
        Thread statsUpdater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Actualizar cada segundo
                    updateStatistics();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statsUpdater.setDaemon(true);
        statsUpdater.start();
    }
    
    /**
     * Logging a la UI
     */
    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }
    
    /**
     * Clase para mantener aspecto cuadrado - EXACTA DEL SISTEMA ORIGINAL
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
    
    /**
     * Limpieza al cerrar la vista
     */
    public void shutdown() {
        trafficController.stop();
    }
}
