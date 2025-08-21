package app.ui;

import app.controller.ScenarioController;
import app.model.Street;
import app.model.Intersection;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

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

    public IntersectionView() {
        this(1);
    }

    public IntersectionView(int scenarioNumber) {
        this.scenarioNumber = scenarioNumber;
        this.scenarioController = new ScenarioController();
        initializeScenario();
        drawScenario();
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
            addTitleScenario1();
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
     * Dibuja las calles Norte-Sur con sus carriles correspondientes
     */
    private void drawNorthSouthStreets(List<Street> streets) {
        for (Street street : streets) {
            String dir = parseDirectionFromId(street.getId());
            if ("north".equals(dir) || "south".equals(dir)) {
                drawIndividualStreet(street);
                addStreetDirectionLabel(street);
            }
        }
    }

    /**
     * Dibuja las calles Este-Oeste con sus carriles correspondientes
     */
    private void drawEastWestStreets(List<Street> streets) {
        for (Street street : streets) {
            String dir = parseDirectionFromId(street.getId());
            if ("east".equals(dir) || "west".equals(dir)) {
                drawIndividualStreet(street);
                addStreetDirectionLabel(street);
            }
        }
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
            centerY - intersectionSize/2 - streetLength, // Desde arriba
            2, // Ancho de la línea
            streetLength * 2 + intersectionSize // Altura total
        );
        verticalDivider.setFill(LANE_DIVIDER_COLOR);
        this.getChildren().add(verticalDivider);

        // Línea divisoria horizontal Este-Oeste (entre carriles de entrada y salida)
        Rectangle horizontalDivider = new Rectangle(
            centerX - intersectionSize/2 - streetLength, // Desde la izquierda
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
     * Obtiene la etiqueta apropiada para cada calle
     */
    private String getStreetLabel(Street street) {
        String id = street.getId();
        String dir = parseDirectionFromId(id);
        boolean entrada = id.contains("entrada");
        boolean salida = id.contains("salida");

        if (entrada || salida) {
            switch (dir) {
                case "north": return "↑ NORTH";
                case "south": return "↓ SOUTH";
                case "east":  return "→ EAST";
                case "west":  return "← WEST";
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
        double x = street.getPosX() + street.getWidth() / 2 - 20;
        double y = street.getPosY() + street.getHeight() / 2;

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
            centerX - intersectionSize / 2,
            centerY - intersectionSize / 2,
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

        Text entryText = new Text(75, startY + 12, "Carriles de Entrada");
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
        javafx.scene.control.Button backButton = new javafx.scene.control.Button("← Volver al Menú");
        backButton.setLayoutX(50);
        backButton.setLayoutY(LaunchView.HEIGHT - 80);
        backButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                           "-fx-font-size: 16px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;");

        backButton.setOnAction(e -> {
            // Obtener el Stage actual desde la escena
            javafx.stage.Stage stage = (javafx.stage.Stage) this.getScene().getWindow();
            LaunchView launchView = new LaunchView();
            stage.setScene(launchView.createLaunchScene(stage));
        });

        this.getChildren().add(backButton);
    }

    /**
     * Obtiene el controlador de escenario para uso externo
     */
    public ScenarioController getScenarioController() {
        return scenarioController;
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

        // 4) Semáforos en entradas de intersecciones
        drawScenario2TrafficLights();

        // 5) Título, reglas y volver
        addTitleScenario2();
        addHighwayRulesPanel();
        addBackButton();
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
        // Usamos el mismo layout que en ScenarioController
        final int leftMargin = 60;
        final int rightMargin = LaunchView.WIDTH - 60;
        final int laneHeight = 28;
        final int laneGap = 6;
        final int bandGap = 50;
        final int topBandY = 220;

        int weLeftY = topBandY;
        int weCenterY = weLeftY + laneHeight + laneGap;
        int weRightY = weCenterY + laneHeight + laneGap;

        int ewLeftY = weRightY + laneHeight + bandGap;
        int ewCenterY = ewLeftY + laneHeight + laneGap;
        int ewRightY = ewCenterY + laneHeight + laneGap;

        // Dibujar separaciones internas (después de cada carril excepto el último de cada banda)
        int[] divY = new int[] {
            weLeftY + laneHeight, weCenterY + laneHeight,
            ewLeftY + laneHeight, ewCenterY + laneHeight
        };
        for (int y : divY) {
            Rectangle divider = new Rectangle(leftMargin, y, rightMargin - leftMargin, 2);
            divider.setFill(LANE_DIVIDER_COLOR);
            this.getChildren().add(divider);
        }
    }

    private void drawLaneArrows(Street street) {
        String id = street.getId().toLowerCase();
        String arrow = id.startsWith("west_east") ? "→" : id.startsWith("east_west") ? "←" : "";
        if (arrow.isEmpty()) return;

        int startX = street.getPosX() + 12;
        int endX = street.getPosX() + street.getWidth() - 12;
        int centerY = street.getPosY() + street.getHeight() / 2 + 4;

        for (int x = startX; x < endX; x += 80) {
            Text a = new Text(x, centerY, arrow);
            a.setFill(Color.WHITE);
            a.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            this.getChildren().add(a);
        }
    }

    private void addHighwayLaneLabel(Street street) {
        String id = street.getId().toLowerCase();
        String label = null;
        if (id.startsWith("west_east_left_lane")) label = "W (L)";
        else if (id.startsWith("west_east_center_lane")) label = "West (C)";
        else if (id.startsWith("west_east_right_lane")) label = "West (R)";
        else if (id.startsWith("east_west_left_lane")) label = "East (L)";
        else if (id.startsWith("east_west_center_lane")) label = "East (C)";
        else if (id.startsWith("east_west_right_lane")) label = "East (R)";

        Text t = new Text(street.getPosX() + 6, street.getPosY() + 14, label);
        t.setFill(Color.WHITE);
        t.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.getChildren().add(t);
    }

    private void drawScenario2TrafficLights() {
        // Posicionamos 2 por intersección: uno por banda
        final int laneHeight = 28;
        final int laneGap = 6;
        final int bandGap = 50;
        final int topBandY = 220;
        int weCenterY = topBandY + (laneHeight + laneGap); // carril central W→E
        int ewCenterY = weCenterY + (laneHeight + laneGap) + laneHeight + bandGap + (laneHeight + laneGap); // carril central E→W

        for (Intersection inter : scenarioController.getAllIntersections()) {
            int leftOfInterX = inter.getPosX() - 14; // entrada para W→E
            int rightOfInterX = inter.getPosX() + inter.getWidth() + 2; // entrada para E→W

            // Semáforo W→E (arriba, antes de la intersección)
            drawTrafficLightGlyph(leftOfInterX, weCenterY + laneHeight / 2 - 8);
            // Semáforo E→W (abajo, antes de la intersección)
            drawTrafficLightGlyph(rightOfInterX, ewCenterY + laneHeight / 2 - 8);
        }
    }

    private void drawTrafficLightGlyph(int x, int y) {
        Rectangle box = new Rectangle(x, y, 12, 16);
        box.setArcWidth(4);
        box.setArcHeight(4);
        box.setFill(Color.DARKRED);
        box.setStroke(Color.BLACK);
        this.getChildren().add(box);

        Text s = new Text(x + 3, y + 12, "S");
        s.setFill(Color.WHITE);
        s.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        this.getChildren().add(s);
    }

    private void addHighwayRulesPanel() {
        int x = 50;
        int y = 40;
        Rectangle panel = new Rectangle(x, y, 420, 110);
        panel.setArcWidth(10);
        panel.setArcHeight(10);
        panel.setFill(Color.color(0, 0, 0, 0.35));
        panel.setStroke(Color.WHITE);
        panel.setStrokeWidth(1);
        this.getChildren().add(panel);

        Text title = new Text(x + 12, y + 22, "Reglas de Carriles");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        this.getChildren().add(title);

        Text r1 = new Text(x + 12, y + 42, "Izquierdo: Giro izquierda + U-Turn");
        Text r2 = new Text(x + 12, y + 62, "Central: Solo recto");
        Text r3 = new Text(x + 12, y + 82, "Derecho: Recto + Giro derecha");
        for (Text t : new Text[]{r1, r2, r3}) {
            t.setFill(Color.LIGHTGRAY);
            t.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            this.getChildren().add(t);
        }
    }

    private void addTitleScenario2() {
        Text title = new Text(500, 30, "Escenario 2: Autopista en Dos Direcciones (Oeste–Este)");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        this.getChildren().add(title);

        Text info = new Text(500, 52, String.format(
            "Segmentos: %d | Intersecciones: %d | Semáforos: 6",
            scenarioController.getStreetCount(),
            scenarioController.getIntersectionCount()
        ));
        info.setFill(Color.LIGHTGRAY);
        info.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        this.getChildren().add(info);
    }
}
