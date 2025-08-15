package app.controller;

import app.model.Intersection;
import app.model.Street;
import app.service.IntersectionService;
import app.service.StreetService;
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

    // Collections to hold scenario components
    private List<Street> streets;
    private List<Intersection> intersections;
    private Map<String, Street> streetMap;
    private Map<String, Intersection> intersectionMap;

    public ScenarioController() {
        this.streetService = new StreetService();
        this.intersectionService = new IntersectionService();
        this.streets = new ArrayList<>();
        this.intersections = new ArrayList<>();
        this.streetMap = new HashMap<>();
        this.intersectionMap = new HashMap<>();
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
