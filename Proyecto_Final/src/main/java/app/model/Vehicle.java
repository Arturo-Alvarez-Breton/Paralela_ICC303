package app.model;

import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;
import java.util.concurrent.atomic.AtomicLong;

public class Vehicle {
    private String id;
    private VehicleTypeEnum type;
    private DirectionEnum direction;
    private static final AtomicLong counter = new AtomicLong(0);
    private long creationTime;
    private long entryTime;
    private long exitTime;

    public Vehicle(VehicleTypeEnum type, DirectionEnum direction) {
        this.id = "V" + counter.incrementAndGet();
        this.type = type;
        this.direction = direction;
        this.creationTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }
    public VehicleTypeEnum getType() {
        return type;
    }
    public DirectionEnum getDirection() {
        return direction;
    }
    public long getCreationTime() {
        return creationTime;
    }
    public long getEntryTime() {
        return entryTime;
    }
    public void setEntryTime(long entryTime) {
        this.entryTime = entryTime;
    }
    public long getExitTime() {
        return exitTime;
    }
    public void setExitTime(long exitTime) {
        this.exitTime = exitTime;
    }
    public long getArrivalOrder() {
        return creationTime;
    }

}


