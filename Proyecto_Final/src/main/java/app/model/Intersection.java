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
                return -1; // v1 tiene mayor prioridad
            } else if (v1.getType() != VehicleTypeEnum.EMERGENCY && v2.getType() == VehicleTypeEnum.EMERGENCY) {
                return 1; // v2 tiene mayor prioridad
            }

            return 0;
        });
    }

    public void addvehicle(Vehicle vehicle) {
        vehicleQueue.offer(vehicle);
    }

    public Vehicle getNextVehicle() {
        return vehicleQueue.poll();
    }

    public void addVehicle(Vehicle logicalVehicle) {
        vehicleQueue.offer(logicalVehicle);
    }
}