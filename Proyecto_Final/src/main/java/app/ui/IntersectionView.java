package app.ui;

import java.util.ArrayList;
import java.util.List;

import app.controller.ScenarioController;
import app.controller.TickController;
import app.controller.TrafficController;
import app.controller.VehicleController;
import app.model.Intersection;
import app.model.Street;
import app.model.TrafficLight;
import app.model.enums.DirectionEnum;
import app.model.enums.VehicleTypeEnum;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Vista visual para mostrar el Escenario 1 con 8 calles (carriles de entrada y salida)
 */
public class IntersectionView extends Pane {

    private final ScenarioController scenarioController;
    private static final Color STREET_COLOR = Color.DARKGRAY;
    private static final Color INTERSECTION_COLOR = Color.LIGHTGRAY;
    private static final Color STREET_BORDER_COLOR = Color.WHITE;
    private static final Color LANE_DIVIDER_COLOR = Color.YELLOW;

    private final int scenarioNumber; // 1: intersección clásica, 2: autopista

    // Traffic light management for Scenario 2
    private TrafficController trafficController;
    private final List<TrafficLightView> trafficLightViews;
    
    // Vehicle management for Scenario 1
    private VehicleController vehicleController;
    private TickController tickController;

    public IntersectionView(int scenarioNumber) {
        this.scenarioNumber = scenarioNumber;
        this.scenarioController = new ScenarioController();
        this.trafficLightViews = new ArrayList<>();
        initializeScenario();
        drawScenario();

        // Initialize traffic control for Scenario 2
        if (scenarioNumber == 2) {
            initializeTrafficControl();
        }
        
        // Initialize vehicle control for both scenarios
        initializeVehicleControl();
    }

    /**
     * Inicializa el escenario segun opcion
     */
    private void initializeScenario() {
        if (scenarioNumber == 2) {
            scenarioController.initializeScenario2();
        } else {
            scenarioController.initializeScenario1();
        }
    }

    /**
     * Dibuja toda la escena del escenario
     */
    private void drawScenario() {
        // Limpiar la vista
        this.getChildren().clear();

        // Dibujar fondo
        drawBackground();

        if (scenarioNumber == 2) {
            drawScenario2Highway();
        } else {
            // Escenario 1
            drawAllStreets();
            drawCentralIntersection();
            drawStopSigns(); // Agregar señales de PARE
            addTitleScenario1();
            addVehicleControlPanel(); // Agregar controles de vehículos
            addBackButton();
        }
    }

    // ====== ESCENARIO 1 (existente) ======

    /**
     * Dibuja todas las calles del escenario con estructura realista
     */
    private void drawAllStreets() {
        List<Street> streets = scenarioController.getAllStreets();

        System.out.println("=== DIBUJANDO CALLES ===");
        System.out.println("Total calles a dibujar: " + streets.size());

        // Dibujar cada calle individualmente y verificar que todas se dibujen
        for (Street street : streets) {
            System.out.printf("Dibujando: %s en (%d,%d) tamaño %dx%d%n",
                street.getId(), street.getPosX(), street.getPosY(),
                street.getWidth(), street.getHeight());

            drawIndividualStreet(street);
            addStreetDirectionLabel(street);
        }

        // Dibujar líneas divisorias amarillas entre carriles
        drawCentralDividers();
    }

    /**
     * Extrae y normaliza la dirección desde el id de la calle.
     * Soporta tokens en español y en inglés, devolviendo: north, south, east o west
     */
    private String parseDirectionFromId(String id) {
        String[] tokens = id.split("_");
        for (String t : tokens) {
            String token = t.toLowerCase();
            switch (token) {
                case "north":
                case "norte":
                    return "north";
                case "south":
                case "sur":
                    return "south";
                case "east":
                case "este":
                    return "east";
                case "west":
                case "oeste":
                    return "west";
                default:
                    // continue
            }
        }
        return "";
    }

    /**
     * Dibuja una calle individual como rectángulo
     */
    private void drawIndividualStreet(Street street) {
        Rectangle streetRect = new Rectangle(
            street.getPosX(),
            street.getPosY(),
            street.getWidth(),
            street.getHeight()
        );

        streetRect.setFill(STREET_COLOR);
        streetRect.setStroke(STREET_BORDER_COLOR);
        streetRect.setStrokeWidth(1);

        this.getChildren().add(streetRect);
    }

    /**
     * Dibuja las líneas divisorias amarillas centrales entre carriles opuestos
     */
    private void drawCentralDividers() {
        int centerX = app.service.StreetService.INTERSECTION_CENTER_X;
        int centerY = app.service.StreetService.INTERSECTION_CENTER_Y;
        int intersectionSize = 80;
        int streetLength = 200;

        // Línea divisoria vertical Norte-Sur (entre carriles de entrada y salida)
        Rectangle verticalDivider = new Rectangle(
            centerX - 1, // Centro entre los carriles
            centerY - intersectionSize/2.0 - streetLength, // Desde arriba
            2, // Ancho de la línea
            streetLength * 2 + intersectionSize // Altura total
        );
        verticalDivider.setFill(LANE_DIVIDER_COLOR);
        this.getChildren().add(verticalDivider);

        // Línea divisoria horizontal Este-Oeste (entre carriles de entrada y salida)
        Rectangle horizontalDivider = new Rectangle(
            centerX - intersectionSize/2.0 - streetLength, // Desde la izquierda
            centerY - 1, // Centro entre los carriles
            streetLength * 2 + intersectionSize, // Ancho total
            2 // Alto de la línea
        );
        horizontalDivider.setFill(LANE_DIVIDER_COLOR);
        this.getChildren().add(horizontalDivider);
    }

    /**
     * Agrega etiquetas direccionales para cada calle
     */
    private void addStreetDirectionLabel(Street street) {
        String label = getStreetLabel(street);
        Color labelColor = getStreetLabelColor(street);

        Text streetText = new Text(label);
        streetText.setFill(labelColor);
        streetText.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        // Posicionar etiqueta según el tipo de calle
        positionStreetLabel(streetText, street);

        this.getChildren().add(streetText);
    }

    /**
     * Obtiene la etiqueta apropiada para cada calle según la convención
     */
    private String getStreetLabel(Street street) {
        String id = street.getId();
        String dir = parseDirectionFromId(id);
        boolean entrada = id.contains("entrada");
        boolean salida = id.contains("salida");

        if (entrada) {
            // Calles de entrada (azules) - según convención de posición
            switch (dir) {
                case "north": return "SOUTH";  // Lado superior: azul va hacia el sur
                case "south": return "NORTH";  // Lado inferior: azul va hacia el norte
                case "east":  return "WEST";   // Lado derecho: azul va hacia el oeste
                case "west":  return "EAST";   // Lado izquierdo: azul va hacia el este
                default: break;
            }
        } else if (salida) {
            // Calles de salida (rojas) - según convención de posición
            switch (dir) {
                case "north": return "NORTH";  // Lado superior: rojo va hacia el norte
                case "south": return "SOUTH";  // Lado inferior: rojo va hacia el sur
                case "east":  return "EAST";   // Lado derecho: rojo va hacia el este
                case "west":  return "WEST";   // Lado izquierdo: rojo va hacia el oeste
                default: break;
            }
        }
        return "STREET";
    }

    /**
     * Obtiene el color apropiado para cada etiqueta
     */
    private Color getStreetLabelColor(Street street) {
        return street.getId().contains("entrada") ? Color.LIGHTBLUE : Color.LIGHTCORAL;
    }

    /**
     * Posiciona las etiquetas según la orientación de la calle
     */
    private void positionStreetLabel(Text label, Street street) {
        double x = street.getPosX() + street.getWidth() / 2.0 - 20;
        double y = street.getPosY() + street.getHeight() / 2.0;

        String dir = parseDirectionFromId(street.getId());
        if ("north".equals(dir) || "south".equals(dir)) {
            y += 15; // Centrar verticalmente en calles verticales
        } else if ("east".equals(dir) || "west".equals(dir)) {
            x -= 10; // Ajustar horizontalmente en calles horizontales
            y += 5;
        }

        label.setX(x);
        label.setY(y);
    }

    /**
     * Dibuja la intersección central
     */
    private void drawCentralIntersection() {
        int centerX = app.service.StreetService.INTERSECTION_CENTER_X;
        int centerY = app.service.StreetService.INTERSECTION_CENTER_Y;
        int intersectionSize = 80;

        Rectangle intersection = new Rectangle(
            centerX - intersectionSize / 2.0,
            centerY - intersectionSize / 2.0,
            intersectionSize,
            intersectionSize
        );

        intersection.setFill(INTERSECTION_COLOR);
        intersection.setStroke(STREET_BORDER_COLOR);
        intersection.setStrokeWidth(3);

        this.getChildren().add(intersection);

        // Círculo central para marcar el centro de la intersección
        Circle centralCircle = new Circle(centerX, centerY, 12);
        centralCircle.setFill(Color.ORANGE);
        centralCircle.setStroke(Color.DARKORANGE);
        centralCircle.setStrokeWidth(2);

        this.getChildren().add(centralCircle);

        // Texto de intersección central
        Text intersectionText = new Text(centerX - 25, centerY + 5, "CRUCE");
        intersectionText.setFill(Color.DARKBLUE);
        intersectionText.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        this.getChildren().add(intersectionText);
    }

    /**
     * Dibuja las señales de PARE en las calles de entrada (azules) antes de la intersección
     */
    private void drawStopSigns() {
        int centerX = app.service.StreetService.INTERSECTION_CENTER_X;
        int centerY = app.service.StreetService.INTERSECTION_CENTER_Y;
        int intersectionSize = 80;
        int stopSignSize = 20; // Tamaño de las señales de PARE
        int distanceFromIntersection = 35; // Distancia desde el borde de la intersección
        
        // Señal PARE - Entrada NORTE (calle azul que viene del norte hacia la intersección)
    // Ajuste: la entrada norte está en el carril izquierdo (centerX - 20)
    double northStopX = centerX - 20; // Centro real del carril de entrada norte
        double northStopY = centerY - intersectionSize/2.0 - distanceFromIntersection;
        StopSignView northStop = StopSignView.createForDirection(northStopX, northStopY, stopSignSize, "north");
        this.getChildren().add(northStop);
        
        // Señal PARE - Entrada SUR (calle azul que viene del sur hacia la intersección)
    // Ajuste: la entrada sur está en el carril derecho (centerX + 20)
    double southStopX = centerX + 20; // Centro real del carril de entrada sur
        double southStopY = centerY + intersectionSize/2.0 + distanceFromIntersection;
        StopSignView southStop = StopSignView.createForDirection(southStopX, southStopY, stopSignSize, "south");
        this.getChildren().add(southStop);
        
        // Señal PARE - Entrada ESTE (calle azul que viene del este hacia la intersección)
        double eastStopX = centerX - intersectionSize/2.0 - distanceFromIntersection;
        double eastStopY = centerY + 20; // Centro del carril de entrada este
        StopSignView eastStop = StopSignView.createForDirection(eastStopX, eastStopY, stopSignSize, "east");
        this.getChildren().add(eastStop);
        
        // Señal PARE - Entrada OESTE (calle azul que viene del oeste hacia la intersección)
        double westStopX = centerX + intersectionSize/2.0 + distanceFromIntersection;
        double westStopY = centerY - 20; // Centro del carril de entrada oeste
        StopSignView westStop = StopSignView.createForDirection(westStopX, westStopY, stopSignSize, "west");
        this.getChildren().add(westStop);
        
        System.out.println("Señales de PARE agregadas en las 4 entradas de la intersección");
    }
    
    /**
     * Inicializa el sistema de control de vehículos para el Escenario 1
     */
    private void initializeVehicleControl() {
        this.tickController = new TickController();
        this.vehicleController = new VehicleController(this, scenarioController, tickController);
        System.out.println("Sistema de control de vehículos y ticks inicializado");
    }
    
    /**
     * Agrega el panel de control de vehículos
     */
    private void addVehicleControlPanel() {
        int panelX = LaunchView.WIDTH - 250;
        int panelY = 100;
        int panelWidth = 250;
        int panelHeight = 300;
        
        // Panel de fondo
        Rectangle controlPanel = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        controlPanel.setArcWidth(8);
        controlPanel.setArcHeight(8);
        controlPanel.setFill(Color.color(0, 0, 0, 0.4));
        controlPanel.setStroke(Color.LIGHTBLUE);
        controlPanel.setStrokeWidth(1.5);
        this.getChildren().add(controlPanel);
        
        // Título del panel
        Text controlTitle = new Text(panelX + 10, panelY + 20, "Control de Vehículos");
        controlTitle.setFill(Color.LIGHTBLUE);
        controlTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        this.getChildren().add(controlTitle);
        
        // Botón Play/Pause simulación
        javafx.scene.control.Button playPauseButton = new javafx.scene.control.Button("Iniciar");
        playPauseButton.setLayoutX(panelX + 10);
        playPauseButton.setLayoutY(panelY + 30);
        playPauseButton.setPrefWidth(100);
        playPauseButton.setPrefHeight(30);
        playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-background-radius: 5;");
        
        playPauseButton.setOnAction(e -> {
            if (tickController.isRunning()) {
                tickController.pause();
                playPauseButton.setText("Reanudar");
                playPauseButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            } else if (tickController.isPaused()) {
                tickController.resume();
                playPauseButton.setText("Pausar");
                playPauseButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            } else {
                tickController.start();
                playPauseButton.setText("Pausar");
                playPauseButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            }
        });
        this.getChildren().add(playPauseButton);
        
        // Botón Auto-spawn
        javafx.scene.control.Button autoSpawnButton = new javafx.scene.control.Button("Auto OFF");
        autoSpawnButton.setLayoutX(panelX + 120);
        autoSpawnButton.setLayoutY(panelY + 30);
        autoSpawnButton.setPrefWidth(100);
        autoSpawnButton.setPrefHeight(30);
        autoSpawnButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; " +
                               "-fx-font-size: 12px; -fx-background-radius: 5;");
        
        autoSpawnButton.setOnAction(e -> {
            boolean newState = !vehicleController.isAutoSpawnEnabled();
            vehicleController.setAutoSpawn(newState);
            
            if (newState) {
                autoSpawnButton.setText("Auto ON");
                autoSpawnButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            } else {
                autoSpawnButton.setText("Auto OFF");
                autoSpawnButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            }
        });
        this.getChildren().add(autoSpawnButton);
        
        // Sección de creación manual
        Text manualTitle = new Text(panelX + 10, panelY + 80, "Crear Vehículo Manual:");
        manualTitle.setFill(Color.WHITE);
        manualTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().add(manualTitle);
        
        // Etiqueta "Desde:"
        Text fromLabel = new Text(panelX + 10, panelY + 105, "Desde:");
        fromLabel.setFill(Color.LIGHTGRAY);
        fromLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.getChildren().add(fromLabel);
        
        // Selector de entrada
        javafx.scene.control.ComboBox<String> entrySelector = new javafx.scene.control.ComboBox<>();
        entrySelector.getItems().addAll("Norte", "Sur", "Este", "Oeste");
        entrySelector.setValue("Sur");
        entrySelector.setLayoutX(panelX + 10);
        entrySelector.setLayoutY(panelY + 110);
        entrySelector.setPrefWidth(100);
        this.getChildren().add(entrySelector);
        
        // Etiqueta "Hacia:"
        Text toLabel = new Text(panelX + 120, panelY + 105, "Hacia:");
        toLabel.setFill(Color.LIGHTGRAY);
        toLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.getChildren().add(toLabel);
        
        // Selector de dirección
        javafx.scene.control.ComboBox<String> directionSelector = new javafx.scene.control.ComboBox<>();
        directionSelector.getItems().addAll("Recto", "Izquierda", "Derecha", "U-Turn");
        directionSelector.setValue("Recto");
        directionSelector.setLayoutX(panelX + 120);
        directionSelector.setLayoutY(panelY + 110);
        directionSelector.setPrefWidth(100);
        this.getChildren().add(directionSelector);
        
        // Texto explicativo de la ruta
        Text routeExplanation = new Text(panelX + 10, panelY + 135, "");
        routeExplanation.setFill(Color.YELLOW);
        routeExplanation.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
        this.getChildren().add(routeExplanation);
        
        // Actualizar explicación cuando cambian los selectores
        Runnable updateExplanation = () -> {
            String from = entrySelector.getValue();
            String direction = directionSelector.getValue();
            String to = calculateDestination(from, direction);
            routeExplanation.setText("Ruta: " + from + " -> " + to);
        };
        
        entrySelector.setOnAction(e -> updateExplanation.run());
        directionSelector.setOnAction(e -> updateExplanation.run());
        
        // Inicializar explicación
        updateExplanation.run();
        
        // Botones de tipo de vehículo
        javafx.scene.control.Button normalButton = new javafx.scene.control.Button("Auto Normal");
        normalButton.setLayoutX(panelX + 10);
        normalButton.setLayoutY(panelY + 145);
        normalButton.setPrefWidth(100);
        normalButton.setPrefHeight(25);
        normalButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                             "-fx-font-size: 11px; -fx-background-radius: 5;");
        
        normalButton.setOnAction(e -> {
            createManualVehicle(entrySelector.getValue(), directionSelector.getValue(), VehicleTypeEnum.NORMAL);
        });
        this.getChildren().add(normalButton);
        
        javafx.scene.control.Button emergencyButton = new javafx.scene.control.Button("Ambulancia");
        emergencyButton.setLayoutX(panelX + 120);
        emergencyButton.setLayoutY(panelY + 145);
        emergencyButton.setPrefWidth(100);
        emergencyButton.setPrefHeight(25);
        emergencyButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-background-radius: 5;");
        
        emergencyButton.setOnAction(e -> {
            createManualVehicle(entrySelector.getValue(), directionSelector.getValue(), VehicleTypeEnum.EMERGENCY);
        });
        this.getChildren().add(emergencyButton);
        
        // Control de velocidad de ticks con slider mejorado
        Text speedTitle = new Text(panelX + 10, panelY + 190, "Velocidad: 50 ticks/seg");
        speedTitle.setFill(Color.WHITE);
        speedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().add(speedTitle);
        
        javafx.scene.control.Slider speedSlider = new javafx.scene.control.Slider();
        speedSlider.setMin(5);    // Mínimo: 5 ticks/seg
        speedSlider.setMax(100);  // Máximo: 100 ticks/seg
        speedSlider.setValue(50); // Valor por defecto: mitad del rango
        speedSlider.setMajorTickUnit(25);
        speedSlider.setMinorTickCount(4);
        speedSlider.setSnapToTicks(false);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setLayoutX(panelX + 10);
        speedSlider.setLayoutY(panelY + 205);
        speedSlider.setPrefWidth(220);
        speedSlider.setPrefHeight(40);
        
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (tickController != null) {
                int ticksPerSecond = newVal.intValue();
                // Convertir ticks por segundo a intervalo en milisegundos
                int intervalMs = Math.max(1, 1000 / ticksPerSecond);
                tickController.setTickInterval(intervalMs);
                speedTitle.setText("Velocidad: " + ticksPerSecond + " ticks/seg");
            }
        });
        this.getChildren().add(speedSlider);
        
        // Información de estado (más espaciada)
        Text statusInfo = new Text(panelX + 10, panelY + 260, "Vehiculos: 0 | Tick: 0");
        statusInfo.setFill(Color.LIGHTGRAY);
        statusInfo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        this.getChildren().add(statusInfo);
        
        // Actualizar información cada segundo
        javafx.animation.Timeline statusUpdater = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                if (vehicleController != null && tickController != null) {
                    String status = String.format("Vehiculos: %d | Tick: %d", 
                        vehicleController.getActiveVehicleCount(), 
                        tickController.getTickCount());
                    statusInfo.setText(status);
                }
            })
        );
        statusUpdater.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        statusUpdater.play();
    }
    
    /**
     * Crea un vehículo manual según los parámetros seleccionados
     */
    private void createManualVehicle(String entry, String direction, VehicleTypeEnum type) {
        // Convertir entrada a ID de calle
        String streetId = switch (entry) {
            case "Norte" -> "calle_north_entrada";
            case "Sur" -> "calle_south_entrada";
            case "Este" -> "calle_east_entrada";
            case "Oeste" -> "calle_west_entrada";
            default -> "calle_north_entrada";
        };
        
        // Convertir dirección
        DirectionEnum dir = switch (direction) {
            case "Recto" -> DirectionEnum.STRAIGHT;
            case "Izquierda" -> DirectionEnum.LEFT;
            case "Derecha" -> DirectionEnum.RIGHT;
            case "U-Turn" -> DirectionEnum.U_TURN;
            default -> DirectionEnum.STRAIGHT;
        };
        
        // Buscar la calle
        Street entryStreet = scenarioController.getAllStreets().stream()
            .filter(street -> street.getId().equals(streetId))
            .findFirst()
            .orElse(null);
        
        if (entryStreet != null && vehicleController != null) {
            vehicleController.spawnVehicle(entryStreet, type, dir);
        }
    }
    
    /**
     * Calcula el destino basado en el origen y la dirección
     */
    private String calculateDestination(String from, String direction) {
        return switch (from) {
            case "Norte" -> switch (direction) {
                case "Recto" -> "Sur";
                case "Izquierda" -> "Oeste";
                case "Derecha" -> "Este";
                case "U-Turn" -> "Norte";
                default -> "?";
            };
            case "Sur" -> switch (direction) {
                case "Recto" -> "Norte";
                case "Izquierda" -> "Este";
                case "Derecha" -> "Oeste";
                case "U-Turn" -> "Sur";
                default -> "?";
            };
            case "Este" -> switch (direction) {
                case "Recto" -> "Oeste";
                case "Izquierda" -> "Norte";
                case "Derecha" -> "Sur";
                case "U-Turn" -> "Este";
                default -> "?";
            };
            case "Oeste" -> switch (direction) {
                case "Recto" -> "Este";
                case "Izquierda" -> "Sur";
                case "Derecha" -> "Norte";
                case "U-Turn" -> "Oeste";
                default -> "?";
            };
            default -> "?";
        };
    }

    private void addTitleScenario1() {
        Text title = new Text(50, 50, "Escenario 1: Intersección de 4 Vías - 8 Carriles");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        this.getChildren().add(title);

        // Información detallada
        Text info = new Text(50, 80, String.format(
            "Calles: %d | Intersecciones: %d | Carriles de Entrada: 4 | Carriles de Salida: 4",
            scenarioController.getStreetCount(),
            scenarioController.getIntersectionCount()
        ));
        info.setFill(Color.LIGHTGRAY);
        info.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        this.getChildren().add(info);

        // Leyenda de colores
        addColorLegend();
    }

    /**
     * Dibuja el fondo de la simulación
     */
    private void drawBackground() {
        Rectangle background = new Rectangle(0, 0, LaunchView.WIDTH, LaunchView.HEIGHT);
        background.setFill(Color.DARKGREEN);
        this.getChildren().add(background);
    }

    /**
     * Agrega una leyenda de colores para explicar la visualización
     */
    private void addColorLegend() {
        int startY = 120;

        // Entrada
        Rectangle entryRect = new Rectangle(50, startY, 15, 15);
        entryRect.setFill(Color.LIGHTBLUE);
        this.getChildren().add(entryRect);

        Text entryText = new Text(75, startY + 12, "Carriles de Entrada (Spawn)");
        entryText.setFill(Color.WHITE);
        entryText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        this.getChildren().add(entryText);

        // Salida
        Rectangle exitRect = new Rectangle(250, startY, 15, 15);
        exitRect.setFill(Color.LIGHTCORAL);
        this.getChildren().add(exitRect);

        Text exitText = new Text(275, startY + 12, "Carriles de Salida");
        exitText.setFill(Color.WHITE);
        exitText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        this.getChildren().add(exitText);

        // Líneas divisorias
        Rectangle dividerRect = new Rectangle(450, startY, 15, 15);
        dividerRect.setFill(LANE_DIVIDER_COLOR);
        this.getChildren().add(dividerRect);

        Text dividerText = new Text(475, startY + 12, "Líneas Divisorias");
        dividerText.setFill(Color.WHITE);
        dividerText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        this.getChildren().add(dividerText);
    }

    /**
     * Agrega un botón para regresar al menú principal
     */
    private void addBackButton() {
        javafx.scene.control.Button backButton = new javafx.scene.control.Button("<- Volver al Menu");
        backButton.setLayoutX(50);
        backButton.setLayoutY(LaunchView.HEIGHT - 80);
        backButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                           "-fx-font-size: 16px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;");

        backButton.setOnAction(e -> {
            // Clean up controllers before switching scenes
            if (trafficController != null) {
                trafficController.stopControl();
            }
            if (vehicleController != null) {
                vehicleController.cleanup();
            }
            if (tickController != null) {
                tickController.stop();
            }
            // Obtener el Stage actual desde la escena
            javafx.stage.Stage stage = (javafx.stage.Stage) this.getScene().getWindow();
            LaunchView launchView = new LaunchView();
            stage.setScene(launchView.createLaunchScene(stage));
        });

        this.getChildren().add(backButton);
    }

    // ====== ESCENARIO 2 (Autopista) ======

    private void drawScenario2Highway() {
        // 1) Calles segmentadas
        List<Street> streets = scenarioController.getAllStreets();
        for (Street street : streets) {
            drawIndividualStreet(street);
            drawLaneArrows(street);
            addHighwayLaneLabel(street);
        }

        // 2) Divisorias entre carriles (bandas superior e inferior)
        drawScenario2LaneDividers();

        // 3) Intersecciones como rectángulos verticales
        drawScenario2Intersections();

        // 4) Traffic lights - Properly positioned and integrated
        drawScenario2TrafficLights();

        // 5) Título, reglas y controles
        addTitleScenario2();
        addHighwayRulesPanel();
        addTrafficControlPanel();
        addVehicleControlPanelScenario2(); // NUEVO: Panel de control de vehículos para Escenario 2
        addBackButton();
    }

    /**
     * Initializes traffic light control system for Scenario 2
     */
    private void initializeTrafficControl() {
        List<Intersection> intersections = scenarioController.getAllIntersections();
        List<TrafficLight> trafficLights = scenarioController.getAllTrafficLights();

        if (!trafficLights.isEmpty()) {
            this.trafficController = new TrafficController(intersections, trafficLights);

            // Position traffic lights properly
            positionTrafficLights();

            // Create visual representations
            createTrafficLightViews();

            // Start the traffic control system
            trafficController.startControl();

            System.out.println("Traffic control initialized with " + trafficLights.size() + " traffic lights");
        }
    }

    /**
     * Positions traffic lights at appropriate locations relative to intersections
     */
    private void positionTrafficLights() {
        List<TrafficLight> trafficLights = scenarioController.getAllTrafficLights();

        for (TrafficLight light : trafficLights) {
            String lightId = light.getId();

            // Find corresponding intersection
            Intersection targetIntersection = null;
            if (lightId.contains("intersection_1")) {
                targetIntersection = findIntersectionById("intersection_1");
            } else if (lightId.contains("intersection_2")) {
                targetIntersection = findIntersectionById("intersection_2");
            } else if (lightId.contains("intersection_3")) {
                targetIntersection = findIntersectionById("intersection_3");
            } else if (lightId.contains("intersection_4")) {
                targetIntersection = findIntersectionById("intersection_4");
            }

            if (targetIntersection != null) {
                // Position lights relative to intersection
                double intersectionX = targetIntersection.getPosX() + targetIntersection.getWidth() / 2.0;
                double intersectionY = targetIntersection.getPosY();

                if (lightId.contains("_we")) {
                    // West->East traffic light (RIGHT side - closer to end of street)
                    light.setPosition(intersectionX + 15, intersectionY + 20);
                } else if (lightId.contains("_ew")) {
                    // East->West traffic light (LEFT side - closer to end of street)
                    light.setPosition(intersectionX - 35, intersectionY + targetIntersection.getHeight() - 80);
                }
            }
        }
    }

    /**
     * Creates visual representations for all traffic lights
     */
    private void createTrafficLightViews() {
        List<TrafficLight> trafficLights = scenarioController.getAllTrafficLights();

        for (TrafficLight light : trafficLights) {
            // Create appropriately sized traffic light for highway scenario
            TrafficLightView lightView = new TrafficLightView(light, 16, 48, 6);

            // Register with controller for updates
            trafficController.registerTrafficLightView(lightView);

            // Add to scene
            this.getChildren().add(lightView);
            trafficLightViews.add(lightView);
        }

        System.out.println("Created " + trafficLightViews.size() + " traffic light views");
    }

    /**
     * Helper method to find intersection by ID
     */
    private Intersection findIntersectionById(String id) {
        return scenarioController.getAllIntersections().stream()
                .filter(intersection -> intersection.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void drawScenario2Intersections() {
        for (Intersection inter : scenarioController.getAllIntersections()) {
            Rectangle r = new Rectangle(inter.getPosX(), inter.getPosY(), inter.getWidth(), inter.getHeight());
            r.setFill(INTERSECTION_COLOR);
            r.setStroke(STREET_BORDER_COLOR);
            r.setStrokeWidth(2);
            this.getChildren().add(r);

            Text t = new Text(inter.getPosX() + 2, inter.getPosY() + 14, inter.getId());
            t.setFill(Color.DARKBLUE);
            t.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            this.getChildren().add(t);
        }
    }

    private void drawScenario2LaneDividers() {
        // Use the same layout constants as in ScenarioController - UPDATED to new dimensions
        final int sceneWidth = 1600;  // Actualizado de 1280 a 1600
        final int sceneHeight = 900;  // Actualizado de 720 a 900
        final int centerX = sceneWidth / 2;
        final int centerY = sceneHeight / 2;

        // Street dimensions matching Scenario 1
        final int laneHeight = 40; // Same as Scenario 1
        final int laneGap = 8; // Slightly larger gap for better visibility
        final int bandGap = 10; // Gap between top and bottom direction bands
        final int intersectionSize = 80; // Same as Scenario 1

        // Calculate total highway height and center it vertically
        int totalHighwayHeight = (laneHeight * 3) + (laneGap * 2) + bandGap + (laneHeight * 3) + (laneGap * 2);

        // East->West lanes (top band)
        int ewLeftY = centerY - (totalHighwayHeight / 2);
        int ewCenterY = ewLeftY + laneHeight + laneGap;
        int ewRightY = ewCenterY + laneHeight + laneGap;

        // West->East lanes (bottom band)
        int weLeftY = ewRightY + laneHeight + bandGap;
        int weCenterY = weLeftY + laneHeight + laneGap;

        // Center intersections horizontally with proper spacing - EXTENDED for wider screen
        int totalRoadLength = 1200; // Aumentado de 1000 a 1200 para aprovechar la pantalla más ancha
        int leftMargin = centerX - (totalRoadLength / 2);
        int intersectionSpacing = totalRoadLength / 3;

        int[] intersectionXs = new int[] {
            leftMargin,
            leftMargin + intersectionSpacing,
            leftMargin + (intersectionSpacing * 2),
            leftMargin + totalRoadLength
        };

        // Central divider line between bands - EXTENDED
        int centralDividerY = ewRightY + laneHeight + (bandGap / 2);
        Rectangle centralDivider = new Rectangle(leftMargin, centralDividerY - 1, totalRoadLength, 3);
        centralDivider.setFill(Color.WHITE);
        this.getChildren().add(centralDivider);

        // Draw lane dividers where there are actual street segments
        // Between intersection_1 and intersection_2
        int segmentStartX = intersectionXs[0] + intersectionSize / 2 + 6;
        int segmentEndX = intersectionXs[1] - intersectionSize / 2 - 6;
        drawLaneDividerSegment(segmentStartX, segmentEndX, ewLeftY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, ewCenterY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, weLeftY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, weCenterY + laneHeight);

        // Between intersection_2 and intersection_3
        segmentStartX = intersectionXs[1] + intersectionSize / 2 + 6;
        segmentEndX = intersectionXs[2] - intersectionSize / 2 - 6;
        drawLaneDividerSegment(segmentStartX, segmentEndX, ewLeftY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, ewCenterY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, weLeftY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, weCenterY + laneHeight);

        // Between intersection_3 and intersection_4
        segmentStartX = intersectionXs[2] + intersectionSize / 2 + 6;
        segmentEndX = intersectionXs[3] - intersectionSize / 2 - 6;
        drawLaneDividerSegment(segmentStartX, segmentEndX, ewLeftY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, ewCenterY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, weLeftY + laneHeight);
        drawLaneDividerSegment(segmentStartX, segmentEndX, weCenterY + laneHeight);
    }

    private void drawLaneDividerSegment(int startX, int endX, int y) {
        if (endX > startX) {
            Rectangle divider = new Rectangle(startX, y, endX - startX, 2);
            divider.setFill(LANE_DIVIDER_COLOR);
            this.getChildren().add(divider);
        }
    }

    private void drawLaneArrows(Street street) {
        String id = street.getId().toLowerCase();
        String arrow = "";
        double rotation = 0;

        // Determine arrow based on lane type
        if (id.contains("left")) {
            arrow = "↰";
        } else if (id.contains("center")) {
            arrow = "↑";
        } else if (id.contains("right")) {
            arrow = "↑↱";
        }

        if(id.contains("south") || id.contains("north")) {
            arrow = "↑";
        }

        // Determine rotation based on traffic direction
        if (id.startsWith("west_")) {
            rotation = 270; // Point left
        } else if (id.startsWith("east_")) {
            rotation = 90; // Point right
        } else if (id.startsWith("north_")) {
            rotation = 0; // Point up
        } else if (id.startsWith("south_")) {
            rotation = 180; // Point down
        }

        if (arrow.isEmpty()) return;

        int startX = street.getPosX() + 12;
        int endX = street.getPosX() + street.getWidth() - 12;
        int centerY = street.getPosY() + street.getHeight() / 2 + 4;

        for (int x = startX; x < endX; x += 80) {
            Text a = new Text(x, centerY, arrow);
            a.setFill(Color.YELLOW);
            a.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            a.setRotate(rotation);
            this.getChildren().add(a);
        }
    }

    private void addHighwayLaneLabel(Street street) {
        String id = street.getId().toLowerCase();
        String label = null;

        if (id.contains("north_salida")) {
            label = "N Exit";
        } else if (id.contains("south_salida")) {
            label = "S Exit";
        } else {
            if (id.startsWith("west_left_lane")) label = "West (L)";
            else if (id.startsWith("west_center_lane")) label = "West (C)";
            else if (id.startsWith("west_right_lane")) label = "West (R)";
            else if (id.startsWith("east_left_lane")) label = "East (L)";
            else if (id.startsWith("east_center_lane")) label = "East (C)";
            else if (id.startsWith("east_right_lane")) label = "East (R)";
        }

        if (label != null) {
            if (id.contains("segment")) {
                char segmentNum = '?';
                if (id.contains("segment1")) segmentNum = '1';
                else if (id.contains("segment2")) segmentNum = '2';
                else if (id.contains("segment3")) segmentNum = '3';

                if (segmentNum != '?') {
                    label = label + " " + segmentNum;
                }
            }

            Text t = new Text(street.getPosX() + 6, street.getPosY() + 14, label);
            t.setFill(Color.WHITE);
            t.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            this.getChildren().add(t);
        }
    }

    private void drawScenario2TrafficLights() {
        // Add small labels to identify traffic light positions
        List<TrafficLight> trafficLights = scenarioController.getAllTrafficLights();
        for (TrafficLight light : trafficLights) {
            Text label = new Text(light.getPosX() - 10, light.getPosY() - 8, "TL");
            label.setFill(Color.WHITE);
            label.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            this.getChildren().add(label);
        }
    }

    private void addTitleScenario2() {
        Text title = new Text(50, 30, "Escenario 2: Autopista con Semáforos Inteligentes");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        this.getChildren().add(title);

        Text info = new Text(50, 60, String.format(
            "Calles: %d | Intersecciones: %d | Semáforos: %d",
            scenarioController.getStreetCount(),
            scenarioController.getIntersectionCount(),
            scenarioController.getAllTrafficLights().size()
        ));
        info.setFill(Color.LIGHTGRAY);
        info.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        this.getChildren().add(info);
    }

    private void addHighwayRulesPanel() {
        int panelX = 50;
        int panelY = 100; // Moved up from 160 to 100

        Rectangle rulesPanel = new Rectangle(panelX, panelY, 320, 80);
        rulesPanel.setArcWidth(8);
        rulesPanel.setArcHeight(8);
        rulesPanel.setFill(Color.color(0, 0, 0, 0.3));
        rulesPanel.setStroke(Color.LIGHTGRAY);
        rulesPanel.setStrokeWidth(1);
        this.getChildren().add(rulesPanel);

        Text rulesTitle = new Text(panelX + 10, panelY + 18, "Reglas de la Autopista:");
        rulesTitle.setFill(Color.WHITE);
        rulesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().add(rulesTitle);

        String[] rules = {
            "• Carril Izquierdo: Solo giros a la izquierda y U-turns",
            "• Carril Central: Solo movimientos rectos",
            "• Carril Derecho: Recto y giros a la derecha"
        };

        for (int i = 0; i < rules.length; i++) {
            Text rule = new Text(panelX + 10, panelY + 38 + (i * 15), rules[i]);
            rule.setFill(Color.LIGHTGRAY);
            rule.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
            this.getChildren().add(rule);
        }
    }

    /**
     * Adds traffic light control panel with pause/resume functionality only
     */
    private void addTrafficControlPanel() {
        int panelX = LaunchView.WIDTH - 250; // Ajustado para panel más pequeño
        int panelY = 50; // Posición alta

        // Control panel background - smaller size for just pause button
        Rectangle controlPanel = new Rectangle(panelX, panelY, 220, 80);
        controlPanel.setArcWidth(8);
        controlPanel.setArcHeight(8);
        controlPanel.setFill(Color.color(0, 0, 0, 0.4));
        controlPanel.setStroke(Color.LIGHTBLUE);
        controlPanel.setStrokeWidth(1.5);
        this.getChildren().add(controlPanel);

        // Title
        Text controlTitle = new Text(panelX + 10, panelY + 18, "Control de Semáforos");
        controlTitle.setFill(Color.LIGHTBLUE);
        controlTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().add(controlTitle);

        // Pause/Resume button - centered in panel
        javafx.scene.control.Button toggleButton = new javafx.scene.control.Button("Pausar");
        toggleButton.setLayoutX(panelX + 50);
        toggleButton.setLayoutY(panelY + 30);
        toggleButton.setPrefWidth(120);
        toggleButton.setPrefHeight(35);
        toggleButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; " +
                             "-fx-font-size: 12px; -fx-padding: 5 15 5 15; " +
                             "-fx-background-radius: 5;");
        toggleButton.setOnAction(e -> {
            if (trafficController != null) {
                if (trafficController.isRunning()) {
                    trafficController.stopControl();
                    toggleButton.setText("Reanudar");
                    toggleButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                         "-fx-font-size: 12px; -fx-padding: 5 15 5 15; " +
                                         "-fx-background-radius: 5;");
                } else {
                    trafficController.startControl();
                    toggleButton.setText("Pausar");
                    toggleButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; " +
                                         "-fx-font-size: 12px; -fx-padding: 5 15 5 15; " +
                                         "-fx-background-radius: 5;");
                }
            }
        });
        this.getChildren().add(toggleButton);
    }

    /**
     * Adds vehicle control panel for Scenario 2 (Highway)
     */
    private void addVehicleControlPanelScenario2() {
        // Calcular la posición de la carretera para evitar superposición
        final int sceneWidth = 1600;
        final int sceneHeight = 900;
        final int centerY = sceneHeight / 2;
        final int laneHeight = 40;
        final int laneGap = 8;
        final int bandGap = 10;
        
        // Calcular la altura total de la autopista
        int totalHighwayHeight = (laneHeight * 3) + (laneGap * 2) + bandGap + (laneHeight * 3) + (laneGap * 2);
        int highwayTopY = centerY - (totalHighwayHeight / 2);
        int highwayBottomY = centerY + (totalHighwayHeight / 2);
        
        // Posicionar el panel debajo de la carretera con margen de seguridad
        int panelX = LaunchView.WIDTH - 320;
        int panelY = highwayBottomY + 30; // 30 píxeles debajo de la carretera
        int panelWidth = 300;
        int panelHeight = Math.min(400, sceneHeight - panelY - 20); // Ajustar altura si es necesario
        
        // Panel de fondo
        Rectangle controlPanel = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        controlPanel.setArcWidth(8);
        controlPanel.setArcHeight(8);
        controlPanel.setFill(Color.color(0.2, 0.2, 0.2, 0.8));
        controlPanel.setStroke(Color.ORANGE);
        controlPanel.setStrokeWidth(2);
        this.getChildren().add(controlPanel);
        
        // Título del panel
        Text panelTitle = new Text(panelX + 10, panelY + 25, "Control de Vehículos - Autopista");
        panelTitle.setFill(Color.ORANGE);
        panelTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        this.getChildren().add(panelTitle);
        
        // === SECCIÓN DE CREACIÓN MANUAL ===
        Text manualTitle = new Text(panelX + 10, panelY + 50, "Crear Vehículo Manual:");
        manualTitle.setFill(Color.WHITE);
        manualTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().add(manualTitle);
        
        // Etiqueta "Desde:"
        Text fromLabel = new Text(panelX + 10, panelY + 75, "Desde:");
        fromLabel.setFill(Color.LIGHTGRAY);
        fromLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.getChildren().add(fromLabel);
        
        // Selector de entrada (West o East)
        javafx.scene.control.ComboBox<String> entrySelector = new javafx.scene.control.ComboBox<>();
        entrySelector.getItems().addAll("West", "East");
        entrySelector.setValue("West");
        entrySelector.setLayoutX(panelX + 10);
        entrySelector.setLayoutY(panelY + 80);
        entrySelector.setPrefWidth(120);
        this.getChildren().add(entrySelector);
        
        // Etiqueta "Hacia:"
        Text toLabel = new Text(panelX + 140, panelY + 75, "Hacia:");
        toLabel.setFill(Color.LIGHTGRAY);
        toLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.getChildren().add(toLabel);
        
        // Selector de destino
        javafx.scene.control.ComboBox<String> destinationSelector = new javafx.scene.control.ComboBox<>();
        destinationSelector.getItems().addAll("Recto", "South1", "South2", "South3", "North1", "North2", "North3");
        destinationSelector.setValue("Recto");
        destinationSelector.setLayoutX(panelX + 140);
        destinationSelector.setLayoutY(panelY + 80);
        destinationSelector.setPrefWidth(120);
        this.getChildren().add(destinationSelector);
        
        // Texto explicativo de la ruta
        Text routeExplanation = new Text(panelX + 10, panelY + 105, "");
        routeExplanation.setFill(Color.YELLOW);
        routeExplanation.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
        this.getChildren().add(routeExplanation);
        
        // Actualizar explicación cuando cambian los selectores
        Runnable updateExplanation = () -> {
            String from = entrySelector.getValue();
            String destination = destinationSelector.getValue();
            String explanation = getRouteExplanationScenario2(from, destination);
            routeExplanation.setText(explanation);
        };
        
        entrySelector.setOnAction(e -> updateExplanation.run());
        destinationSelector.setOnAction(e -> updateExplanation.run());
        
        // Inicializar explicación
        updateExplanation.run();
        
        // Botones de tipo de vehículo
        javafx.scene.control.Button normalButton = new javafx.scene.control.Button("Auto Normal");
        normalButton.setLayoutX(panelX + 10);
        normalButton.setLayoutY(panelY + 115);
        normalButton.setPrefWidth(120);
        normalButton.setPrefHeight(25);
        normalButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                             "-fx-font-size: 11px; -fx-background-radius: 5;");
        
        normalButton.setOnAction(e -> {
            createManualVehicleScenario2(entrySelector.getValue(), destinationSelector.getValue(), VehicleTypeEnum.NORMAL);
        });
        this.getChildren().add(normalButton);
        
        javafx.scene.control.Button emergencyButton = new javafx.scene.control.Button("Ambulancia");
        emergencyButton.setLayoutX(panelX + 140);
        emergencyButton.setLayoutY(panelY + 115);
        emergencyButton.setPrefWidth(120);
        emergencyButton.setPrefHeight(25);
        emergencyButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-background-radius: 5;");
        
        emergencyButton.setOnAction(e -> {
            createManualVehicleScenario2(entrySelector.getValue(), destinationSelector.getValue(), VehicleTypeEnum.EMERGENCY);
        });
        this.getChildren().add(emergencyButton);
        
        // === SECCIÓN DE CONTROL DE SIMULACIÓN ===
        int buttonY = panelY + 160;
        int buttonSpacing = 35;
        
        // Botón Play/Pause simulación
        javafx.scene.control.Button playPauseButton = new javafx.scene.control.Button("Iniciar");
        playPauseButton.setLayoutX(panelX + 10);
        playPauseButton.setLayoutY(buttonY);
        playPauseButton.setPrefWidth(100);
        playPauseButton.setPrefHeight(30);
        playPauseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-background-radius: 5;");
        
        playPauseButton.setOnAction(e -> {
            if (tickController.isRunning()) {
                tickController.pause();
                playPauseButton.setText("Reanudar");
                playPauseButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            } else if (tickController.isPaused()) {
                tickController.resume();
                playPauseButton.setText("Pausar");
                playPauseButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            } else {
                tickController.start();
                playPauseButton.setText("Pausar");
                playPauseButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            }
        });
        this.getChildren().add(playPauseButton);
        
        // Botón Auto-spawn
        javafx.scene.control.Button autoSpawnButton = new javafx.scene.control.Button("Auto OFF");
        autoSpawnButton.setLayoutX(panelX + 120);
        autoSpawnButton.setLayoutY(buttonY);
        autoSpawnButton.setPrefWidth(100);
        autoSpawnButton.setPrefHeight(30);
        autoSpawnButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; " +
                               "-fx-font-size: 12px; -fx-background-radius: 5;");
        
        autoSpawnButton.setOnAction(e -> {
            boolean newState = !vehicleController.isAutoSpawnEnabled();
            vehicleController.setAutoSpawn(newState);
            
            if (newState) {
                autoSpawnButton.setText("Auto ON");
                autoSpawnButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            } else {
                autoSpawnButton.setText("Auto OFF");
                autoSpawnButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; " +
                                        "-fx-font-size: 12px; -fx-background-radius: 5;");
            }
        });
        this.getChildren().add(autoSpawnButton);
        
        buttonY += buttonSpacing;
        
        // Botón reset
        Button resetButton = new Button("Reset Sistema");
        resetButton.setLayoutX(panelX + 10);
        resetButton.setLayoutY(buttonY);
        resetButton.setPrefSize(210, 25);
        resetButton.setOnAction(e -> {
            if (vehicleController != null) {
                vehicleController.resetCollisionSystem();
            }
        });
        this.getChildren().add(resetButton);
        
        buttonY += buttonSpacing;
        
        // Control de velocidad de ticks
        Text speedTitle = new Text(panelX + 10, buttonY, "Velocidad: 50 ticks/seg");
        speedTitle.setFill(Color.WHITE);
        speedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().add(speedTitle);
        
        javafx.scene.control.Slider speedSlider = new javafx.scene.control.Slider();
        speedSlider.setMin(5);
        speedSlider.setMax(100);
        speedSlider.setValue(50);
        speedSlider.setMajorTickUnit(25);
        speedSlider.setMinorTickCount(4);
        speedSlider.setSnapToTicks(false);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setLayoutX(panelX + 10);
        speedSlider.setLayoutY(buttonY + 10);
        speedSlider.setPrefWidth(240);
        speedSlider.setPrefHeight(40);
        
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (tickController != null) {
                int ticksPerSecond = newVal.intValue();
                int intervalMs = Math.max(1, 1000 / ticksPerSecond);
                tickController.setTickInterval(intervalMs);
                speedTitle.setText("Velocidad: " + ticksPerSecond + " ticks/seg");
            }
        });
        this.getChildren().add(speedSlider);
        
        buttonY += 60;
        
        // Información de estado
        Text statusInfo = new Text(panelX + 10, buttonY, "Vehiculos: 0 | Tick: 0");
        statusInfo.setFill(Color.LIGHTGRAY);
        statusInfo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        this.getChildren().add(statusInfo);
        
        // Actualizar información cada segundo
        javafx.animation.Timeline statusUpdater = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                if (vehicleController != null && tickController != null) {
                    String status = String.format("Vehiculos: %d | Tick: %d", 
                        vehicleController.getActiveVehicleCount(), 
                        tickController.getTickCount());
                    statusInfo.setText(status);
                }
            })
        );
        statusUpdater.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        statusUpdater.play();
    }

    /**
     * Obtiene el controlador de escenario para uso externo
     */
    public ScenarioController getScenarioController() {
        return scenarioController;
    }

    /**
     * Gets the traffic controller for external use
     */
    public TrafficController getTrafficController() {
        return trafficController;
    }

    /**
     * Cleanup method to stop traffic control when view is disposed
     */
    public void cleanup() {
        if (trafficController != null) {
            trafficController.stopControl();
        }
    }
    
    /**
     * Genera explicación de ruta para el escenario 2
     */
    private String getRouteExplanationScenario2(String from, String destination) {
        if (destination.equals("Recto")) {
            return "Ruta: " + from + " -> " + (from.equals("West") ? "East" : "West");
        } else {
            // Para destinos norte/sur, mostrar la intersección más cercana
            String intersection = switch (destination) {
                case "North1", "South1" -> "Intersección 2";
                case "North2", "South2" -> "Intersección 3";
                case "North3", "South3" -> "Intersección 4";
                default -> "?";
            };
            return "Ruta: " + from + " -> " + destination + " (" + intersection + ")";
        }
    }
    
    /**
     * Crea un vehículo manual para el escenario 2
     */
    private void createManualVehicleScenario2(String entry, String destination, VehicleTypeEnum type) {
        // Determinar la dirección basada en el destino
        DirectionEnum direction;
        String laneType = "_center_lane"; // Por defecto usar carril central
        
        if (destination.equals("Recto")) {
            direction = DirectionEnum.STRAIGHT;
        } else if (destination.startsWith("South")) {
            direction = DirectionEnum.RIGHT; // Giro a la derecha hacia el sur
            laneType = "_right_lane"; // Usar carril derecho para giros
        } else if (destination.startsWith("North")) {
            direction = DirectionEnum.LEFT; // Giro a la izquierda hacia el norte
            laneType = "_left_lane"; // Usar carril izquierdo para giros
        } else {
            direction = DirectionEnum.STRAIGHT;
        }
        
        // Construir el ID de la calle de entrada
        String streetId = entry.toLowerCase() + laneType + "_segment1";
        
        // Buscar la calle correspondiente
        Street entryStreet = scenarioController.getAllStreets().stream()
            .filter(street -> street.getId().equals(streetId))
            .findFirst()
            .orElse(null);
        
        if (entryStreet != null && vehicleController != null) {
            boolean success = vehicleController.spawnVehicle(entryStreet, type, direction);
            System.out.println("Creando vehículo " + type + " desde " + entry + " hacia " + destination + 
                             " en calle " + streetId + " - " + (success ? "Éxito" : "Falló"));
        } else {
            System.out.println("Error: No se encontró la calle " + streetId + " o vehicleController es null");
        }
    }
}
