package app.model;

import app.model.enums.VehicleTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class Intersection {
    private String id;
    boolean rightTurnAllowed;
    private PriorityBlockingQueue<Vehicle> vehicleQueue;

    // Visual/geometry info for UI rendering (optional)
    private int posX;    // top-left X
    private int posY;    // top-left Y
    private int width;
    private int height;

    public Intersection(String id, boolean rightTurnAllowed) {
        this.id = id;
        this.rightTurnAllowed = rightTurnAllowed;
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
    // Devuelve y remueve el siguiente vehiculo segun su prioridad
    public Vehicle getNextVehicle() {
        return vehicleQueue.poll();
    }

    // inserta un vehiculo en la cola
    public void addVehicle(Vehicle logicalVehicle) {
        vehicleQueue.offer(logicalVehicle);
    }

    // Devuelve una vista de la cola sin vaciarla
    public List<Vehicle> peekAllVehicles() {
        return new ArrayList<>(vehicleQueue);
    }

    // Elimina un vehículo concreto de la cola
    public boolean removeVehicle(Vehicle v) {
        return vehicleQueue.remove(v);
    }

    // Getter para el ID de la intersección
    public String getId() {
        return id;
    }

    // Getter para verificar si el giro a la derecha está permitido
    public boolean isRightTurnAllowed() {
        return rightTurnAllowed;
    }

    // Geometry getters/setters for UI layout
    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setBounds(int x, int y, int width, int height) {
        this.posX = x;
        this.posY = y;
        this.width = width;
        this.height = height;
    }
}