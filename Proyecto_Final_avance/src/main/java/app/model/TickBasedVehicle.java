package app.model;

import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Vehículo mejorado para sistema basado en ticks con estados robustos
 */
public class TickBasedVehicle {
    
    // Contador global para IDs únicos
    private static final AtomicLong COUNTER = new AtomicLong(0);
    
    // === IDENTIDAD Y CARACTERÍSTICAS ===
    private final String id;
    private final VehicleTypeEnum type;
    private final DirectionEnum direction;
    private final Movement.Entry entryPoint;
    private final long arrivalOrder;
    
    // === ESTADO Y POSICIONAMIENTO ===
    private VehicleState state;
    private Position currentPosition;
    private int queuePosition; // Posición en la cola (0 = primero)
    
    // === VELOCIDAD Y MOVIMIENTO ===
    private static final double NORMAL_SPEED = 2.0;  // píxeles por tick
    private static final double EMERGENCY_SPEED = 2.5; // píxeles por tick (ligeramente más rápido)
    private final double speed;
    
    // === RUTA DE MOVIMIENTO ===
    private List<Position> routePositions;
    private int currentRouteIndex;
    private Position currentTarget;
    
    // === ÁREA DE COLISIÓN ===
    private static final double COLLISION_RADIUS = 12.0; // Radio de colisión del vehículo (reducido para menos false positives)
    
    // === CONTROL DE AUTORIZACIÓN ===
    private boolean authorizedToCross;
    private long ticksWaiting; // Cuántos ticks ha estado esperando
    
    public TickBasedVehicle(String customId, VehicleTypeEnum type, DirectionEnum direction, Movement.Entry entryPoint) {
        this.arrivalOrder = COUNTER.getAndIncrement();
        this.id = customId != null ? customId : "v" + arrivalOrder;
        this.type = type;
        this.direction = direction;
        this.entryPoint = entryPoint;
        this.state = VehicleState.APPROACHING;
        this.authorizedToCross = false;
        this.ticksWaiting = 0;
        this.queuePosition = 0;
        this.currentRouteIndex = 0;
        
        // Velocidad según tipo
        this.speed = (type == VehicleTypeEnum.EMERGENCY) ? EMERGENCY_SPEED : NORMAL_SPEED;
        
        // Inicializar posición y ruta
        initializeRoute();
    }
    
    /**
     * Inicializa la ruta completa del vehículo con coordenadas exactas del sistema original
     */
    private void initializeRoute() {
        routePositions = new ArrayList<>();
        String entryPointStr = entryPoint.name().toLowerCase();
        
        // 1. Posición de inicio
        Position startPos = IntersectionCoordinates.getStartPosition(entryPointStr);
        routePositions.add(new Position(startPos));
        
        // 2. Posición de parada (se ajustará con spacing cuando se asigne queue position)
        Position stopPos = IntersectionCoordinates.getStopLinePosition(entryPointStr);
        routePositions.add(new Position(stopPos));
        
        // 3. Posiciones intermedias (para giros y U-turns)
        Position[] intermediates = IntersectionCoordinates.getIntermediatePositions(entryPointStr, direction);
        for (Position intermediate : intermediates) {
            routePositions.add(new Position(intermediate));
        }
        
        // 4. Posición de salida
        Position exitPos = IntersectionCoordinates.getExitPosition(entryPointStr, direction);
        routePositions.add(new Position(exitPos));
        
        // Inicializar posición actual y target de manera segura
        if (routePositions.size() > 0) {
            this.currentPosition = new Position(routePositions.get(0));
        } else {
            // Fallback si no hay posiciones en la ruta
            this.currentPosition = new Position(0, 0);
        }
        
        if (routePositions.size() > 1) {
            this.currentTarget = new Position(routePositions.get(1));
        } else {
            // Fallback si no hay target disponible
            this.currentTarget = new Position(this.currentPosition);
        }
    }
    
    /**
     * Actualiza la posición de parada considerando la posición en la cola
     * ARREGLADO: Maneja movimiento gradual en lugar de teletransporte
     */
    public void updateQueuePosition(int newQueuePosition) {
        try {
            int oldQueuePosition = this.queuePosition;
            this.queuePosition = newQueuePosition;
            
            // Actualizar la posición de parada en la ruta (índice 1)
            if (routePositions != null && routePositions.size() > 1) {
                String entryPointStr = entryPoint.name().toLowerCase();
                Position adjustedStopPos = IntersectionCoordinates.getStopLinePositionWithSpacing(entryPointStr, queuePosition);
                
                if (adjustedStopPos != null) {
                    routePositions.set(1, adjustedStopPos);
                    
                    // NUEVO: Si el vehículo está WAITING, debe moverse hacia su nueva posición de parada
                    if (state == VehicleState.WAITING && currentPosition != null) {
                        // Calcular la distancia a la nueva posición de parada
                        double distanceToNewStop = currentPosition.distanceTo(adjustedStopPos);
                        
                        // Solo mover si hay una distancia significativa (evitar micro-movimientos)
                        if (distanceToNewStop > 5.0) {
                            // Si estamos esperando y hay distancia significativa, establecer nuevo objetivo
                            currentTarget = new Position(adjustedStopPos);
                            // Cambiar temporalmente a APPROACHING para que se mueva
                            state = VehicleState.APPROACHING;
                            System.out.println("🚗 Vehículo " + id + " avanzando en cola desde posición " + oldQueuePosition + " a " + newQueuePosition + " (distancia: " + String.format("%.1f", distanceToNewStop) + ")");
                        } else {
                            System.out.println("📍 Vehículo " + id + " ya está cerca de su nueva posición en cola " + newQueuePosition);
                        }
                    }
                    // Si actualmente nos dirigimos a la parada, actualizar el target
                    else if (currentRouteIndex == 0 && currentTarget != null) {
                        currentTarget = new Position(adjustedStopPos);
                    }
                }
            }
            
            // MEJORADO: Solo actualizar posición inicial si NO causa teletransporte
            updateInitialPositionWithSpacingGradual();
            
        } catch (Exception e) {
            System.err.println("Error actualizando posición en cola para vehículo " + id + ": " + e.getMessage());
        }
    }
    
    /**
     * Actualiza la posición inicial del vehículo con spacing para evitar colisiones inmediatas
     * VERSIÓN ORIGINAL (sin cambios para mantener compatibilidad)
     */
    private void updateInitialPositionWithSpacing() {
        try {
            if (queuePosition > 0 && routePositions != null && !routePositions.isEmpty()) {
                String entryPointStr = entryPoint.name().toLowerCase();
                Position baseStartPos = IntersectionCoordinates.getStartPosition(entryPointStr);
                
                // Aplicar spacing hacia atrás desde la posición inicial
                double spacing = queuePosition * IntersectionCoordinates.VEHICLE_SPACING;
                Position spacedStartPos = new Position(baseStartPos);
                
                switch (entryPointStr) {
                    case "norte" -> spacedStartPos.setY(baseStartPos.getY() - spacing);
                    case "sur" -> spacedStartPos.setY(baseStartPos.getY() + spacing);
                    case "este" -> spacedStartPos.setX(baseStartPos.getX() + spacing);
                    case "oeste" -> spacedStartPos.setX(baseStartPos.getX() - spacing);
                }
                
                // Actualizar posición inicial en la ruta
                routePositions.set(0, spacedStartPos);
                
                // Si el vehículo aún está en la posición inicial, actualizar su posición actual
                if (currentRouteIndex == 0) {
                    currentPosition = new Position(spacedStartPos);
                }
            }
        } catch (Exception e) {
            System.err.println("Error actualizando posición inicial con spacing para vehículo " + id + ": " + e.getMessage());
        }
    }
    
    /**
     * Versión mejorada que evita teletransporte
     * ARREGLADO: Solo actualiza si el vehículo NO se ha movido significativamente
     */
    private void updateInitialPositionWithSpacingGradual() {
        try {
            if (queuePosition > 0 && routePositions != null && !routePositions.isEmpty()) {
                String entryPointStr = entryPoint.name().toLowerCase();
                Position baseStartPos = IntersectionCoordinates.getStartPosition(entryPointStr);
                
                // Aplicar spacing hacia atrás desde la posición inicial
                double spacing = queuePosition * IntersectionCoordinates.VEHICLE_SPACING;
                Position spacedStartPos = new Position(baseStartPos);
                
                switch (entryPointStr) {
                    case "norte" -> spacedStartPos.setY(baseStartPos.getY() - spacing);
                    case "sur" -> spacedStartPos.setY(baseStartPos.getY() + spacing);
                    case "este" -> spacedStartPos.setX(baseStartPos.getX() + spacing);
                    case "oeste" -> spacedStartPos.setX(baseStartPos.getX() - spacing);
                }
                
                // Actualizar posición inicial en la ruta
                routePositions.set(0, spacedStartPos);
                
                // MEJORADO: Solo "teletransportar" si está muy cerca de la posición inicial original
                if (currentRouteIndex == 0 && currentPosition != null) {
                    double distanceFromOriginalStart = currentPosition.distanceTo(baseStartPos);
                    
                    // Solo actualizar posición si está muy cerca del punto de partida original
                    if (distanceFromOriginalStart < IntersectionCoordinates.VEHICLE_SPACING) {
                        currentPosition = new Position(spacedStartPos);
                        System.out.println("📍 Vehículo " + id + " reposicionado con spacing en cola " + queuePosition);
                    } else {
                        // Si ya se movió significativamente, NO teletransportar
                        System.out.println("⚠️ Vehículo " + id + " ya se movió, evitando teletransporte");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error actualizando posición inicial con spacing gradual para vehículo " + id + ": " + e.getMessage());
        }
    }
    
    /**
     * Actualiza el vehículo en un tick
     * @return true si el vehículo sigue activo, false si ha completado su recorrido
     */
    public boolean updateTick() {
        switch (state) {
            case APPROACHING -> {
                return updateApproaching();
            }
            case WAITING -> {
                return updateWaiting();
            }
            case CROSSING -> {
                return updateCrossing();
            }
            case EXITING -> {
                return updateExiting();
            }
            case COMPLETED -> {
                return false; // Vehículo completado
            }
        }
        return true;
    }
    
    private boolean updateApproaching() {
        try {
            // Verificar que tenemos un target válido
            if (currentTarget == null || currentPosition == null) {
                System.err.println("Error: vehículo " + id + " no tiene target o posición válida en APPROACHING");
                state = VehicleState.COMPLETED;
                return false;
            }
            
            // Moverse hacia el objetivo (puede ser parada inicial o nueva posición en cola)
            currentPosition.moveTowards(currentTarget, speed);
            
            // Verificar si llegó al objetivo
            if (currentPosition.distanceTo(currentTarget) < 1.0) {
                // MEJORADO: Siempre volver a WAITING cuando llega a cualquier posición de parada
                state = VehicleState.WAITING;
                ticksWaiting = 0;
                
                // Log para debug del comportamiento de cola
                System.out.println("🛑 Vehículo " + id + " llegó a posición de parada (cola: " + queuePosition + ")");
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error en updateApproaching para vehículo " + id + ": " + e.getMessage());
            state = VehicleState.COMPLETED;
            return false;
        }
    }
    
    private boolean updateWaiting() {
        try {
            ticksWaiting++;
            
            // CAMBIADO: NO cambiar automáticamente a CROSSING aquí
            // El controller se encargará de promover vehículos autorizados a CROSSING
            // de manera segura, verificando que no haya otros vehículos cruzando
            
            return true;
        } catch (Exception e) {
            System.err.println("Error en updateWaiting para vehículo " + id + ": " + e.getMessage());
            state = VehicleState.COMPLETED;
            return false;
        }
    }
    
    /**
     * NUEVO: Promover vehículo a estado CROSSING de manera segura
     * Solo debe ser llamado por el controller tras verificaciones
     */
    public boolean promoteToTraversing() {
        try {
            if (state != VehicleState.WAITING || !authorizedToCross) {
                System.err.println("Error: vehículo " + id + " no está listo para promoción a CROSSING");
                return false;
            }
            
            state = VehicleState.CROSSING;
            if (!moveToNextTarget()) {
                System.err.println("Error: vehículo " + id + " no pudo moverse al siguiente target en promoción");
                state = VehicleState.COMPLETED;
                return false;
            }
            
            System.out.println("🚦 Vehículo " + id + " promovido a CROSSING");
            return true;
        } catch (Exception e) {
            System.err.println("Error en promoteToTraversing para vehículo " + id + ": " + e.getMessage());
            state = VehicleState.COMPLETED;
            return false;
        }
    }
    
    private boolean updateCrossing() {
        try {
            // Verificar que tenemos un target válido
            if (currentTarget == null || currentPosition == null) {
                System.err.println("Error: vehículo " + id + " no tiene target o posición válida en CROSSING");
                state = VehicleState.COMPLETED;
                return false;
            }
            
            // Moverse hacia el siguiente target en la ruta
            currentPosition.moveTowards(currentTarget, speed);
            
            // Verificar si llegó al target actual
            if (currentPosition.distanceTo(currentTarget) < 1.0) {
                if (hasNextTarget()) {
                    if (!moveToNextTarget()) {
                        System.err.println("Error: vehículo " + id + " no pudo moverse al siguiente target en CROSSING");
                        state = VehicleState.COMPLETED;
                        return false;
                    }
                } else {
                    // Ha llegado al final de la ruta de cruce, cambiar a exiting
                    state = VehicleState.EXITING;
                }
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error en updateCrossing para vehículo " + id + ": " + e.getMessage());
            state = VehicleState.COMPLETED;
            return false;
        }
    }
    
    private boolean updateExiting() {
        try {
            // Verificar que tenemos posición válida
            if (currentPosition == null) {
                System.err.println("Error: vehículo " + id + " no tiene posición válida en EXITING");
                state = VehicleState.COMPLETED;
                return false;
            }
            
            // Continuar hasta la posición final
            if (currentTarget != null) {
                currentPosition.moveTowards(currentTarget, speed);
                
                // Verificar si llegó al final
                if (currentPosition.distanceTo(currentTarget) < 1.0) {
                    state = VehicleState.COMPLETED;
                    return false; // Vehículo completado
                }
            } else {
                // Si no hay target, marcar como completado
                state = VehicleState.COMPLETED;
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Error en updateExiting para vehículo " + id + ": " + e.getMessage());
            state = VehicleState.COMPLETED;
            return false;
        }
    }
    
    private boolean moveToNextTarget() {
        try {
            currentRouteIndex++;
            if (currentRouteIndex < routePositions.size()) {
                Position nextPos = routePositions.get(currentRouteIndex);
                if (nextPos != null) {
                    currentTarget = new Position(nextPos);
                    return true;
                } else {
                    System.err.println("Error: posición null en ruta para vehículo " + id + " en índice " + currentRouteIndex);
                    currentTarget = null;
                    return false;
                }
            } else {
                currentTarget = null;
                return true; // Es válido llegar al final de la ruta
            }
        } catch (Exception e) {
            System.err.println("Error en moveToNextTarget para vehículo " + id + ": " + e.getMessage());
            currentTarget = null;
            return false;
        }
    }
    
    private boolean hasNextTarget() {
        return currentRouteIndex + 1 < routePositions.size();
    }
    
    /**
     * Verifica si este vehículo está en colisión con otro de manera segura
     */
    public boolean isCollidingWith(TickBasedVehicle other) {
        // Verificaciones de seguridad básicas
        if (other == null || other.state == VehicleState.COMPLETED) return false;
        if (this.state == VehicleState.COMPLETED) return false;
        
        // Verificar que las posiciones no sean null
        if (this.currentPosition == null || other.currentPosition == null) return false;
        
        try {
            double distance = currentPosition.distanceTo(other.currentPosition);
            return distance < (COLLISION_RADIUS * 2);
        } catch (Exception e) {
            // Si hay cualquier error calculando la distancia, asumir que no hay colisión
            return false;
        }
    }
    
    /**
     * Verifica si el vehículo está en la zona de intersección
     */
    public boolean isInIntersection() {
        double centerX = IntersectionCoordinates.CENTER;
        double centerY = IntersectionCoordinates.CENTER;
        double intersectionRadius = IntersectionCoordinates.HALF_ROAD_WIDTH;
        
        double distanceToCenter = Math.sqrt(
            Math.pow(currentPosition.getX() - centerX, 2) + 
            Math.pow(currentPosition.getY() - centerY, 2)
        );
        
        return distanceToCenter <= intersectionRadius;
    }
    
    // === GETTERS Y SETTERS ===
    
    public String getId() { return id; }
    public VehicleTypeEnum getType() { return type; }
    public DirectionEnum getDirection() { return direction; }
    public Movement.Entry getEntryPoint() { return entryPoint; }
    public long getArrivalOrder() { return arrivalOrder; }
    public VehicleState getState() { return state; }
    public Position getCurrentPosition() { 
        if (currentPosition == null) {
            // Si por alguna razón la posición es null, devolver una posición por defecto
            return new Position(0, 0);
        }
        return new Position(currentPosition); 
    }
    public int getQueuePosition() { return queuePosition; }
    public boolean isAuthorizedToCross() { return authorizedToCross; }
    public long getTicksWaiting() { return ticksWaiting; }
    public double getSpeed() { return speed; }
    
    public void setAuthorizedToCross(boolean authorized) { 
        this.authorizedToCross = authorized; 
    }
    
    public Movement getMovement() {
        return new Movement(entryPoint, Movement.Turn.valueOf(direction.name()));
    }
    
    // === MÉTODOS DE CONVENIENCIA ===
    
    public boolean isEmergency() {
        return type == VehicleTypeEnum.EMERGENCY;
    }
    
    public boolean isReadyToCross() {
        return state == VehicleState.WAITING;
    }
    
    public boolean isActivelyMoving() {
        return state == VehicleState.APPROACHING || 
               state == VehicleState.CROSSING || 
               state == VehicleState.EXITING;
    }
    
    @Override
    public String toString() {
        return String.format("Vehicle[%s, %s, %s, %s, pos=%.1f,%.1f]", 
            id, type, direction, state, currentPosition.getX(), currentPosition.getY());
    }
}
