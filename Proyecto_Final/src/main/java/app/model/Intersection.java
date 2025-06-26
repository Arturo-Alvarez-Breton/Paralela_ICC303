package app.model;

import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    boolean rightTurnAllowed;
    private PriorityBlockingQueue<Vehicle> vehicleQueue;

    public Intersection(String id, boolean rightTurnAllowed) {
        this.id = id;
        this.rightTurnAllowed = rightTurnAllowed;
        this.vehicleQueue = new PriorityBlockingQueue<>(10, (v1, v2) -> {
            // TODO
            // Implement a comparator based on vehicle priority, e.g., type or direction
            return 0;
        });
    }

    public void addvehicle(Vehicle vehicle) {
        vehicleQueue.offer(vehicle);
    }

    public Vehicle getNextVehicle() {
        return vehicleQueue.poll();
    }
}
