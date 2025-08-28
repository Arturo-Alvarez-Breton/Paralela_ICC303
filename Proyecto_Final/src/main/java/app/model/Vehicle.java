    package app.model;

    import java.util.concurrent.atomic.AtomicLong;

    import app.model.enums.DirectionEnum;
    import app.model.enums.VehicleTypeEnum;

    public class Vehicle {
    private String id;
    private VehicleTypeEnum type;
    private DirectionEnum direction;
    private String specificDestination; // Nuevo campo para destino específico (ej: "S1-1", "S1-2")
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

    // Nuevo constructor con destino específico
    public Vehicle(VehicleTypeEnum type, DirectionEnum direction, String specificDestination) {
        this.id = "V" + counter.incrementAndGet();
        this.type = type;
        this.direction = direction;
        this.specificDestination = specificDestination;
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
    
    public String getSpecificDestination() {
        return specificDestination;
    }
    
    public void setSpecificDestination(String specificDestination) {
        this.specificDestination = specificDestination;
    }
}
