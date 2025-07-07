package app.model;

import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;

public class Vehicle {
    private String id;
    private VehicleTypeEnum type;
    private DirectionEnum direction;
    private boolean inIntersection;

    public Vehicle(String id, VehicleTypeEnum type, DirectionEnum direction) {
        this.id = id;
        this.type = type;
        this.direction = direction;
        this.inIntersection = false; // Default value
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public VehicleTypeEnum getType() {
        return type;
    }

    public void setType(VehicleTypeEnum type) {
        this.type = type;
    }

    public DirectionEnum getDirection() {
        return direction;
    }

    public void setDirection(DirectionEnum direction) {
        this.direction = direction;
    }

    public boolean isInIntersection() {
        return inIntersection;
    }

    public void setInTersection(boolean inTersection) {
        this.inIntersection = inTersection;
    }
}


