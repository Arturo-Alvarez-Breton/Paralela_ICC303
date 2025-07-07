package app.controller;

import app.model.Intersection;
import app.model.TrafficLight;
import app.model.Vehicle;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrafficController {
    private List<Intersection> intersections;
    private List<TrafficLight> trafficLights;
    private ScheduledExecutorService scheduler;

    public TrafficController(List<Intersection> intersections, List<TrafficLight> trafficLights) {
        this.intersections = intersections;
        this.trafficLights = trafficLights;
        this.scheduler = Executors.newScheduledThreadPool(10);
    }

    public void startControl() {
        for(TrafficLight trafficLight : trafficLights) {
//            scheduler.scheduleAtFixedRate(light::changeLight, 0, 60, TimeUnit.SECONDS);
        }
        scheduler.scheduleAtFixedRate(this::manageIntersections, 0, 1, TimeUnit.SECONDS);
    }

    private void manageIntersections() {
        for (Intersection intersection : intersections) {
            Vehicle nextVehicle = intersection.getNextVehicle();
            if (nextVehicle != null && nextVehicle.isInIntersection()) {
                // Process the vehicle in the intersection
            }
        }
    }

    public void stopControl() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
