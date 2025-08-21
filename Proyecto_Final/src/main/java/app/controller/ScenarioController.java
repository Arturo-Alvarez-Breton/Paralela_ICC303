package app.controller;

import app.model.Intersection;
import app.model.Street;
import app.model.TrafficLight;
import app.service.IntersectionService;
import app.service.StreetService;
import app.service.TrafficLightService;
import app.model.enums.DirectionEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Scenario 2: Two-way Highway (West->East top band, East->West bottom band)
     * - 3 intersections at fixed X positions (300, 600, 900)
     * - 6 lanes total (3 per direction), each split into 3 segments
     * - 2 traffic lights per intersection (one per direction)
     */
    public void initializeScenario2() {
        clearScenario();

        // Layout constants
        final int sceneWidth = 1280;
        final int leftMargin = 60;
        final int rightMargin = sceneWidth - 60;
        final int laneHeight = 28;
        final int laneGap = 6;
        final int bandGap = 50; // gap between top and bottom direction bands
        final int topBandY = 220;
        final int intersectionWidth = 22;

        // Compute lane Y positions (top band: W->E)
        int weLeftY = topBandY;
        int weCenterY = weLeftY + laneHeight + laneGap;
        int weRightY = weCenterY + laneHeight + laneGap;

        // Bottom band (E->W)
        int ewLeftY = weRightY + laneHeight + bandGap;   // visually left lane of bottom band
        int ewCenterY = ewLeftY + laneHeight + laneGap;
        int ewRightY = ewCenterY + laneHeight + laneGap;

        // Intersections at fixed X with full height over both bands
        int totalBandHeight = (weRightY + laneHeight) - weLeftY + bandGap + (ewRightY + laneHeight - ewLeftY);
        int intersectionsTopY = weLeftY - 6;

        // Create intersections
        int[] intersectionXs = new int[] {300, 600, 900};
        intersections = new ArrayList<>();
        for (int i = 0; i < intersectionXs.length; i++) {
            String id = "intersection_" + (i + 1);
            Intersection inter = intersectionService.createStandardIntersection(id);
            inter.setBounds(intersectionXs[i] - (intersectionWidth / 2), intersectionsTopY, intersectionWidth, totalBandHeight + 12);
            intersections.add(inter);
            intersectionMap.put(id, inter);
        }

        // Streets
        createScenario2Streets(
                leftMargin, rightMargin, intersectionXs, intersectionWidth,
                laneHeight,
                weLeftY, weCenterY, weRightY,
                ewLeftY, ewCenterY, ewRightY
        );

        // Traffic lights: 2 per intersection (we/eW)
        trafficLights = new ArrayList<>();
        for (int i = 0; i < intersectionXs.length; i++) {
            String idWe = "traffic_light_intersection_" + (i + 1) + "_we";
            String idEw = "traffic_light_intersection_" + (i + 1) + "_ew";
            TrafficLight tlWe = trafficLightService.createTrafficLight(idWe);
            TrafficLight tlEw = trafficLightService.createTrafficLight(idEw);
            trafficLights.add(tlWe);
            trafficLights.add(tlEw);
            trafficLightMap.put(idWe, tlWe);
            trafficLightMap.put(idEw, tlEw);
        }

        System.out.println("Scenario 2 initialized successfully:");
        System.out.println("- Streets (segments): " + streets.size());
        System.out.println("- Intersections: " + intersections.size());
        System.out.println("- Traffic Lights: " + trafficLights.size());
    }

    private void createScenario2Streets(
            int leftMargin, int rightMargin, int[] interXs, int interWidth,
            int laneHeight,
            int weLeftY, int weCenterY, int weRightY,
            int ewLeftY, int ewCenterY, int ewRightY
    ) {
        streets = new ArrayList<>();

        // Helper to add 3 segments for a lane with given base ID and Y
        class LaneBuilder {
            void addSegments(String baseId, int y, List<DirectionEnum> dirs) {
                // segment1: left -> before intersection 1
                int x0 = leftMargin;
                int x1 = interXs[0] - interWidth / 2 - 6;
                if (x1 > x0) addStreet(baseId + "_segment1", x0, y, x1 - x0, laneHeight, dirs);

                // segment2: after i1 -> before i2
                int x2L = interXs[0] + interWidth / 2 + 6;
                int x2R = interXs[1] - interWidth / 2 - 6;
                if (x2R > x2L) addStreet(baseId + "_segment2", x2L, y, x2R - x2L, laneHeight, dirs);

                // segment3: after i2 -> before i3
                int x3L = interXs[1] + interWidth / 2 + 6;
                int x3R = interXs[2] - interWidth / 2 - 6;
                if (x3R > x3L) addStreet(baseId + "_segment3", x3L, y, x3R - x3L, laneHeight, dirs);
            }

            void addStreet(String id, int x, int y, int w, int h, List<DirectionEnum> dirs) {
                Street s = streetService.createStreet(id, dirs, x, y, w, h);
                streets.add(s);
                streetMap.put(id, s);
            }
        }
        LaneBuilder builder = new LaneBuilder();

        // Direction presets
        List<DirectionEnum> leftDirs = streetService.createLeftLaneDirections();
        List<DirectionEnum> centerDirs = streetService.createCenterLaneDirections();
        List<DirectionEnum> rightDirs = streetService.createRightLaneDirections();

        // West -> East lanes (top band)
        builder.addSegments("west_east_left_lane", weLeftY, leftDirs);
        builder.addSegments("west_east_center_lane", weCenterY, centerDirs);
        builder.addSegments("west_east_right_lane", weRightY, rightDirs);

        // East -> West lanes (bottom band)
        builder.addSegments("east_west_right_lane", ewLeftY, leftDirs);
        builder.addSegments("east_west_center_lane", ewCenterY, centerDirs);
        builder.addSegments("east_west_left_lane", ewRightY, rightDirs);
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

        // === NORTH ===
        Street norteEntrada = streetService.createStreet(
                "calle_north_entrada", standardDirections,
                centerX, centerY + intersectionSize/2,
                laneWidth, streetLength
        );
        Street norteSalida = streetService.createStreet(
                "calle_north_salida", standardDirections,
                centerX, centerY - intersectionSize/2 - streetLength,
                laneWidth, streetLength
        );

        // === SOUTH ===
        Street surEntrada = streetService.createStreet(
                "calle_south_entrada", standardDirections,
                centerX - laneWidth, centerY - intersectionSize/2 - streetLength,
                laneWidth, streetLength
        );
        Street surSalida = streetService.createStreet(
                "calle_south_salida", standardDirections,
                centerX - laneWidth, centerY + intersectionSize/2,
                laneWidth, streetLength
        );

        // === EAST ===
        Street esteEntrada = streetService.createStreet(
                "calle_east_entrada", standardDirections,
                centerX - intersectionSize/2 - streetLength, centerY,
                streetLength, laneWidth
        );
        Street esteSalida = streetService.createStreet(
                "calle_east_salida", standardDirections,
                centerX + intersectionSize/2, centerY,
                streetLength, laneWidth
        );

        // === WEST ===
        Street oesteEntrada = streetService.createStreet(
                "calle_west_entrada", standardDirections,
                centerX + intersectionSize/2, centerY - laneWidth,
                streetLength, laneWidth
        );
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
