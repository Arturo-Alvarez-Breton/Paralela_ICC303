package app.service;

import app.model.Intersection;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing intersection creation and configuration.
 * Provides standardized methods to create intersections for different scenarios.
 */
public class IntersectionService {

    /**
     * Creates an intersection with specified configuration
     * @param id Unique identifier for the intersection
     * @param rightTurnAllowed Whether right turns are allowed at this intersection
     * @return Configured Intersection object
     */
    public Intersection createIntersection(String id, boolean rightTurnAllowed) {
        return new Intersection(id, rightTurnAllowed);
    }

    /**
     * Creates a standard intersection with right turns allowed
     * @param id Unique identifier for the intersection
     * @return Configured Intersection object with right turns enabled
     */
    public Intersection createStandardIntersection(String id) {
        return createIntersection(id, true);
    }

    /**
     * Creates multiple intersections with standard configuration
     * @param ids Array of unique identifiers for the intersections
     * @return List of configured Intersection objects
     */
    public List<Intersection> createMultipleIntersections(String[] ids) {
        List<Intersection> intersections = new ArrayList<>();
        for (String id : ids) {
            intersections.add(createStandardIntersection(id));
        }
        return intersections;
    }

    /**
     * Creates intersections for Scenario 1 (4-way intersection)
     * @return List of 4 intersections for north, south, east, and west approaches
     */
    public List<Intersection> createScenario1Intersections() {
        String[] intersectionIds = {"north", "south", "east", "west"};
        return createMultipleIntersections(intersectionIds);
    }
}
