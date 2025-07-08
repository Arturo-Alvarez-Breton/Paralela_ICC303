package app.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa un carril ocupado por un vehículo en una intersección.
 * Contiene información sobre el vehículo que ocupa el carril y el tiempo de ocupación.
 */
public class OccupiedLane {
    private final Vehicle vehicle;
    private final Movement movement;
    private final LocalDateTime occupiedSince;
    private final String laneId;
    
    /**
     * Constructor para crear un carril ocupado
     * @param vehicle Vehículo que ocupa el carril
     * @param movement Movimiento del vehículo (entrada + giro)
     * @param occupiedSince Momento en que se ocupó el carril
     */
    public OccupiedLane(Vehicle vehicle, Movement movement, LocalDateTime occupiedSince) {
        this.vehicle = vehicle;
        this.movement = movement;
        this.occupiedSince = occupiedSince;
        // Generar ID único del carril basado en entrada y giro
        this.laneId = movement.getEntry() + "_" + movement.getTurn();
    }
    
    /**
     * Constructor convencional que usa el tiempo actual
     * @param vehicle Vehículo que ocupa el carril
     * @param movement Movimiento del vehículo
     */
    public OccupiedLane(Vehicle vehicle, Movement movement) {
        this(vehicle, movement, LocalDateTime.now());
    }
    
    public Vehicle getVehicle() {
        return vehicle;
    }
    
    public Movement getMovement() {
        return movement;
    }
    
    public LocalDateTime getOccupiedSince() {
        return occupiedSince;
    }
    
    public String getLaneId() {
        return laneId;
    }
    
    /**
     * Verifica si el carril ha estado ocupado por más tiempo del especificado
     * @param secondsThreshold Umbral en segundos
     * @return true si ha pasado el tiempo límite
     */
    public boolean hasBeenOccupiedFor(int secondsThreshold) {
        return occupiedSince.plusSeconds(secondsThreshold).isBefore(LocalDateTime.now());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OccupiedLane)) return false;
        OccupiedLane that = (OccupiedLane) o;
        return Objects.equals(laneId, that.laneId) && 
               Objects.equals(vehicle.getId(), that.vehicle.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(laneId, vehicle.getId());
    }
    
    @Override
    public String toString() {
        return String.format("OccupiedLane{vehicle=%s, lane=%s, since=%s}", 
                           vehicle.getId(), laneId, occupiedSince);
    }
} 