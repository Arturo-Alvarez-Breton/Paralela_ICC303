package app.controller;

import app.model.TickBasedVehicle;
import app.model.VehicleState;
import app.model.Position;
import app.model.IntersectionCoordinates;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Detector de colisiones para el sistema basado en ticks
 * Verifica colisiones presentes y futuras entre vehículos
 */
public class CollisionDetector {
    
    // Distancia mínima segura entre vehículos
    private static final double SAFE_DISTANCE = 30.0;
    
    // Radio de la zona de intersección para detección especial - Reservado para uso futuro
    // private static final double INTERSECTION_RADIUS = IntersectionCoordinates.HALF_ROAD_WIDTH;
    
    /**
     * Verifica si hay colisiones actuales entre todos los vehículos activos de manera segura
     */
    public static Set<String> detectCurrentCollisions(List<TickBasedVehicle> vehicles) {
        Set<String> collisions = new HashSet<>();
        
        if (vehicles == null || vehicles.isEmpty()) {
            return collisions;
        }
        
        try {
            for (int i = 0; i < vehicles.size(); i++) {
                TickBasedVehicle vehicle1 = vehicles.get(i);
                
                // Verificaciones de seguridad
                if (vehicle1 == null || vehicle1.getState() == VehicleState.COMPLETED) continue;
                
                for (int j = i + 1; j < vehicles.size(); j++) {
                    TickBasedVehicle vehicle2 = vehicles.get(j);
                    
                    // Verificaciones de seguridad
                    if (vehicle2 == null || vehicle2.getState() == VehicleState.COMPLETED) continue;
                    
                    // Verificar colisión de manera segura
                    try {
                        if (vehicle1.isCollidingWith(vehicle2)) {
                            String vehicle1Id = vehicle1.getId() != null ? vehicle1.getId() : "unknown";
                            String vehicle2Id = vehicle2.getId() != null ? vehicle2.getId() : "unknown";
                            collisions.add(vehicle1Id + "-" + vehicle2Id);
                        }
                    } catch (Exception e) {
                        // Si hay error verificando colisión entre estos dos vehículos específicos,
                        // continuar con otros pares
                        System.err.println("Error verificando colisión entre vehículos: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error general en detección de colisiones: " + e.getMessage());
        }
        
        return collisions;
    }
    
    /**
     * Verifica si es seguro para un vehículo proceder al siguiente estado
     */
    public static boolean isSafeToAuthorize(TickBasedVehicle candidate, List<TickBasedVehicle> allVehicles) {
        
        // 1. Verificar que no haya colisiones actuales
        for (TickBasedVehicle other : allVehicles) {
            if (other == candidate || other.getState() == VehicleState.COMPLETED) continue;
            
            if (candidate.isCollidingWith(other)) {
                return false;
            }
        }
        
        // 2. Verificar conflictos en la intersección
        if (candidate.isReadyToCross()) {
            return isSafeToEnterIntersection(candidate, allVehicles);
        }
        
        return true;
    }
    
    /**
     * Verifica si es seguro para un vehículo entrar a la intersección
     */
    private static boolean isSafeToEnterIntersection(TickBasedVehicle candidate, List<TickBasedVehicle> allVehicles) {
        
        // 1. Verificar que no haya vehículos ya cruzando
        for (TickBasedVehicle other : allVehicles) {
            if (other == candidate || other.getState() == VehicleState.COMPLETED) continue;
            
            // Si hay otro vehículo cruzando, no es seguro
            if (other.getState() == VehicleState.CROSSING && other.isInIntersection()) {
                return false;
            }
        }
        
        // 2. Verificar conflictos de movimiento usando IntersectionManager
        IntersectionManager manager = new IntersectionManager();
        
        // Obtener otros vehículos que también podrían estar esperando para cruzar
        for (TickBasedVehicle other : allVehicles) {
            if (other == candidate || other.getState() == VehicleState.COMPLETED) continue;
            
            // Si hay otro vehículo esperando con movimiento conflictivo
            if (other.isReadyToCross() && other.isAuthorizedToCross()) {
                if (!manager.isSafeToProceed(convertToOldVehicle(candidate), 
                                            List.of(convertToOldVehicle(other)))) {
                    return false;
                }
            }
        }
        
        // 3. Verificar espaciado seguro en la ruta de entrada a la intersección
        return hasAdequateSpacing(candidate, allVehicles);
    }
    
    /**
     * Verifica que haya espaciado adecuado entre vehículos
     */
    private static boolean hasAdequateSpacing(TickBasedVehicle candidate, List<TickBasedVehicle> allVehicles) {
        Position candidatePos = candidate.getCurrentPosition();
        
        for (TickBasedVehicle other : allVehicles) {
            if (other == candidate || other.getState() == VehicleState.COMPLETED) continue;
            
            double distance = candidatePos.distanceTo(other.getCurrentPosition());
            
            // Si están muy cerca y en estados activos de movimiento
            if (distance < SAFE_DISTANCE && 
                (other.isActivelyMoving() || other.isReadyToCross())) {
                
                // Verificar si están en la misma ruta general
                if (areOnSimilarPath(candidate, other)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Verifica si dos vehículos están en rutas similares que podrían generar conflicto
     */
    private static boolean areOnSimilarPath(TickBasedVehicle vehicle1, TickBasedVehicle vehicle2) {
        // Si vienen del mismo punto de entrada
        if (vehicle1.getEntryPoint() == vehicle2.getEntryPoint()) {
            return true;
        }
        
        // Si van hacia el mismo punto de salida
        String entry1 = vehicle1.getEntryPoint().name().toLowerCase();
        String entry2 = vehicle2.getEntryPoint().name().toLowerCase();
        
        Position exit1 = IntersectionCoordinates.getExitPosition(entry1, vehicle1.getDirection());
        Position exit2 = IntersectionCoordinates.getExitPosition(entry2, vehicle2.getDirection());
        
        return exit1.distanceTo(exit2) < SAFE_DISTANCE;
    }
    
    /**
     * Convierte un TickBasedVehicle al formato antiguo para compatibilidad con IntersectionManager
     * TODO: Actualizar IntersectionManager para usar TickBasedVehicle directamente
     */
    private static app.model.Vehicle convertToOldVehicle(TickBasedVehicle tickVehicle) {
        return new app.model.Vehicle(
            tickVehicle.getId(),
            tickVehicle.getType(), 
            tickVehicle.getDirection(),
            tickVehicle.getEntryPoint()
        );
    }
    
    /**
     * Detecta vehículos que están bloqueando el paso de emergencias
     * Útil para implementar la regla de "despeje hacia adelante"
     */
    public static List<TickBasedVehicle> findVehiclesBlockingEmergency(
            TickBasedVehicle emergencyVehicle, 
            List<TickBasedVehicle> allVehicles) {
        
        if (!emergencyVehicle.isEmergency()) {
            return List.of();
        }
        
        return allVehicles.stream()
            .filter(v -> v != emergencyVehicle)
            .filter(v -> v.getState() != VehicleState.COMPLETED)
            .filter(v -> v.getEntryPoint() == emergencyVehicle.getEntryPoint()) // Mismo carril
            .filter(v -> v.getQueuePosition() < emergencyVehicle.getQueuePosition()) // Delante en la cola
            .filter(v -> v.isReadyToCross() || v.getState() == VehicleState.WAITING)
            .toList();
    }
    
    /**
     * Verifica si hay vehículos estancados (esperando demasiado tiempo)
     */
    public static List<TickBasedVehicle> findStuckVehicles(List<TickBasedVehicle> vehicles, long maxWaitingTicks) {
        return vehicles.stream()
            .filter(v -> v.getState() == VehicleState.WAITING)
            .filter(v -> v.getTicksWaiting() > maxWaitingTicks)
            .toList();
    }
}
