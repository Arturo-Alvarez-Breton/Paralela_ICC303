package app.model;

import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;
import java.util.concurrent.atomic.AtomicLong;

public class Vehicle {

    // Contador global para asignar un orden único a cada vehículo al instanciarlo
    private static final AtomicLong COUNTER = new AtomicLong(0);
    private String id;
    private VehicleTypeEnum type;
    private DirectionEnum direction;
    private boolean inIntersection;
    // se coloca final porque no se va a cambiar el arrival order en el programa una vez el carro es introducido solo debe tener 1 unico valor en arrival order
    private final long arrivalOrder;
    private final Movement.Entry entryPoint;

    public Vehicle(String id,
                   VehicleTypeEnum type,
                   DirectionEnum direction,
                   Movement.Entry entryPoint) {
        this.arrivalOrder   = COUNTER.getAndIncrement();
        this.id             = id;
        this.type           = type;
        this.direction      = direction;
        this.entryPoint     = entryPoint;
        this.inIntersection = false;
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

    public void setInIntersection(boolean inIntersection) {
        this.inIntersection = inIntersection;
    }

    public long getArrivalOrder() {
        return arrivalOrder;
    }

    public Movement getMovement() {
        return new Movement(entryPoint,
                Movement.Turn.valueOf(direction.name()));
    }
}


