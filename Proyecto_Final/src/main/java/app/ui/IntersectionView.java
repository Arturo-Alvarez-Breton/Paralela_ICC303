package app.ui;

import app.controller.TrafficController;
import app.enums.DirectionEnum;
import app.model.Intersection;
import app.enums.VehicleTypeEnum;

import app.model.Movement;
import app.model.TrafficLight;
import app.model.Vehicle;
import javafx.animation.PathTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel principal de la intersección con controles para agregar vehículos
 * Ahora incluye TrafficController integrado y animaciones completas con paradas
 */
public class IntersectionView extends BorderPane {
    private static final int SIZE = LaunchView.HEIGHT;
    private static final int ROAD_WIDTH = SIZE / 4;
    private static final int HALF_ROAD_WIDTH = ROAD_WIDTH / 2;
    private static final int QUARTER_ROAD_WIDTH = HALF_ROAD_WIDTH / 2;
    private static final double LINE_WIDTH = SIZE * 0.008;
    private static final double LINE_LENGTH = SIZE * 0.02;

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

    // lienzo donde se van a pintar las carreteras
    private final Pane intersectionPane;
    // instancia del modelo intersection
    private final Intersection logicalIntersection;
    // Controlador de tráfico integrado
    private final TrafficController trafficController;
    // area para los logs
    private TextArea logArea;
    //contador de vehiculos
    private static int vehicleCounter = 1;

    // === SISTEMA DE COLISIONES - Contadores de vehículos esperando ===
    private int vehiclesWaitingNorth = 0;
    private int vehiclesWaitingSouth = 0;
    private int vehiclesWaitingEast = 0;
    private int vehiclesWaitingWest = 0;
    
    // Distancia entre vehículos en la cola (tamaño aproximado de un vehículo)
    private static final double VEHICLE_SPACING = 25.0;
    
    // === CONTROL DE ANIMACIONES ===
    private boolean crossingAnimationInProgress = false;  // Indica si hay una animación de cruce en progreso

    public IntersectionView() {
        this.logicalIntersection = new Intersection("case-1", true);
        
        // === INTEGRACIÓN CON TRAFFIC CONTROLLER ===
        List<Intersection> intersections = new ArrayList<>();
        intersections.add(logicalIntersection);
        
        List<TrafficLight> trafficLights = new ArrayList<>(); // Por ahora vacío
        
        this.trafficController = new TrafficController(intersections, trafficLights, this);
        // Iniciar el control automático de tráfico
        trafficController.startControl();

        intersectionPane = new Pane();
        drawBackground();
        drawRoads();
        drawCenterLines();
        drawStopSigns();

        SquarePane square = new SquarePane();
        square.getChildren().add(intersectionPane);
        square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setLeft(square);
        BorderPane.setMargin(square, new Insets(0));

        // Área de log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(12);
        logArea.setPrefColumnCount(40);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");
        logArea.setWrapText(true);

        Label normalLabel = new Label("Vehículos normales");
        normalLabel.setTextFill(Color.WHITE);
        Label emergencyLabel = new Label("Vehículos de emergencia");
        emergencyLabel.setTextFill(Color.WHITE);

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
     * Crea un vehículo y maneja la animación completa con parada antes del cruce.
     * NUEVA IMPLEMENTACIÓN: Animación en dos fases con integración del TrafficController
     */
    private void addVehicleFrom(String entryPoint,
                                DirectionEnum turn,
                                VehicleTypeEnum type) {

        String vehicleId = "v" + (vehicleCounter++);
        Movement.Entry entryEnum = Movement.Entry.valueOf(entryPoint.toUpperCase());

        // Crear el vehículo lógico y agregarlo a la intersección
        Vehicle logicalVehicle = new Vehicle(vehicleId, type, turn, entryEnum);
        logicalIntersection.addVehicle(logicalVehicle);

        // Obtener colores según el punto de entrada
        Color[] colors = getVehicleColors(entryPoint);
        Color fillColor = colors[0];
        Color strokeColor = colors[1];

        // Crear vista del vehículo
        VehicleView vehicle = new VehicleView(0, 0, fillColor, strokeColor, type, turn, entryPoint);
        intersectionPane.getChildren().add(vehicle);

        // === FASE 1: ANIMACIÓN HASTA ANTES DEL CRUCE ===
        animateToStopLine(vehicle, logicalVehicle, entryPoint, turn);
        
        log("🚗 Vehículo " + vehicleId + " (" + type + ") acercándose desde " + entryPoint + " (" + turn + ")");
    }

    /**
     * FASE 1: Anima el vehículo desde el borde hasta la línea de parada antes del cruce
     * MODIFICADO: Incluye sistema de colisiones para evitar superposición visual
     */
    private void animateToStopLine(VehicleView vehicle, Vehicle logicalVehicle, String entryPoint, DirectionEnum turn) {
        // === SISTEMA DE COLISIONES: Incrementar contador y calcular posición en cola ===
        int queuePosition = incrementWaitingCounter(entryPoint);
        
        // Calcular posiciones de inicio y parada (con spacing para evitar colisiones)
        double[] startPos = getStartPosition(entryPoint);
        double[] stopPos = getStopLinePositionWithSpacing(entryPoint, queuePosition);
        
        // Posicionar vehículo en el inicio
        vehicle.setPosition(startPos[0], startPos[1]);
        
        // Crear línea de movimiento hasta la parada
        Line pathToStop = new Line(startPos[0], startPos[1], stopPos[0], stopPos[1]);
        
        // Animación de 2 segundos hasta la línea de parada
        PathTransition approachTransition = new PathTransition(Duration.seconds(2), pathToStop, vehicle);
        approachTransition.setCycleCount(1);
        
        approachTransition.setOnFinished(e -> {
            log("⏸️ Vehículo " + logicalVehicle.getId() + " esperando autorización en " + entryPoint + " (posición en cola: " + queuePosition + ")");
            
            // === MARCAR VEHÍCULO COMO LISTO PARA CRUZAR ===
            // Ahora que ha llegado a la línea de parada, está listo para ser procesado
            logicalVehicle.setReadyToCross(true);
            
            // === VISUALIZACIÓN DE PRIORIDAD ===
            updateVehiclePriorityVisuals(vehicle, logicalVehicle);
            
            // === FASE 2: ESPERAR AUTORIZACIÓN Y CRUZAR ===
            waitForAuthorizationAndCross(vehicle, logicalVehicle, entryPoint, turn);
        });
        
        approachTransition.play();
    }

    /**
     * Actualiza la visualización del vehículo según su prioridad en la cola
     */
    private void updateVehiclePriorityVisuals(VehicleView vehicle, Vehicle logicalVehicle) {
        List<Vehicle> queuedVehicles = logicalIntersection.peekAllVehicles();
        
        // Verificar si el vehículo está en la cola
        boolean vehicleInQueue = queuedVehicles.stream()
                .anyMatch(v -> v.getId().equals(logicalVehicle.getId()));
        
        if (!vehicleInQueue) {
            // Si el vehículo no está en la cola (ya fue procesado), no actualizar visuales
            return;
        }
        
        // Si no hay vehículos en la cola, algo salió mal
        if (queuedVehicles.isEmpty()) {
            return;
        }
        
        // Comprobar si es el siguiente en la cola de prioridad
        Vehicle firstVehicle = queuedVehicles.get(0);
        boolean isNext = firstVehicle.getId().equals(logicalVehicle.getId());
        
        // Actualizar visual según prioridad
        if (isNext) {
            // Este vehículo es el siguiente - destacarlo visualmente
            if (logicalVehicle.getType() == VehicleTypeEnum.EMERGENCY) {
                // Vehículo de emergencia con alta prioridad
                vehicle.setNextInQueueVisual(true, true);
            } else {
                // Vehículo normal pero siguiente en cola
                vehicle.setNextInQueueVisual(true, false);
            }
        } else {
            // No es el siguiente, quitar cualquier destacado
            vehicle.setNextInQueueVisual(false, false);
            
            // Comprobar posición en la cola para mostrar información
            int position = queuedVehicles.indexOf(logicalVehicle) + 1;
            if (position > 0) {
                log("🔢 Vehículo " + logicalVehicle.getId() + " en posición " + 
                    position + " de " + queuedVehicles.size() + " en la cola");
            }
        }
    }

    /**
     * FASE 2: Espera autorización del TrafficController y ejecuta el cruce completo
     * MEJORADO: Respeta estrictamente el orden de la cola de prioridad
     */
    private void waitForAuthorizationAndCross(VehicleView vehicle, Vehicle logicalVehicle, String entryPoint, DirectionEnum turn) {
        // Crear un thread que simule una pausa realista y luego proceda
        Thread authorizationChecker = new Thread(() -> {
            try {
                // Pausa breve para simular verificación (1 segundo)
                Thread.sleep(1000);
                
                // Obtener todos los vehículos en la cola con su orden de prioridad
                List<Vehicle> queuedVehicles = logicalIntersection.peekAllVehicles();
                boolean canProceed = false;
                
                // Verificar si el vehículo aún está en la cola
                boolean vehicleInQueue = queuedVehicles.stream()
                        .anyMatch(v -> v.getId().equals(logicalVehicle.getId()));
                
                if (!vehicleInQueue) {
                    // El vehículo ya fue procesado por el TrafficController
                    if (logicalVehicle.isInIntersection()) {
                        // Ya está cruzando, proceder con la animación
                        Platform.runLater(() -> {
                            log("🚦 Vehículo " + logicalVehicle.getId() + " ya fue autorizado por el TrafficController");
                            decrementWaitingCounter(entryPoint);
                            executeCompleteMovement(vehicle, logicalVehicle, entryPoint, turn);
                        });
                        return;
                    } else {
                        // Error real - el vehículo no está en la cola ni cruzando
                        log("⚠️ Error: Vehículo " + logicalVehicle.getId() + " no está en la cola ni cruzando");
                        return;
                    }
                }
                
                if (queuedVehicles.isEmpty()) {
                    return; // No hay vehículos esperando
                }
                
                // === VERIFICACIÓN ESTRICTA DE PRIORIDAD ===
                // Solo puede proceder si es exactamente el primer vehículo en la cola de prioridad
                Vehicle firstVehicle = queuedVehicles.get(0);
                canProceed = firstVehicle.getId().equals(logicalVehicle.getId());
                
                if (canProceed) {
                    // Es el próximo vehículo según la cola de prioridad
                    String priorityInfo = firstVehicle.getType() == VehicleTypeEnum.EMERGENCY ? 
                            " (PRIORIDAD: Emergencia)" : " (PRIORIDAD: Orden de llegada)";
                    
                    Platform.runLater(() -> {
                        log("✅ Vehículo " + logicalVehicle.getId() + priorityInfo + " autorizado para cruzar");
                        
                        // === SISTEMA DE COLISIONES: Decrementar contador al cruzar ===
                        decrementWaitingCounter(entryPoint);
                        
                        executeCompleteMovement(vehicle, logicalVehicle, entryPoint, turn);
                    });
                } else {
                    // No es su turno según la cola de prioridad
                    String waitReason;
                    if (firstVehicle.getType() == VehicleTypeEnum.EMERGENCY && logicalVehicle.getType() != VehicleTypeEnum.EMERGENCY) {
                        waitReason = " (esperando a vehículo de emergencia)";
                    } else {
                        waitReason = " (esperando su turno, posición en cola: " + 
                                      (queuedVehicles.indexOf(logicalVehicle) + 1) + " de " + 
                                      queuedVehicles.size() + ")";
                    }
                    
                    final String reasonToWait = waitReason;
                    Platform.runLater(() -> {
                        log("⏱️ Vehículo " + logicalVehicle.getId() + reasonToWait);
                        
                        // Reintentar después de 1 segundo
                        Thread retryThread = new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(() -> waitForAuthorizationAndCross(vehicle, logicalVehicle, entryPoint, turn));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                        retryThread.setDaemon(true);
                        retryThread.start();
                    });
                }
                
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        
        authorizationChecker.setDaemon(true);
        authorizationChecker.start();
    }

    /**
     * FASE 3: Ejecuta el movimiento completo a través de la intersección
     * Implementación para U-TURNS desde TODAS las direcciones
     */
    private void executeCompleteMovement(VehicleView vehicle, Vehicle logicalVehicle, String entryPoint, DirectionEnum turn) {
        // Verificar si el vehículo todavía está en la cola antes de intentar removerlo
        // (El TrafficController podría haberlo removido ya)
        if (!logicalVehicle.isInIntersection()) {
            // Solo intentar remover si aún no está en la intersección
            logicalIntersection.removeVehicle(logicalVehicle);
            logicalVehicle.setInIntersection(true);
        }
        
        // === MARCAR ANIMACIÓN DE CRUCE EN PROGRESO ===
        crossingAnimationInProgress = true;
        
        // === ACTUALIZAR VISUALMENTE LA COLA DE VEHÍCULOS ===
        updateWaitingVehiclesPriority();
        
        // === IMPLEMENTAR MOVIMIENTOS ANGULARES PARA TODAS LAS DIRECCIONES ===
        switch (entryPoint) {
            case "sur" -> {
                switch (turn) {
                    case U_TURN -> executeUTurnFromSouth(vehicle, logicalVehicle);
                    case RIGHT -> executeRightTurnFromSouth(vehicle, logicalVehicle);
                    case LEFT -> executeLeftTurnFromSouth(vehicle, logicalVehicle);
                    case STRAIGHT -> executeStraightFromSouth(vehicle, logicalVehicle);
                }
            }
            case "norte" -> {
                switch (turn) {
                    case U_TURN -> executeUTurnFromNorth(vehicle, logicalVehicle);
                    case RIGHT -> executeRightTurnFromNorth(vehicle, logicalVehicle);
                    case LEFT -> executeLeftTurnFromNorth(vehicle, logicalVehicle);
                    case STRAIGHT -> executeStraightFromNorth(vehicle, logicalVehicle);
                }
            }
            case "este" -> {
                switch (turn) {
                    case U_TURN -> executeUTurnFromEast(vehicle, logicalVehicle);
                    case RIGHT -> executeRightTurnFromEast(vehicle, logicalVehicle);
                    case LEFT -> executeLeftTurnFromEast(vehicle, logicalVehicle);
                    case STRAIGHT -> executeStraightFromEast(vehicle, logicalVehicle);
                }
            }
            case "oeste" -> {
                switch (turn) {
                    case U_TURN -> executeUTurnFromWest(vehicle, logicalVehicle);
                    case RIGHT -> executeRightTurnFromWest(vehicle, logicalVehicle);
                    case LEFT -> executeLeftTurnFromWest(vehicle, logicalVehicle);
                    case STRAIGHT -> executeStraightFromWest(vehicle, logicalVehicle);
                }
            }
            default -> executeSimpleMovement(vehicle, logicalVehicle, entryPoint, turn);
        }
    }
    
    /**
     * Actualiza la visualización de prioridad de todos los vehículos en espera
     * Se ejecuta después de que un vehículo sale de la intersección
     */
    private void updateWaitingVehiclesPriority() {
        // Obtener la lista actualizada de vehículos en la cola
        List<Vehicle> queuedVehicles = logicalIntersection.peekAllVehicles();
        
        if (queuedVehicles.isEmpty()) {
            return;  // No hay vehículos esperando
        }
        
        // Actualizar el estado del próximo vehículo en la cola
        Vehicle nextVehicle = queuedVehicles.get(0);
        
        // Buscar la representación visual del vehículo en el panel
        for (javafx.scene.Node node : intersectionPane.getChildren()) {
            if (node instanceof VehicleView vehicleView) {
                // Intentar encontrar el vehículo correspondiente en la cola
                for (Vehicle queuedVehicle : queuedVehicles) {
                    // Si este es el siguiente en la cola, destacarlo
                    if (queuedVehicle.equals(nextVehicle)) {
                        vehicleView.setNextInQueueVisual(true, nextVehicle.getType() == VehicleTypeEnum.EMERGENCY);
                        
                        // Mostrar mensaje en el log indicando cuál es el próximo vehículo
                        log("🚦 Próximo vehículo a cruzar: " + nextVehicle.getId() + 
                           (nextVehicle.getType() == VehicleTypeEnum.EMERGENCY ? " (EMERGENCIA)" : ""));
                        
                        break;
                    } else {
                        // No es el próximo, quitar cualquier destacado
                        vehicleView.setNextInQueueVisual(false, false);
                    }
                }
            }
        }
    }

    /**
     * Implementación específica del U-Turn desde el Sur
     * CORREGIDO: Trayectoria angular - izquierda luego hacia abajo
     */
    private void executeUTurnFromSouth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del sur
        double startX = CENTER + QUARTER_ROAD_WIDTH;
        double startY = CENTER + HALF_ROAD_WIDTH;
        
        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la izquierda ===
        double midX = CENTER - QUARTER_ROAD_WIDTH;  // Carril opuesto (horizontal)
        double midY = CENTER + HALF_ROAD_WIDTH;     // Misma altura Y
        
        // === PUNTO FINAL: Bajar verticalmente hacia la salida ===
        double finalX = CENTER - QUARTER_ROAD_WIDTH; // Misma X que el punto intermedio
        double finalY = SIZE;                         // Salida por donde vino
        
        // Crear path angular con líneas rectas (más realista)
        Path uTurnPath = new Path();
        uTurnPath.getElements().add(new MoveTo(startX, startY));
        
        // MOVIMIENTO 1: Línea horizontal hacia la izquierda
        LineTo horizontalMove = new LineTo(midX, midY);
        
        // MOVIMIENTO 2: Línea vertical hacia abajo
        LineTo verticalMove = new LineTo(finalX, finalY);
        
        uTurnPath.getElements().addAll(horizontalMove, verticalMove);
        
        // Animación del U-turn completo (4 segundos)
        PathTransition uTurnTransition = new PathTransition(Duration.seconds(4), uTurnPath, vehicle);
        uTurnTransition.setCycleCount(1);
        
        uTurnTransition.setOnFinished(e -> {
            finishVehicleAnimation(vehicle, logicalVehicle, startX, startY, finalX, finalY,
                                 "U-turn angular desde sur");
        });
        
        uTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 U-Turn Sur: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Implementación específica del U-Turn desde el Norte
     * Trayectoria: hacia la derecha luego hacia arriba
     */
    private void executeUTurnFromNorth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del norte
        double startX = CENTER - QUARTER_ROAD_WIDTH;
        double startY = CENTER - HALF_ROAD_WIDTH;
        
        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la derecha ===
        double midX = CENTER + QUARTER_ROAD_WIDTH;  // Carril opuesto (horizontal)
        double midY = CENTER - HALF_ROAD_WIDTH;     // Misma altura Y
        
        // === PUNTO FINAL: Subir verticalmente hacia la salida ===
        double finalX = CENTER + QUARTER_ROAD_WIDTH; // Misma X que el punto intermedio
        double finalY = 0;                            // Salida por donde vino
        
        // Crear path angular con líneas rectas
        Path uTurnPath = new Path();
        uTurnPath.getElements().add(new MoveTo(startX, startY));
        
        // MOVIMIENTO 1: Línea horizontal hacia la derecha
        LineTo horizontalMove = new LineTo(midX, midY);
        
        // MOVIMIENTO 2: Línea vertical hacia arriba
        LineTo verticalMove = new LineTo(finalX, finalY);
        
        uTurnPath.getElements().addAll(horizontalMove, verticalMove);
        
        // Animación del U-turn completo (4 segundos)
        PathTransition uTurnTransition = new PathTransition(Duration.seconds(4), uTurnPath, vehicle);
        uTurnTransition.setCycleCount(1);
        
        uTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("🔄 Vehículo " + logicalVehicle.getId() + " completó U-turn angular desde norte");
        });
        
        uTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 U-Turn Norte: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Implementación específica del U-Turn desde el Este
     * Trayectoria: hacia arriba luego hacia la derecha
     */
    private void executeUTurnFromEast(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del este
        double startX = CENTER + HALF_ROAD_WIDTH;
        double startY = CENTER - QUARTER_ROAD_WIDTH;
        
        // === PUNTO INTERMEDIO: Moverse verticalmente hacia arriba ===
        double midX = CENTER + HALF_ROAD_WIDTH;     // Misma X
        double midY = CENTER + QUARTER_ROAD_WIDTH;  // Carril opuesto (vertical)
        
        // === PUNTO FINAL: Moverse horizontalmente hacia la derecha (salida) ===
        double finalX = SIZE;                        // Salida por donde vino
        double finalY = CENTER + QUARTER_ROAD_WIDTH; // Misma Y que el punto intermedio
        
        // Crear path angular con líneas rectas
        Path uTurnPath = new Path();
        uTurnPath.getElements().add(new MoveTo(startX, startY));
        
        // MOVIMIENTO 1: Línea vertical hacia arriba
        LineTo verticalMove = new LineTo(midX, midY);
        
        // MOVIMIENTO 2: Línea horizontal hacia la derecha
        LineTo horizontalMove = new LineTo(finalX, finalY);
        
        uTurnPath.getElements().addAll(verticalMove, horizontalMove);
        
        // Animación del U-turn completo (4 segundos)
        PathTransition uTurnTransition = new PathTransition(Duration.seconds(4), uTurnPath, vehicle);
        uTurnTransition.setCycleCount(1);
        
        uTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("🔄 Vehículo " + logicalVehicle.getId() + " completó U-turn angular desde este");
        });
        
        uTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 U-Turn Este: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Implementación específica del U-Turn desde el Oeste
     * Trayectoria: hacia abajo luego hacia la izquierda
     */
    private void executeUTurnFromWest(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del oeste
        double startX = CENTER - HALF_ROAD_WIDTH;
        double startY = CENTER + QUARTER_ROAD_WIDTH;
        
        // === PUNTO INTERMEDIO: Moverse verticalmente hacia abajo ===
        double midX = CENTER - HALF_ROAD_WIDTH;     // Misma X
        double midY = CENTER - QUARTER_ROAD_WIDTH;  // Carril opuesto (vertical)
        
        // === PUNTO FINAL: Moverse horizontalmente hacia la izquierda (salida) ===
        double finalX = 0;                           // Salida por donde vino
        double finalY = CENTER - QUARTER_ROAD_WIDTH; // Misma Y que el punto intermedio
        
        // Crear path angular con líneas rectas
        Path uTurnPath = new Path();
        uTurnPath.getElements().add(new MoveTo(startX, startY));
        
        // MOVIMIENTO 1: Línea vertical hacia abajo
        LineTo verticalMove = new LineTo(midX, midY);
        
        // MOVIMIENTO 2: Línea horizontal hacia la izquierda
        LineTo horizontalMove = new LineTo(finalX, finalY);
        
        uTurnPath.getElements().addAll(verticalMove, horizontalMove);
        
        // Animación del U-turn completo (4 segundos)
        PathTransition uTurnTransition = new PathTransition(Duration.seconds(4), uTurnPath, vehicle);
        uTurnTransition.setCycleCount(1);
        
        uTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("🔄 Vehículo " + logicalVehicle.getId() + " completó U-turn angular desde oeste");
        });
        
        uTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 U-Turn Oeste: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    // ========== MOVIMIENTOS DESDE EL SUR ==========
    /**
     * Implementación específica del giro a la DERECHA desde el Sur
     * CORREGIDO: Trayectoria angular - hacia la DERECHA (este) luego hacia arriba
     */
    private void executeRightTurnFromSouth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del sur
        double startX = CENTER + QUARTER_ROAD_WIDTH;
        double startY = CENTER + HALF_ROAD_WIDTH;
        
        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la DERECHA (este) ===
        double midX = CENTER + HALF_ROAD_WIDTH;     // Hacia la DERECHA en coordenadas (este)
        double midY = CENTER + QUARTER_ROAD_WIDTH;  // Carril este INFERIOR (más cerca del centro)
        
        // === PUNTO FINAL: Salir hacia el este ===
        double finalX = SIZE;                       // Salida por el este (derecha)
        double finalY = CENTER + QUARTER_ROAD_WIDTH; // Carril este INFERIOR (correcto para giro derecha)
        
        // Crear path angular con líneas rectas
        Path rightTurnPath = new Path();
        rightTurnPath.getElements().add(new MoveTo(startX, startY));
        
        // MOVIMIENTO 1: Línea vertical hacia el centro (ajustar altura)
        LineTo verticalMove = new LineTo(startX, midY);
        
        // MOVIMIENTO 2: Línea horizontal hacia la DERECHA (este)
        LineTo horizontalMove = new LineTo(finalX, finalY);
        
        rightTurnPath.getElements().addAll(verticalMove, horizontalMove);
        
        // Animación del giro a la derecha (3 segundos)
        PathTransition rightTurnTransition = new PathTransition(Duration.seconds(3), rightTurnPath, vehicle);
        rightTurnTransition.setCycleCount(1);
        
        rightTurnTransition.setOnFinished(e -> {
            finishVehicleAnimation(vehicle, logicalVehicle, startX, startY, finalX, finalY,
                                 "giro a la DERECHA desde sur");
        });
        
        rightTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Giro Derecha Sur: (" + (int)startX + "," + (int)startY + ") → (" + (int)startX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un giro a la izquierda desde el sur usando movimiento angular
     */
    private void executeLeftTurnFromSouth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del sur
        double startX = CENTER + QUARTER_ROAD_WIDTH;
        double startY = CENTER + HALF_ROAD_WIDTH;
        
        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la IZQUIERDA (oeste) ===
        double midX = CENTER - QUARTER_ROAD_WIDTH;  // Hacia la IZQUIERDA en coordenadas (oeste)
        double midY = CENTER - QUARTER_ROAD_WIDTH;  // Carril oeste SUPERIOR (más lejos del centro)
        
        // === PUNTO FINAL: Salir hacia el oeste ===
        double finalX = 0;                          // Salida por el oeste (izquierda)
        double finalY = CENTER - QUARTER_ROAD_WIDTH; // Carril oeste SUPERIOR (correcto para giro izquierda)
        
        // Crear path angular con líneas rectas
        Path leftTurnPath = new Path();
        leftTurnPath.getElements().add(new MoveTo(startX, startY));
        
        // MOVIMIENTO 1: Línea vertical hacia el centro (ajustar altura)
        LineTo verticalMove = new LineTo(startX, midY);
        
        // MOVIMIENTO 2: Línea horizontal hacia la IZQUIERDA (oeste)
        LineTo horizontalMove = new LineTo(finalX, finalY);
        
        leftTurnPath.getElements().addAll(verticalMove, horizontalMove);
        
        // Animación del giro a la izquierda (3 segundos)
        PathTransition leftTurnTransition = new PathTransition(Duration.seconds(3), leftTurnPath, vehicle);
        leftTurnTransition.setCycleCount(1);
        
        leftTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("⬅️ Vehículo " + logicalVehicle.getId() + " completó giro a la IZQUIERDA desde sur");
        });
        
        leftTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Giro Izquierda Sur: (" + (int)startX + "," + (int)startY + ") → (" + (int)startX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un movimiento directo (recto) desde el sur hacia el norte
     */
    private void executeStraightFromSouth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del sur
        double startX = CENTER + QUARTER_ROAD_WIDTH;
        double startY = CENTER + HALF_ROAD_WIDTH;
        
        // === PUNTO FINAL: Salir hacia el norte (directo) ===
        double finalX = CENTER + QUARTER_ROAD_WIDTH;  // Mismo carril (mantener X)
        double finalY = 0;                            // Salida por el norte
        
        // Crear path directo (línea recta)
        Path straightPath = new Path();
        straightPath.getElements().add(new MoveTo(startX, startY));
        straightPath.getElements().add(new LineTo(finalX, finalY));
        
        // Animación del movimiento directo (2 segundos)
        PathTransition straightTransition = new PathTransition(Duration.seconds(2), straightPath, vehicle);
        straightTransition.setCycleCount(1);
        
        straightTransition.setOnFinished(e -> {
            finishVehicleAnimation(vehicle, logicalVehicle, startX, startY, finalX, finalY,
                                 "movimiento DIRECTO desde sur");
        });
        
        straightTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Movimiento Directo Sur: (" + (int)startX + "," + (int)startY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    // ========== MOVIMIENTOS DESDE EL NORTE ==========
    
    /**
     * Ejecuta un giro a la derecha desde el norte usando movimiento angular
     */
    private void executeRightTurnFromNorth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del norte
        double startX = CENTER - QUARTER_ROAD_WIDTH;
        double startY = CENTER - HALF_ROAD_WIDTH;

        // === PUNTO INTERMEDIO: Moverse verticalmente hacia abajo, luego horizontal hacia la derecha ===
        double midX = CENTER - QUARTER_ROAD_WIDTH;  // Mantener X
        double midY = CENTER - QUARTER_ROAD_WIDTH;  // Nivel de giro (más abajo)

        // === PUNTO FINAL: Salir hacia el oeste (derecha) ===
        double finalX = 0;                          // Salida por el este
        double finalY = CENTER - QUARTER_ROAD_WIDTH; // Carril superior del este

        // Crear path angular con líneas rectas
        Path leftTurnPath = new Path();
        leftTurnPath.getElements().add(new MoveTo(startX, startY));

        // MOVIMIENTO 1: Línea vertical hacia el centro (ajustar altura)
        LineTo verticalMove = new LineTo(midX, midY);

        // MOVIMIENTO 2: Línea horizontal hacia la derecha (este)
        LineTo horizontalMove = new LineTo(finalX, finalY);

        leftTurnPath.getElements().addAll(verticalMove, horizontalMove);

        // Animación del giro a la derecha (3 segundos)
        PathTransition leftTurnTransition = new PathTransition(Duration.seconds(3), leftTurnPath, vehicle);
        leftTurnTransition.setCycleCount(1);

        leftTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("⬅️ Vehículo " + logicalVehicle.getId() + " completó giro a la IZQUIERDA desde norte");
        });

        leftTurnTransition.play();

        // Log detallado de la trayectoria
        log("📍 Giro Derecha Norte: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un giro a la izquierda desde el norte usando movimiento angular
     */
    private void executeLeftTurnFromNorth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del norte
        double startX = CENTER - QUARTER_ROAD_WIDTH;
        double startY = CENTER - HALF_ROAD_WIDTH;

        // === PUNTO INTERMEDIO: Moverse verticalmente hacia abajo, luego horizontal hacia la izquierda ===
        double midX = CENTER - QUARTER_ROAD_WIDTH;  // Mantener X
        double midY = CENTER + QUARTER_ROAD_WIDTH;  // Nivel de giro

        // === PUNTO FINAL: Salir hacia el este (izquierda) ===
        double finalX = SIZE;                       // Salida por el este
        double finalY = CENTER + QUARTER_ROAD_WIDTH; // Carril inferior del este

        // Crear path angular con líneas rectas
        Path rightTurnPath = new Path();
        rightTurnPath.getElements().add(new MoveTo(startX, startY));

        // MOVIMIENTO 1: Línea vertical hacia el centro (ajustar altura)
        LineTo verticalMove = new LineTo(midX, midY);

        // MOVIMIENTO 2: Línea horizontal hacia la izquirda (oeste)
        LineTo horizontalMove = new LineTo(finalX, finalY);

        rightTurnPath.getElements().addAll(verticalMove, horizontalMove);

        // Animación del giro a la izquierda (3 segundos)
        PathTransition rightTurnTransition = new PathTransition(Duration.seconds(3), rightTurnPath, vehicle);
        rightTurnTransition.setCycleCount(1);

        rightTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("➡️ Vehículo " + logicalVehicle.getId() + " completó giro a la DERECHA desde norte");
        });

        rightTurnTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Giro Izquierda Norte: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un movimiento directo (recto) desde el norte hacia el sur
     */
    private void executeStraightFromNorth(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del norte
        double startX = CENTER - QUARTER_ROAD_WIDTH;
        double startY = CENTER - HALF_ROAD_WIDTH;
        
        // === PUNTO FINAL: Salir hacia el sur (directo) ===
        double finalX = CENTER - QUARTER_ROAD_WIDTH;  // Mismo carril (mantener X)
        double finalY = SIZE;                         // Salida por el sur
        
        // Crear path directo (línea recta)
        Path straightPath = new Path();
        straightPath.getElements().add(new MoveTo(startX, startY));
        straightPath.getElements().add(new LineTo(finalX, finalY));
        
        // Animación del movimiento directo (2 segundos)
        PathTransition straightTransition = new PathTransition(Duration.seconds(2), straightPath, vehicle);
        straightTransition.setCycleCount(1);
        
        straightTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("⬇️ Vehículo " + logicalVehicle.getId() + " completó movimiento DIRECTO desde norte");
        });
        
        straightTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Movimiento Directo Norte: (" + (int)startX + "," + (int)startY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    // ========== MOVIMIENTOS DESDE EL ESTE ==========

    /**
     * Ejecuta un giro a la derecha desde el este usando movimiento angular
     */
    private void executeRightTurnFromEast(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del este
        double startX = CENTER + HALF_ROAD_WIDTH;
        double startY = CENTER - QUARTER_ROAD_WIDTH;

        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la izquierda, luego vertical hacia arriba ===
        double midX = CENTER + QUARTER_ROAD_WIDTH;  // Nivel de giro (más a la izquierda)
        double midY = CENTER - QUARTER_ROAD_WIDTH;  // Mantener Y

        // === PUNTO FINAL: Salir hacia el norte (izquierda desde este) ===
        double finalX = CENTER + QUARTER_ROAD_WIDTH; // Carril izquierdo del norte
        double finalY = 0;                           // Salida por el norte

        // Crear path angular con líneas rectas
        Path rightTurnPath = new Path();
        rightTurnPath.getElements().add(new MoveTo(startX, startY));

        // MOVIMIENTO 1: Línea horizontal hacia el centro (ajustar anchura)
        LineTo horizontalMove = new LineTo(midX, midY);

        // MOVIMIENTO 2: Línea vertical hacia ARRIBA (norte)
        LineTo verticalMove = new LineTo(finalX, finalY);

        rightTurnPath.getElements().addAll(horizontalMove, verticalMove);

        // Animación del giro a la derecha (3 segundos)
        PathTransition rightTurnTransition = new PathTransition(Duration.seconds(3), rightTurnPath, vehicle);
        rightTurnTransition.setCycleCount(1);

        rightTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("➡️ Vehículo " + logicalVehicle.getId() + " completó giro a la DERECHA desde este");
        });

        rightTurnTransition.play();

        // Log detallado de la trayectoria
        log("📍 Giro Derecha Este: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un giro a la izquierda desde el este usando movimiento angular
     */
    private void executeLeftTurnFromEast(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del este
        double startX = CENTER + HALF_ROAD_WIDTH;
        double startY = CENTER - QUARTER_ROAD_WIDTH;

        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la izquierda, luego vertical hacia abajo ===
        double midX = CENTER - QUARTER_ROAD_WIDTH;  // Nivel de giro
        double midY = CENTER - QUARTER_ROAD_WIDTH;  // Mantener Y

        // === PUNTO FINAL: Salir hacia el sur (derecha desde este) ===
        double finalX = CENTER - QUARTER_ROAD_WIDTH; // Carril derecho del sur
        double finalY = SIZE;                        // Salida por el sur

        // Crear path angular con líneas rectas
        Path leftTurnPath = new Path();
        leftTurnPath.getElements().add(new MoveTo(startX, startY));

        // MOVIMIENTO 1: Línea horizontal hacia el centro (ajustar anchura)
        LineTo horizontalMove = new LineTo(midX, midY);

        // MOVIMIENTO 2: Línea vertical hacia ABAJO (sur)
        LineTo verticalMove = new LineTo(finalX, finalY);

        leftTurnPath.getElements().addAll(horizontalMove, verticalMove);

        // Animación del giro a la izquierda (3 segundos)
        PathTransition leftTurnTransition = new PathTransition(Duration.seconds(3), leftTurnPath, vehicle);
        leftTurnTransition.setCycleCount(1);

        leftTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("⬅️ Vehículo " + logicalVehicle.getId() + " completó giro a la IZQUIERDA desde este");
        });

        leftTurnTransition.play();

        // Log detallado de la trayectoria
        log("📍 Giro Izquierda Este: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }


    /**
     * Ejecuta un movimiento directo (recto) desde el este hacia el oeste
     */
    private void executeStraightFromEast(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del este
        double startX = CENTER + HALF_ROAD_WIDTH;
        double startY = CENTER - QUARTER_ROAD_WIDTH;
        
        // === PUNTO FINAL: Salir hacia el oeste (directo) ===
        double finalX = 0;                            // Salida por el oeste
        double finalY = CENTER - QUARTER_ROAD_WIDTH;  // Mismo carril (mantener Y)
        
        // Crear path directo (línea recta)
        Path straightPath = new Path();
        straightPath.getElements().add(new MoveTo(startX, startY));
        straightPath.getElements().add(new LineTo(finalX, finalY));
        
        // Animación del movimiento directo (2 segundos)
        PathTransition straightTransition = new PathTransition(Duration.seconds(2), straightPath, vehicle);
        straightTransition.setCycleCount(1);
        
        straightTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("⬅️ Vehículo " + logicalVehicle.getId() + " completó movimiento DIRECTO desde este");
        });
        
        straightTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Movimiento Directo Este: (" + (int)startX + "," + (int)startY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    // ========== MOVIMIENTOS DESDE EL OESTE ==========

    /**
     * Ejecuta un giro a la derecha desde el oeste usando movimiento angular
     */
    private void executeRightTurnFromWest(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del oeste
        double startX = CENTER - HALF_ROAD_WIDTH;
        double startY = CENTER + QUARTER_ROAD_WIDTH;

        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la derecha, luego vertical hacia abajo ===
        double midX = CENTER - QUARTER_ROAD_WIDTH;  // Nivel de giro (más a la derecha)
        double midY = CENTER + QUARTER_ROAD_WIDTH;  // Mantener Y

        // === PUNTO FINAL: Salir hacia el sur (izquierda desde oeste) ===
        double finalX = CENTER - QUARTER_ROAD_WIDTH; // Carril izquierdo del sur
        double finalY = SIZE;                        // Salida por el sur

        // Crear path angular con líneas rectas
        Path rightTurnPath = new Path();
        rightTurnPath.getElements().add(new MoveTo(startX, startY));

        // MOVIMIENTO 1: Línea horizontal hacia el centro (ajustar anchura)
        LineTo horizontalMove = new LineTo(midX, midY);

        // MOVIMIENTO 2: Línea vertical hacia ABAJO (sur)
        LineTo verticalMove = new LineTo(finalX, finalY);

        rightTurnPath.getElements().addAll(horizontalMove, verticalMove);

        // Animación del giro a la derecha (3 segundos)
        PathTransition rightTurnTransition = new PathTransition(Duration.seconds(3), rightTurnPath, vehicle);
        rightTurnTransition.setCycleCount(1);

        rightTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("➡️ Vehículo " + logicalVehicle.getId() + " completó giro a la DERECHA desde oeste");
        });

        rightTurnTransition.play();

        // Log detallado de la trayectoria
        log("📍 Giro Derecha Oeste: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un giro a la izquierda desde el oeste usando movimiento angular
     */
    private void executeLeftTurnFromWest(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del oeste
        double startX = CENTER - HALF_ROAD_WIDTH;
        double startY = CENTER + QUARTER_ROAD_WIDTH;

        // === PUNTO INTERMEDIO: Moverse horizontalmente hacia la derecha, luego vertical hacia arriba ===
        double midX = CENTER + QUARTER_ROAD_WIDTH;  // Nivel de giro
        double midY = CENTER + QUARTER_ROAD_WIDTH;  // Mantener Y

        // === PUNTO FINAL: Salir hacia el norte (derecha desde oeste) ===
        double finalX = CENTER + QUARTER_ROAD_WIDTH; // Carril derecho del norte
        double finalY = 0;                           // Salida por el norte

        // Crear path angular con líneas rectas
        Path leftTurnPath = new Path();
        leftTurnPath.getElements().add(new MoveTo(startX, startY));

        // MOVIMIENTO 1: Línea horizontal hacia el centro (ajustar anchura)
        LineTo horizontalMove = new LineTo(midX, midY);

        // MOVIMIENTO 2: Línea vertical hacia ARRIBA (norte)
        LineTo verticalMove = new LineTo(finalX, finalY);

        leftTurnPath.getElements().addAll(horizontalMove, verticalMove);

        // Animación del giro a la izquierda (3 segundos)
        PathTransition leftTurnTransition = new PathTransition(Duration.seconds(3), leftTurnPath, vehicle);
        leftTurnTransition.setCycleCount(1);

        leftTurnTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("⬅️ Vehículo " + logicalVehicle.getId() + " completó giro a la IZQUIERDA desde oeste");
        });

        leftTurnTransition.play();

        // Log detallado de la trayectoria
        log("📍 Giro Izquierda Oeste: (" + (int)startX + "," + (int)startY + ") → (" + (int)midX + "," + (int)midY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }

    /**
     * Ejecuta un movimiento directo (recto) desde el oeste hacia el este
     */
    private void executeStraightFromWest(VehicleView vehicle, Vehicle logicalVehicle) {
        // Posición actual: línea de parada del oeste
        double startX = CENTER - HALF_ROAD_WIDTH;
        double startY = CENTER + QUARTER_ROAD_WIDTH;
        
        // === PUNTO FINAL: Salir hacia el este (directo) ===
        double finalX = SIZE;                         // Salida por el este
        double finalY = CENTER + QUARTER_ROAD_WIDTH;  // Mismo carril (mantener Y)
        
        // Crear path directo (línea recta)
        Path straightPath = new Path();
        straightPath.getElements().add(new MoveTo(startX, startY));
        straightPath.getElements().add(new LineTo(finalX, finalY));
        
        // Animación del movimiento directo (2 segundos)
        PathTransition straightTransition = new PathTransition(Duration.seconds(2), straightPath, vehicle);
        straightTransition.setCycleCount(1);
        
        straightTransition.setOnFinished(e -> {
            logicalVehicle.setInIntersection(false);
            intersectionPane.getChildren().remove(vehicle);
            showLaneFeedback(startX, startY, finalX, finalY);
            log("➡️ Vehículo " + logicalVehicle.getId() + " completó movimiento DIRECTO desde oeste");
        });
        
        straightTransition.play();
        
        // Log detallado de la trayectoria
        log("📍 Movimiento Directo Oeste: (" + (int)startX + "," + (int)startY + ") → (" + (int)finalX + "," + (int)finalY + ")");
    }


    /**
     * Movimiento simple para otros casos (por ahora)
     */
    private void executeSimpleMovement(VehicleView vehicle, Vehicle logicalVehicle, String entryPoint, DirectionEnum turn) {
        // Por ahora, usar la lógica original para otros movimientos
        double[] stopPos = getStopLinePosition(entryPoint);
        double[] endPos = getExitPosition(entryPoint, turn);
        
        Line crossingPath = new Line(stopPos[0], stopPos[1], endPos[0], endPos[1]);
        PathTransition crossingTransition = new PathTransition(Duration.seconds(3), crossingPath, vehicle);
        crossingTransition.setCycleCount(1);
        
        crossingTransition.setOnFinished(e -> {
            finishVehicleAnimation(vehicle, logicalVehicle, stopPos[0], stopPos[1], endPos[0], endPos[1],
                                 "movimiento " + turn);
        });
        
        crossingTransition.play();
    }

    /**
     * Calcula la posición de inicio según el punto de entrada
     */
    private double[] getStartPosition(String entryPoint) {
        return switch (entryPoint) {
            case "norte" -> new double[]{CENTER - QUARTER_ROAD_WIDTH, 0};
            case "sur" -> new double[]{CENTER + QUARTER_ROAD_WIDTH, SIZE};
            case "este" -> new double[]{SIZE, CENTER - QUARTER_ROAD_WIDTH};
            case "oeste" -> new double[]{0, CENTER + QUARTER_ROAD_WIDTH};
            default -> new double[]{CENTER, CENTER};
        };
    }

    /**
     * Calcula la posición de parada antes del cruce
     */
    private double[] getStopLinePosition(String entryPoint) {
        return switch (entryPoint) {
            case "norte" -> new double[]{CENTER - QUARTER_ROAD_WIDTH, CENTER - HALF_ROAD_WIDTH};
            case "sur" -> new double[]{CENTER + QUARTER_ROAD_WIDTH, CENTER + HALF_ROAD_WIDTH};
            case "este" -> new double[]{CENTER + HALF_ROAD_WIDTH, CENTER - QUARTER_ROAD_WIDTH};
            case "oeste" -> new double[]{CENTER - HALF_ROAD_WIDTH, CENTER + QUARTER_ROAD_WIDTH};
            default -> new double[]{CENTER, CENTER};
        };
    }

    /**
     * Calcula la posición de salida según entrada y giro
     */
    private double[] getExitPosition(String entryPoint, DirectionEnum turn) {
        // Por ahora, implementación simple
        return switch (entryPoint) {
            case "norte" -> switch (turn) {
                case STRAIGHT -> new double[]{CENTER - QUARTER_ROAD_WIDTH, SIZE};
                case RIGHT -> new double[]{0, CENTER + QUARTER_ROAD_WIDTH};
                case LEFT -> new double[]{SIZE, CENTER - QUARTER_ROAD_WIDTH};
                case U_TURN -> new double[]{CENTER + QUARTER_ROAD_WIDTH, 0};
            };
            case "sur" -> switch (turn) {
                case STRAIGHT -> new double[]{CENTER + QUARTER_ROAD_WIDTH, 0};
                case RIGHT -> new double[]{SIZE, CENTER - QUARTER_ROAD_WIDTH};
                case LEFT -> new double[]{0, CENTER + QUARTER_ROAD_WIDTH};
                case U_TURN -> new double[]{CENTER - QUARTER_ROAD_WIDTH, SIZE};
            };
            case "este" -> switch (turn) {
                case STRAIGHT -> new double[]{0, CENTER - QUARTER_ROAD_WIDTH};
                case RIGHT -> new double[]{CENTER - QUARTER_ROAD_WIDTH, 0};
                case LEFT -> new double[]{CENTER + QUARTER_ROAD_WIDTH, SIZE};
                case U_TURN -> new double[]{SIZE, CENTER + QUARTER_ROAD_WIDTH};
            };
            case "oeste" -> switch (turn) {
                case STRAIGHT -> new double[]{SIZE, CENTER + QUARTER_ROAD_WIDTH};
                case RIGHT -> new double[]{CENTER + QUARTER_ROAD_WIDTH, SIZE};
                case LEFT -> new double[]{CENTER - QUARTER_ROAD_WIDTH, 0};
                case U_TURN -> new double[]{0, CENTER - QUARTER_ROAD_WIDTH};
            };
            default -> new double[]{CENTER, CENTER};
        };
    }

    /**
     * Obtiene los colores del vehículo según el punto de entrada
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

    // === MÉTODOS DEL SISTEMA DE COLISIONES ===
    
    /**
     * Incrementa el contador de vehículos esperando en una dirección específica
     * @param entryPoint La dirección de entrada del vehículo
     * @return La posición en la cola (0 = primer vehículo, 1 = segundo, etc.)
     */
    private int incrementWaitingCounter(String entryPoint) {
        switch (entryPoint) {
            case "norte" -> vehiclesWaitingNorth++;
            case "sur" -> vehiclesWaitingSouth++;
            case "este" -> vehiclesWaitingEast++;
            case "oeste" -> vehiclesWaitingWest++;
        }
        return getCurrentWaitingCount(entryPoint) - 1; // Retorna posición 0-indexada
    }
    
    /**
     * Decrementa el contador de vehículos esperando en una dirección específica
     * @param entryPoint La dirección de entrada del vehículo que se está moviendo
     */
    private void decrementWaitingCounter(String entryPoint) {
        switch (entryPoint) {
            case "norte" -> {
                if (vehiclesWaitingNorth > 0) vehiclesWaitingNorth--;
            }
            case "sur" -> {
                if (vehiclesWaitingSouth > 0) vehiclesWaitingSouth--;
            }
            case "este" -> {
                if (vehiclesWaitingEast > 0) vehiclesWaitingEast--;
            }
            case "oeste" -> {
                if (vehiclesWaitingWest > 0) vehiclesWaitingWest--;
            }
        }
    }
    
    /**
     * Obtiene el número actual de vehículos esperando en una dirección
     * @param entryPoint La dirección de entrada
     * @return El número de vehículos esperando
     */
    private int getCurrentWaitingCount(String entryPoint) {
        return switch (entryPoint) {
            case "norte" -> vehiclesWaitingNorth;
            case "sur" -> vehiclesWaitingSouth;
            case "este" -> vehiclesWaitingEast;
            case "oeste" -> vehiclesWaitingWest;
            default -> 0;
        };
    }
    
    /**
     * Calcula la posición de parada con spacing para evitar colisiones
     * @param entryPoint La dirección de entrada del vehículo
     * @param queuePosition La posición en la cola (0 = primero, 1 = segundo, etc.)
     * @return Array con las coordenadas [x, y] de la posición de parada con spacing
     */
    private double[] getStopLinePositionWithSpacing(String entryPoint, int queuePosition) {
        // Obtener la posición base de la línea de parada
        double[] basePos = getStopLinePosition(entryPoint);
        double x = basePos[0];
        double y = basePos[1];
        
        // Aplicar spacing según la dirección de entrada
        double offset = queuePosition * VEHICLE_SPACING;
        
        switch (entryPoint) {
            case "norte" -> {
                // Los vehículos se alinean hacia atrás (menor Y)
                y = basePos[1] - offset;
            }
            case "sur" -> {
                // Los vehículos se alinean hacia atrás (mayor Y)
                y = basePos[1] + offset;
            }
            case "este" -> {
                // Los vehículos se alinean hacia atrás (mayor X)
                x = basePos[0] + offset;
            }
            case "oeste" -> {
                // Los vehículos se alinean hacia atrás (menor X)
                x = basePos[0] - offset;
            }
        }
        
        return new double[]{x, y};
    }

    /**
     * Método público para verificar si hay una animación de cruce en progreso.
     * Usado por el TrafficController para evitar procesar vehículos durante animaciones.
     * @return true si hay una animación de cruce en progreso
     */
    public boolean isCrossingAnimationInProgress() {
        return crossingAnimationInProgress;
    }
    
    /**
     * Marca el final de una animación de cruce.
     * Debe ser llamado al terminar cualquier animación de movimiento por la intersección.
     */
    private void onCrossingAnimationFinished(String vehicleId) {
        crossingAnimationInProgress = false;
        log("🏁 Animación de cruce completada para vehículo " + vehicleId + " - Intersección libre");
    }
    
    /**
     * Método helper para finalizar cualquier animación de vehículo
     * Centraliza la lógica común de finalización
     */
    private void finishVehicleAnimation(VehicleView vehicle, Vehicle logicalVehicle, 
                                       double startX, double startY, double finalX, double finalY,
                                       String movementDescription) {
        // Marcar como completado y limpiar
        logicalVehicle.setInIntersection(false);
        intersectionPane.getChildren().remove(vehicle);
        
        // Mostrar feedback del carril utilizado
        showLaneFeedback(startX, startY, finalX, finalY);
        
        // === MARCAR ANIMACIÓN COMO COMPLETADA ===
        onCrossingAnimationFinished(logicalVehicle.getId());
        
        log("✅ Vehículo " + logicalVehicle.getId() + " completó " + movementDescription);
    }
 }