package app.service;

import app.model.TrafficLight;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing traffic light creation and configuration.
 * Provides standardized methods to create traffic lights for different scenarios.
 */
public class TrafficLightService {

    /**
     * Creates a traffic light with specified ID
     * @param id Unique identifier for the traffic light
     * @return Configured TrafficLight object
     */
    public TrafficLight createTrafficLight(String id) {
        return new TrafficLight(id);
    }

    /**
     * Creates multiple traffic lights for Scenario 1
     * @return List of 4 traffic lights for each direction
     */
    public List<TrafficLight> createScenario1TrafficLights() {
        List<TrafficLight> trafficLights = new ArrayList<>();
        String[] directions = {"norte", "sur", "este", "oeste"};

        for (String direction : directions) {
            trafficLights.add(createTrafficLight("semaforo_" + direction));
        }

        return trafficLights;
    }
}
