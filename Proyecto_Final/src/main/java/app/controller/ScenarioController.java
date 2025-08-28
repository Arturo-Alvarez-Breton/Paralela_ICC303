package app.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.model.Intersection;
import app.model.Street;
import app.model.TrafficLight;
import app.model.enums.DirectionEnum;
import app.service.IntersectionService;
import app.service.StreetService;
import app.service.TrafficLightService;

/**
 * Controller for managing different traffic simulation scenarios.
 * Currently implements Scenario 1: 4-way intersection with standardized streets.
 */
public class ScenarioController {

    private final StreetService streetService;
    private final IntersectionService intersectionService;
    private final TrafficLightService trafficLightService;

    // Collections to hold scenario components
    private List<Street> streets;
    private List<Intersection> intersections;
    private Map<String, Street> streetMap;
    private Map<String, Intersection> intersectionMap;

    // Scenario 2 extras
    private List<TrafficLight> trafficLights;
    private Map<String, TrafficLight> trafficLightMap;

    public ScenarioController() {
        this.streetService = new StreetService();
        this.intersectionService = new IntersectionService();
        this.trafficLightService = new TrafficLightService();
        this.streets = new ArrayList<>();
        this.intersections = new ArrayList<>();
        this.streetMap = new HashMap<>();
        this.intersectionMap = new HashMap<>();
        this.trafficLights = new ArrayList<>();
        this.trafficLightMap = new HashMap<>();
    }

    /**
     * Initializes Scenario 1: 4-way intersection
     * Creates streets approaching from north, south, east, and west
     * Each street connects to its corresponding intersection
     */
    public void initializeScenario1() {
        // Clear any existing data
        clearScenario();

        // Create intersections for each approach
        intersections = intersectionService.createScenario1Intersections();
        for (Intersection intersection : intersections) {
            intersectionMap.put(intersection.getId(), intersection);
        }

        // Create streets with standardized positioning
        createScenario1Streets();

        System.out.println("Scenario 1 initialized successfully:");
        System.out.println("- Streets: " + streets.size());
        System.out.println("- Intersections: " + intersections.size());
    }

    /**
     * Scenario 2: Two-way Highway (East->West top band, West->East bottom band)
     * - 4 intersections: intersection_1 at start (West end), intersection_2/3/4 in middle positions
     * - 6 lanes total (3 per direction), each split into segments
     * - 2 traffic lights per middle intersection (one per direction)
     */
    public void initializeScenario2() {
        clearScenario();

        // Layout constants - centered and matching Scenario 1 dimensions
        final int sceneWidth = 1280;
        final int sceneHeight = 720;
        final int centerX = sceneWidth / 2;
        final int centerY = sceneHeight / 2;

        // Street dimensions matching Scenario 1 - making streets longer
        final int laneWidth = 40; // Same as Scenario 1
        final int laneHeight = 40; // Same as Scenario 1
        final int laneGap = 8; // Slightly larger gap for better visibility
        final int bandGap = 10; // REDUCIDO: Gap mínimo entre top y bottom direction bands
        final int intersectionSize = 80; // Same as Scenario 1

        // Calculate total highway height and center it vertically
        int totalHighwayHeight = (laneHeight * 3) + (laneGap * 2) + bandGap + (laneHeight * 3) + (laneGap * 2);
        int topBandY = centerY - (totalHighwayHeight / 2);

        // Compute lane Y positions (top band: E->W) - INTERCAMBIADO
        int ewLeftY = topBandY;
        int ewCenterY = ewLeftY + laneHeight + laneGap;
        int ewRightY = ewCenterY + laneHeight + laneGap;

        // Bottom band (W->E) - INTERCAMBIADO - PEGADO al top band
        int weLeftY = ewRightY + laneHeight + bandGap;
        int weCenterY = weLeftY + laneHeight + laneGap;
        int weRightY = weCenterY + laneHeight + laneGap;

        // Center intersections horizontally with proper spacing - making highway longer
        int totalRoadLength = 1000; // Increased from 800 to make streets longer
        int leftMargin = centerX - (totalRoadLength / 2);
        int intersectionSpacing = totalRoadLength / 3; // Divide into 3 equal segments for 4 intersections

        int[] intersectionXs = new int[] {
            leftMargin,
            leftMargin + intersectionSpacing,
            leftMargin + (intersectionSpacing * 2),
            leftMargin + totalRoadLength
        };
        String[] intersectionIds = new String[] {"intersection_1", "intersection_2", "intersection_3", "intersection_4"};

        // Intersections with adjusted size to match the reduced highway height
        int intersectionsTopY = topBandY - 5; // Menor margen superior
        int intersectionHeight = totalHighwayHeight + 10; // Menor padding total

        intersections = new ArrayList<>();
        for (int i = 0; i < intersectionXs.length; i++) {
            String id = intersectionIds[i];
            int intersectionIndex = i + 1;
            Intersection inter = intersectionService.createStandardIntersection(id);

            // Ajustar dimensiones según el tipo de intersección
            if (intersectionIndex == 1) {
                // Intersección 1: Solo carriles West (banda superior East->West)
                int westOnlyHeight = (laneHeight * 3) + (laneGap * 2); // Solo 3 carriles West
                int westOnlyTopY = ewLeftY - 5; // Margen pequeño arriba
                inter.setBounds(intersectionXs[i] - (intersectionSize / 2), westOnlyTopY, intersectionSize, westOnlyHeight + 10);
            } else if (intersectionIndex == 4) {
                // Intersección 4: Solo carriles East (banda inferior West->East)
                int eastOnlyHeight = (laneHeight * 3) + (laneGap * 2); // Solo 3 carriles East
                int eastOnlyTopY = weLeftY - 5; // Margen pequeño arriba
                inter.setBounds(intersectionXs[i] - (intersectionSize / 2), eastOnlyTopY, intersectionSize, eastOnlyHeight + 10);
            } else {
                // Intersecciones 2 y 3: Dimensiones completas (ambas bandas + calles de salida)
                inter.setBounds(intersectionXs[i] - (intersectionSize / 2), intersectionsTopY, intersectionSize, intersectionHeight);
            }

            intersections.add(inter);
            intersectionMap.put(id, inter);
        }

        // Streets - INTERCAMBIADOS los parámetros
        createScenario2Streets(
                leftMargin, leftMargin + totalRoadLength, intersectionXs, intersectionSize,
                laneHeight,
                ewLeftY, ewCenterY, ewRightY,  // Ahora East->West está arriba
                weLeftY, weCenterY, weRightY   // Ahora West->East está abajo
        );

        // Traffic lights: for all intersections that have traffic flow
        trafficLights = new ArrayList<>();
        
        // Intersection 1: Only West->East traffic (top band)
        String idInter1We = "traffic_light_intersection_1_we";
        TrafficLight tlInter1We = trafficLightService.createTrafficLight(idInter1We);
        trafficLights.add(tlInter1We);
        trafficLightMap.put(idInter1We, tlInter1We);
        
        // Intersections 2 and 3: Both directions (existing code)
        for (int i = 1; i <= 2; i++) { // Only intersections 2 and 3 have traffic lights
            String idWe = "traffic_light_intersection_" + (i + 1) + "_we";
            String idEw = "traffic_light_intersection_" + (i + 1) + "_ew";
            TrafficLight tlWe = trafficLightService.createTrafficLight(idWe);
            TrafficLight tlEw = trafficLightService.createTrafficLight(idEw);
            trafficLights.add(tlWe);
            trafficLights.add(tlEw);
            trafficLightMap.put(idWe, tlWe);
            trafficLightMap.put(idEw, tlEw);
        }
        
        // Intersection 4: Only East->West traffic (bottom band)
        String idInter4Ew = "traffic_light_intersection_4_ew";
        TrafficLight tlInter4Ew = trafficLightService.createTrafficLight(idInter4Ew);
        trafficLights.add(tlInter4Ew);
        trafficLightMap.put(idInter4Ew, tlInter4Ew);

        System.out.println("Scenario 2 initialized successfully:");
        System.out.println("- Streets (segments): " + streets.size());
        System.out.println("- Intersections: " + intersections.size());
        System.out.println("- Traffic Lights: " + trafficLights.size());
    }

    private void createScenario2Streets(
            int leftMargin, int rightMargin, int[] interXs, int interWidth,
            int laneHeight,
            int ewLeftY, int ewCenterY, int ewRightY,
            int weLeftY, int weCenterY, int weRightY
    ) {
        streets = new ArrayList<>();

        // Helper to add segments for a lane with given base ID and Y
        class LaneBuilder {
            void addSegments(String baseId, int y, List<DirectionEnum> dirs) {
                // segment1: after intersection_1 -> before intersection_2
                int x0 = interXs[0] + interWidth / 2 + 6;
                int x1 = interXs[1] - interWidth / 2 - 6;
                if (x1 > x0) addStreet(baseId + "_segment1", x0, y, x1 - x0, laneHeight, dirs);

                // segment2: after intersection_2 -> before intersection_3
                int x2L = interXs[1] + interWidth / 2 + 6;
                int x2R = interXs[2] - interWidth / 2 - 6;
                if (x2R > x2L) addStreet(baseId + "_segment2", x2L, y, x2R - x2L, laneHeight, dirs);

                // segment3: after intersection_3 -> before intersection_4
                int x3L = interXs[2] + interWidth / 2 + 6;
                int x3R = interXs[3] - interWidth / 2 - 6;
                if (x3R > x3L) addStreet(baseId + "_segment3", x3L, y, x3R - x3L, laneHeight, dirs);

                // segment4 removed - no streets after intersection_4
            }

            void addStreet(String id, int x, int y, int w, int h, List<DirectionEnum> dirs) {
                Street s = streetService.createStreet(id, dirs, x, y, w, h);
                streets.add(s);
                streetMap.put(id, s);
            }

            // Método para crear calles norte-sur de salida alrededor de intersecciones
            void addNorthSouthExitStreets() {
                // Dirección STRAIGHT únicamente para calles de salida
                List<DirectionEnum> straightOnly = List.of(DirectionEnum.STRAIGHT);

                // Para cada intersección, crear calles norte y sur de salida (2 carriles por dirección)
                // ELIMINADO: Las intersecciones 1 y 4 no tendrán calles de salida
                for (int i = 0; i < interXs.length; i++) {
                    int intersectionIndex = i + 1; // intersection_1, intersection_2, etc.

                    // Solo agregar calles de salida para las intersecciones 2 y 3
                    if (intersectionIndex == 1 || intersectionIndex == 4) {
                        continue; // Saltar intersecciones 1 y 4
                    }

                    int intersectionX = interXs[i];
                    String intersectionId = "intersection_" + intersectionIndex;

                    // Obtener la intersección correspondiente para usar sus dimensiones
                    Intersection intersection = intersectionMap.get(intersectionId);
                    if (intersection == null) continue;

                    // Calcular posiciones Y utilizando el tamaño de la intersección
                    // ARREGLO: Usar las dimensiones Y de la intersección para calcular posiciones
                    int northStreetY = intersection.getPosY(); // Parte superior de la intersección
                    int southStreetY = intersection.getPosY() + intersection.getHeight(); // Parte inferior de la intersección

                    // Longitud de las calles norte-sur de salida
                    int exitStreetLength = 120;

                    // === CALLES NORTE (2 carriles lado a lado) - AMBAS SON DE SALIDA ===
                    // Carril Norte Salida Izquierdo - alineado con el centro de la intersección
                    addStreet("north_salida_left_" + intersectionId,
                             intersectionX - laneHeight, northStreetY - exitStreetLength,
                             laneHeight, exitStreetLength, straightOnly);

                    // Carril Norte Salida Derecho - alineado con el centro de la intersección
                    addStreet("north_salida_right_" + intersectionId,
                             intersectionX, northStreetY - exitStreetLength,
                             laneHeight, exitStreetLength, straightOnly);

                    // === CALLES SUR (2 carriles lado a lado) - AMBAS SON DE SALIDA ===
                    // Carril Sur Salida Izquierdo - alineado con el centro de la intersección
                    addStreet("south_salida_left_" + intersectionId,
                             intersectionX - laneHeight, southStreetY,
                             laneHeight, exitStreetLength, straightOnly);

                    // Carril Sur Salida Derecho - alineado con el centro de la intersección
                    addStreet("south_salida_right_" + intersectionId,
                             intersectionX, southStreetY,
                             laneHeight, exitStreetLength, straightOnly);
                }
            }
        }
        LaneBuilder builder = new LaneBuilder();

        // Direction presets
        List<DirectionEnum> leftDirs = streetService.createLeftLaneDirections();
        List<DirectionEnum> centerDirs = streetService.createCenterLaneDirections();
        List<DirectionEnum> rightDirs = streetService.createRightLaneDirections();

        // East -> West lanes (top band) - INTERCAMBIADO
        builder.addSegments("west_right_lane", ewLeftY, leftDirs);
        builder.addSegments("west_center_lane", ewCenterY, centerDirs);
        builder.addSegments("west_left_lane", ewRightY, rightDirs);

        // West -> East lanes (bottom band) - INTERCAMBIADO
        builder.addSegments("east_left_lane", weLeftY, leftDirs);
        builder.addSegments("east_center_lane", weCenterY, centerDirs);
        builder.addSegments("east_right_lane", weRightY, rightDirs);

        // Agregar calles norte-sur de salida alrededor de las intersecciones
        builder.addNorthSouthExitStreets();
    }

    /**
     * Creates the 8 streets for Scenario 1 with proper positioning
     * 4 entry streets (before intersection) and 4 exit streets (after intersection)
     */
    private void createScenario1Streets() {
        int centerX = StreetService.INTERSECTION_CENTER_X;
        int centerY = StreetService.INTERSECTION_CENTER_Y;
        int laneWidth = 40;
        int streetLength = 200;
        int intersectionSize = 80;

        List<DirectionEnum> standardDirections = streetService.createStandardDirections();

        // === LADO SUPERIOR (NORTE) ===
        // Carril azul (entrada): texto "SOUTH", flecha hacia abajo - vehiculos van hacia el sur
        Street norteEntrada = streetService.createStreet(
                "calle_north_entrada", standardDirections,
                centerX - laneWidth, centerY - intersectionSize/2 - streetLength,
                laneWidth, streetLength
        );
        // Carril rojo (salida): texto "NORTH", flecha hacia arriba - vehiculos van hacia el norte
        Street norteSalida = streetService.createStreet(
                "calle_north_salida", standardDirections,
                centerX, centerY - intersectionSize/2 - streetLength,
                laneWidth, streetLength
        );

        // === LADO INFERIOR (SUR) ===
        // Carril azul (entrada): texto "NORTH", flecha hacia arriba - vehiculos van hacia el norte
        Street surEntrada = streetService.createStreet(
                "calle_south_entrada", standardDirections,
                centerX, centerY + intersectionSize/2,
                laneWidth, streetLength
        );
        // Carril rojo (salida): texto "SOUTH", flecha hacia abajo - vehiculos van hacia el sur
        Street surSalida = streetService.createStreet(
                "calle_south_salida", standardDirections,
                centerX - laneWidth, centerY + intersectionSize/2,
                laneWidth, streetLength
        );

        // === LADO DERECHO (ESTE) ===
        // Carril azul (entrada): texto "WEST", flecha hacia oeste - vehiculos van hacia el oeste
        Street esteEntrada = streetService.createStreet(
                "calle_east_entrada", standardDirections,
                centerX + intersectionSize/2, centerY - laneWidth,
                streetLength, laneWidth
        );
        // Carril rojo (salida): texto "EAST", flecha hacia este - vehiculos van hacia el este
        Street esteSalida = streetService.createStreet(
                "calle_east_salida", standardDirections,
                centerX + intersectionSize/2, centerY,
                streetLength, laneWidth
        );

        // === LADO IZQUIERDO (OESTE) ===
        // Carril azul (entrada): texto "EAST", flecha hacia este - vehiculos van hacia el este
        Street oesteEntrada = streetService.createStreet(
                "calle_west_entrada", standardDirections,
                centerX - intersectionSize/2 - streetLength, centerY,
                streetLength, laneWidth
        );
        // Carril rojo (salida): texto "WEST", flecha hacia oeste - vehiculos van hacia el oeste
        Street oesteSalida = streetService.createStreet(
                "calle_west_salida", standardDirections,
                centerX - intersectionSize/2 - streetLength, centerY - laneWidth,
                streetLength, laneWidth
        );

        // Agregar a listas y mapa
        streets.addAll(List.of(
                norteEntrada, norteSalida,
                surEntrada, surSalida,
                esteEntrada, esteSalida,
                oesteEntrada, oesteSalida
        ));

        streetMap.put("north_entrada", norteEntrada);
        streetMap.put("north_salida", norteSalida);
        streetMap.put("south_entrada", surEntrada);
        streetMap.put("south_salida", surSalida);
        streetMap.put("east_entrada", esteEntrada);
        streetMap.put("east_salida", esteSalida);
        streetMap.put("west_entrada", oesteEntrada);
        streetMap.put("west_salida", oesteSalida);
    }

    /**
     * Clears all scenario data
     */
    private void clearScenario() {
        streets.clear();
        intersections.clear();
        streetMap.clear();
        intersectionMap.clear();
        if (trafficLights != null) trafficLights.clear();
        if (trafficLightMap != null) trafficLightMap.clear();
    }

    /**
     * Gets a street by its direction identifier
     * @param direction "norte", "sur", "este", or "oeste"
     * @return Street object or null if not found
     */
    public Street getStreet(String direction) {
        return streetMap.get(direction);
    }

    /**
     * Gets an intersection by its identifier
     * @param id Intersection identifier
     * @return Intersection object or null if not found
     */
    public Intersection getIntersection(String id) {
        return intersectionMap.get(id);
    }

    /**
     * Gets all streets in the current scenario
     * @return List of all streets
     */
    public List<Street> getAllStreets() {
        return new ArrayList<>(streets);
    }

    /**
     * Gets all intersections in the current scenario
     * @return List of all intersections
     */
    public List<Intersection> getAllIntersections() {
        return new ArrayList<>(intersections);
    }

    /**
     * Gets all traffic lights in the current scenario
     */
    public List<TrafficLight> getAllTrafficLights() {
        return new ArrayList<>(trafficLights);
    }

    /**
     * Gets the total number of streets in the scenario
     * @return Number of streets
     */
    public int getStreetCount() {
        return streets.size();
    }

    /**
     * Gets the total number of intersections in the scenario
     * @return Number of intersections
     */
    public int getIntersectionCount() {
        return intersections.size();
    }

    /**
     * Prints scenario configuration for debugging
     */
    public void printScenarioInfo() {
        System.out.println("=== Scenario 1 Configuration ===");
        System.out.println("Streets:");
        for (Street street : streets) {
            System.out.printf("  %s: Position(%d,%d) Size(%dx%d) Directions: %s%n",
                street.getId(), street.getPosX(), street.getPosY(),
                street.getWidth(), street.getHeight(), street.getPossibleDirections());
        }
        System.out.println("Intersections:");
        for (Intersection intersection : intersections) {
            System.out.printf("  %s: Right turn allowed: %s%n",
                intersection.getId(), intersection.isRightTurnAllowed());
        }
    }
}
