package app.service;

import app.model.Street;
import app.model.enums.DirectionEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for managing street creation and configuration.
 * Provides standardized methods to create streets with consistent dimensions and positioning.
 */
public class StreetService {

    // Standard dimensions for streets
    public static final int STANDARD_STREET_WIDTH = 80;
    public static final int STANDARD_STREET_HEIGHT = 200;
    public static final int STANDARD_THRESHOLD = 10; // Max vehicles per street

    // Reference point - center of intersection
    public static final int INTERSECTION_CENTER_X = 640; // Half of LaunchView.WIDTH
    public static final int INTERSECTION_CENTER_Y = 360; // Half of LaunchView.HEIGHT

    /**
     * Creates a street with standard configuration
     * @param id Unique identifier for the street
     * @param possibleDirections List of allowed directions
     * @param posX X position
     * @param posY Y position
     * @param width Street width
     * @param height Street height
     * @return Configured Street object
     */
    public Street createStreet(String id, List<DirectionEnum> possibleDirections,
                              int posX, int posY, int width, int height) {
        return new Street(id, possibleDirections, STANDARD_THRESHOLD, width, height, posX, posY);
    }

    /**
     * Creates a street with standard dimensions
     * @param id Unique identifier for the street
     * @param possibleDirections List of allowed directions
     * @param posX X position
     * @param posY Y position
     * @return Configured Street object with standard dimensions
     */
    public Street createStandardStreet(String id, List<DirectionEnum> possibleDirections,
                                     int posX, int posY) {
        return createStreet(id, possibleDirections, posX, posY,
                          STANDARD_STREET_WIDTH, STANDARD_STREET_HEIGHT);
    }

    /**
     * Creates all standard directions (all turns allowed)
     * @return List containing all direction enums
     */
    public List<DirectionEnum> createAllDirections() {
        return Arrays.asList(DirectionEnum.STRAIGHT, DirectionEnum.LEFT,
                           DirectionEnum.RIGHT, DirectionEnum.U_TURN);
    }

    /**
     * Creates directions without U-turn (more realistic for most intersections)
     * @return List containing all directions except U-turn
     */
    public List<DirectionEnum> createStandardDirections() {
        return Arrays.asList(DirectionEnum.STRAIGHT, DirectionEnum.LEFT, DirectionEnum.RIGHT);
    }

    /**
     * Creates directions for a restricted street (only straight and right)
     * @return List containing straight and right directions
     */
    public List<DirectionEnum> createRestrictedDirections() {
        return Arrays.asList(DirectionEnum.STRAIGHT, DirectionEnum.RIGHT);
    }
}
