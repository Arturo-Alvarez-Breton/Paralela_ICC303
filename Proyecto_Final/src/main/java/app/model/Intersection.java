package app.model;

import app.enums.VehicleTypeEnum;

import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    boolean rightTurnAllowed;

    // Cola concurrente que ordena vehiculos segun prioridad
    private PriorityBlockingQueue<Vehicle> vehicleQueue;

    public Intersection(String id, boolean rightTurnAllowed) {
        this.id = id;
        this.rightTurnAllowed = rightTurnAllowed;

        //Inicializar una cola con comparator que prioriza los vehiculos de emergencia
        this.vehicleQueue = new PriorityBlockingQueue<>(10, (v1, v2) -> {
            if (v1.getType() == VehicleTypeEnum.EMERGENCY && v2.getType() != VehicleTypeEnum.EMERGENCY) {
                return -1;  // v1 antes que v2
            } else if (v1.getType() != VehicleTypeEnum.EMERGENCY && v2.getType() == VehicleTypeEnum.EMERGENCY) {
                return 1;   // v2 antes que v1
            }
            // si son del mismo tipo por ejemplo dos ambulancias ordenar por arrivalOrder (menor = llegó antes)
            return Long.compare(v1.getArrivalOrder(), v2.getArrivalOrder());
        });
    }

    public Vehicle getNextVehicle() {
        return vehicleQueue.poll();
    }

    public void addVehicle(Vehicle logicalVehicle) {
        vehicleQueue.offer(logicalVehicle);
    }
}