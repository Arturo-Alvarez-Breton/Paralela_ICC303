package app.model;

import app.enums.DirectionEnum;
import app.enums.vehicleTypeEnum;

public class Vehicle {
    private String id;
    private vehicleTypeEnum type;
    private DirectionEnum direction;
    private boolean inTersection;

    public Vehicle(String id, vehicleTypeEnum type, DirectionEnum direction) {
        this.id = id;
        this.type = type;
        this.direction = direction;
        this.inTersection = false; // Default value
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public vehicleTypeEnum getType() {
        return type;
    }

    public void setType(vehicleTypeEnum type) {
        this.type = type;
    }

    public DirectionEnum getDirection() {
        return direction;
    }

    public void setDirection(DirectionEnum direction) {
        this.direction = direction;
    }

    public boolean isInTersection() {
        return inTersection;
    }

    public void setInTersection(boolean inTersection) {
        this.inTersection = inTersection;
    }
}


