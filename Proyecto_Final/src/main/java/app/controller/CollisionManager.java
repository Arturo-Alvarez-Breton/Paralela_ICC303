package app.controller;

import app.service.StreetService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Maneja las colisiones entre vehículos y el control de acceso a la intersección
 * Implementa zonas de espera y detección de hitbox con colas FIFO
 */
public class CollisionManager {
    
    // Constantes de la intersección
    private static final int CENTER_X = StreetService.INTERSECTION_CENTER_X;
    private static final int CENTER_Y = StreetService.INTERSECTION_CENTER_Y;
    private static final int INTERSECTION_SIZE = 80;
    private static final int WAITING_ZONE_SIZE = 60;
    
    // Dimensiones del hitbox de vehículos
    private static final double VEHICLE_WIDTH = 12.0;
    private static final double VEHICLE_HEIGHT = 20.0;
    private static final double COLLISION_BUFFER = 5.0;
    private static final double SPAWN_SEPARATION_DISTANCE = 40.0; // Distancia mínima entre vehículos al spawn
    
    // Control de intersección
    private String vehicleInIntersection = null;
    
    // NUEVO: Control de espera post-salida de intersección
    private int postExitWaitTicks = 0;
    private static final int POST_EXIT_WAIT_DURATION = 10;
    
    // Colas FIFO para diferentes zonas
    private final Queue<String> intersectionQueue = new LinkedBlockingQueue<>();
    private final Queue<String> waitingZoneQueue = new LinkedBlockingQueue<>();
    
    // Colas FIFO para spawn por dirección
    private final Map<String, Queue<String>> spawnQueues = new ConcurrentHashMap<>();
    
    // Tracking de posiciones y orden de llegada
    private final Map<String, VehiclePosition> vehiclePositions = new ConcurrentHashMap<>();
    private final Map<String, Long> vehicleArrivalTime = new ConcurrentHashMap<>();
    private long arrivalCounter = 0;
    // === PRIORIDAD DE EMERGENCIA ===
    private final Map<String, Boolean> emergencyFlag = new ConcurrentHashMap<>(); // vehicleId -> es emergencia
    private final Map<String, String> vehicleLane = new ConcurrentHashMap<>(); // vehicleId -> lane (north/south/east/west)
    private final Map<String, Integer> emergencyCountPerLane = new ConcurrentHashMap<>(); // lane -> count emergencia
    // Cola de prioridad de lanes con emergencia por orden de llegada del PRIMER vehículo de emergencia
    private final PriorityQueue<EmergencyLane> emergencyLaneQueue = new PriorityQueue<>(Comparator.comparingLong(e -> e.firstEmergencyArrivalOrder));
    private final Map<String, EmergencyLane> emergencyLaneIndex = new HashMap<>(); // lane -> EmergencyLane existente
    
    /**
     * Registra un vehículo para tracking de colisiones con entrada de spawn segura
     */
    public boolean canSpawnVehicle(String spawnDirection, double x, double y) {
        // Verificar si hay suficiente espacio en la posición de spawn
        for (VehiclePosition pos : vehiclePositions.values()) {
            double distance = Math.sqrt(Math.pow(x - pos.x, 2) + Math.pow(y - pos.y, 2));
            if (distance < SPAWN_SEPARATION_DISTANCE) {
                System.out.println("Spawn bloqueado: vehículo demasiado cerca en spawn de " + spawnDirection);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Registra un vehículo para tracking de colisiones
     */
    public void registerVehicle(String vehicleId, double x, double y, String spawnDirection) {
        registerVehicle(vehicleId, x, y, spawnDirection, false);
    }

    /**
     * Registro extendido con bandera de emergencia
     */
    public void registerVehicle(String vehicleId, double x, double y, String spawnDirection, boolean isEmergency) {
        vehiclePositions.put(vehicleId, new VehiclePosition(x, y));
        long order = arrivalCounter++;
        vehicleArrivalTime.put(vehicleId, order);
        vehicleLane.put(vehicleId, spawnDirection);
        emergencyFlag.put(vehicleId, isEmergency);
        spawnQueues.computeIfAbsent(spawnDirection, k -> new LinkedBlockingQueue<>());

        if (isEmergency) {
            emergencyCountPerLane.merge(spawnDirection, 1, Integer::sum);
            // Si es la primera emergencia de ese carril, agregar a cola prioritaria
            if (!emergencyLaneIndex.containsKey(spawnDirection)) {
                EmergencyLane el = new EmergencyLane(spawnDirection, order);
                emergencyLaneQueue.add(el);
                emergencyLaneIndex.put(spawnDirection, el);
                System.out.println("[EMERGENCIA] Nuevo carril prioritario: " + spawnDirection + " (orden " + order + ")");
            }
        }
        System.out.println("Vehículo registrado en CollisionManager: " + vehicleId +
                " en (" + x + ", " + y + ") dirección: " + spawnDirection + (isEmergency ? " [EMERGENCY]" : ""));
    }
    
    /**
     * Método de compatibilidad para el registro simple
     */
    public void registerVehicle(String vehicleId, double x, double y) {
        registerVehicle(vehicleId, x, y, "unknown");
    }
    
    /**
     * Actualiza la posición de un vehículo
     */
    public void updateVehiclePosition(String vehicleId, double x, double y) {
        VehiclePosition pos = vehiclePositions.get(vehicleId);
        if (pos != null) {
            pos.x = x;
            pos.y = y;
        }
    }
    
    /**
     * Desregistra un vehículo del sistema
     */
    public void unregisterVehicle(String vehicleId) {
        vehiclePositions.remove(vehicleId);
        vehicleArrivalTime.remove(vehicleId);
        Boolean wasEmergency = emergencyFlag.remove(vehicleId);
        String lane = vehicleLane.remove(vehicleId);
        if (wasEmergency != null && wasEmergency && lane != null) {
            emergencyCountPerLane.merge(lane, -1, Integer::sum);
            if (emergencyCountPerLane.getOrDefault(lane, 0) <= 0) {
                emergencyCountPerLane.remove(lane);
                // Retirar lane de la cola prioritaria
                EmergencyLane el = emergencyLaneIndex.remove(lane);
                if (el != null) {
                    emergencyLaneQueue.remove(el);
                    System.out.println("[EMERGENCIA] Carril deja de tener prioridad: " + lane);
                }
            }
        }
        
        // Remover de todas las colas
        intersectionQueue.remove(vehicleId);
        waitingZoneQueue.remove(vehicleId);
        spawnQueues.values().forEach(queue -> queue.remove(vehicleId));
        
        if (vehicleId.equals(vehicleInIntersection)) {
            vehicleInIntersection = null;
            // Permitir que el siguiente en la cola de intersección pase
            processIntersectionQueue();
            System.out.println("Intersección liberada por vehículo: " + vehicleId);
        }
    }
    
    /**
     * Verifica si un vehículo puede moverse a una nueva posición con sistema FIFO
     */
    public boolean canMove(String vehicleId, double newX, double newY, double speed) {
        // 1. Verificar colisiones con otros vehículos, respetando prioridades FIFO
        if (hasCollisionWithOtherVehicles(vehicleId, newX, newY)) {
            return false;
        }
        
        // 2. Verificar acceso a la intersección con cola FIFO
        if (isInIntersectionZone(newX, newY)) {
            return canEnterIntersectionFIFO(vehicleId);
        }
        
        // 3. Verificar zona de espera con cola FIFO
        if (isInWaitingZone(newX, newY)) {
            return canEnterWaitingZoneFIFO(vehicleId, newX, newY);
        }
        
        return true;
    }
    
    /**
     * Verifica colisiones con otros vehículos respetando prioridades FIFO
     */
    private boolean hasCollisionWithOtherVehicles(String vehicleId, double x, double y) {
        for (Map.Entry<String, VehiclePosition> entry : vehiclePositions.entrySet()) {
            String otherId = entry.getKey();
            if (otherId.equals(vehicleId)) continue;
            
            VehiclePosition otherPos = entry.getValue();
            if (isColliding(x, y, otherPos.x, otherPos.y)) {
                // Si hay colisión, verificar quién llegó primero
                Long myArrival = vehicleArrivalTime.get(vehicleId);
                Long otherArrival = vehicleArrivalTime.get(otherId);
                
                if (myArrival != null && otherArrival != null && myArrival < otherArrival) {
                    // Yo llegué primero, puedo mover
                    System.out.println("Colisión detectada: " + vehicleId + " (llegó primero) vs " + otherId);
                    return false;
                } else {
                    // El otro llegó primero o al mismo tiempo, debo esperar
                    System.out.println("Colisión detectada: " + vehicleId + " espera a " + otherId + " (llegó primero)");
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Calcula si dos vehículos están colisionando
     */
    private boolean isColliding(double x1, double y1, double x2, double y2) {
        double dx = Math.abs(x1 - x2);
        double dy = Math.abs(y1 - y2);
        
        double minDistanceX = VEHICLE_WIDTH + COLLISION_BUFFER;
        double minDistanceY = VEHICLE_HEIGHT + COLLISION_BUFFER;
        
        return dx < minDistanceX && dy < minDistanceY;
    }
    
    /**
     * Verifica si está en la zona de intersección
     */
    private boolean isInIntersectionZone(double x, double y) {
        return x >= CENTER_X - INTERSECTION_SIZE/2.0 && 
               x <= CENTER_X + INTERSECTION_SIZE/2.0 &&
               y >= CENTER_Y - INTERSECTION_SIZE/2.0 && 
               y <= CENTER_Y + INTERSECTION_SIZE/2.0;
    }
    
    /**
     * Verifica si está en la zona de espera
     */
    private boolean isInWaitingZone(double x, double y) {
        return x >= CENTER_X - (INTERSECTION_SIZE/2.0 + WAITING_ZONE_SIZE) && 
               x <= CENTER_X + (INTERSECTION_SIZE/2.0 + WAITING_ZONE_SIZE) &&
               y >= CENTER_Y - (INTERSECTION_SIZE/2.0 + WAITING_ZONE_SIZE) && 
               y <= CENTER_Y + (INTERSECTION_SIZE/2.0 + WAITING_ZONE_SIZE) &&
               !isInIntersectionZone(x, y);
    }
    
    /**
     * Verifica si puede entrar a la intersección usando cola FIFO
     * NUEVO: Implementa espera de 10 ticks después de que un vehículo salga
     */
    private boolean canEnterIntersectionFIFO(String vehicleId) {
        String lane = vehicleLane.get(vehicleId);
        boolean emergencyActive = !emergencyLaneQueue.isEmpty();
        String topEmergencyLane = emergencyActive ? emergencyLaneQueue.peek().lane : null;

        // Si hay emergencia activa, ignorar periodo post-salida para prioridad
        if (!emergencyActive && postExitWaitTicks > 0) {
            if (!intersectionQueue.contains(vehicleId)) {
                intersectionQueue.add(vehicleId);
                System.out.println("Vehículo " + vehicleId + " agregado a cola - esperando " + postExitWaitTicks + " ticks post-salida");
            }
            return false;
        }

        // Intersección libre
        if (vehicleInIntersection == null) {
            if (emergencyActive) {
                if (lane != null && lane.equals(topEmergencyLane)) {
                    int remainingEmergencies = emergencyCountPerLane.getOrDefault(lane, 0);
                    boolean isEmergency = emergencyFlag.getOrDefault(vehicleId, false);
                    // Mientras queden ambulancias en ese carril, solo ellas pueden entrar
                    if (remainingEmergencies > 0 && !isEmergency) {
                        // No permitir todavía a vehículo normal del carril prioritario
                    } else {
                        vehicleInIntersection = vehicleId;
                        intersectionQueue.remove(vehicleId);
                        System.out.println("[EMERGENCIA] Vehículo " + vehicleId + " entra (carril " + lane + ")" + (isEmergency?" [EMERGENCY]":""));
                        return true;
                    }
                }
            } else {
                if (intersectionQueue.isEmpty() || vehicleId.equals(intersectionQueue.peek())) {
                    vehicleInIntersection = vehicleId;
                    intersectionQueue.remove(vehicleId);
                    System.out.println("Vehículo " + vehicleId + " entra a la intersección (FIFO)");
                    return true;
                }
            }
        } else if (vehicleId.equals(vehicleInIntersection)) {
            return true; // ya dentro
        }

        // Añadir a cola si no está
        if (!intersectionQueue.contains(vehicleId)) {
            intersectionQueue.add(vehicleId);
            System.out.println("Vehículo " + vehicleId + (emergencyActive ? " (espera prioridad emergencia)" : "") +
                    " agregado a cola intersección pos " + getQueuePosition(intersectionQueue, vehicleId));
        }
        return false;
    }
    
    /**
     * Verifica si puede entrar a la zona de espera usando cola FIFO
     */
    private boolean canEnterWaitingZoneFIFO(String vehicleId, double x, double y) {
        // Verificar si hay espacio físico
        for (Map.Entry<String, VehiclePosition> entry : vehiclePositions.entrySet()) {
            if (entry.getKey().equals(vehicleId)) continue;
            
            VehiclePosition otherPos = entry.getValue();
            if (isInWaitingZone(otherPos.x, otherPos.y)) {
                double distance = Math.sqrt(Math.pow(x - otherPos.x, 2) + Math.pow(y - otherPos.y, 2));
                if (distance < VEHICLE_HEIGHT * 2) {
                    // Verificar quién llegó primero
                    Long myArrival = vehicleArrivalTime.get(vehicleId);
                    Long otherArrival = vehicleArrivalTime.get(entry.getKey());
                    
                    if (myArrival != null && otherArrival != null && myArrival >= otherArrival) {
                        // El otro llegó primero o al mismo tiempo, debo esperar
                        if (!waitingZoneQueue.contains(vehicleId)) {
                            waitingZoneQueue.add(vehicleId);
                            System.out.println("Vehículo " + vehicleId + " agregado a cola de espera (posición " + 
                                              getQueuePosition(waitingZoneQueue, vehicleId) + ")");
                        }
                        return false;
                    }
                }
            }
        }
        
        // Remover de la cola de espera si puede pasar
        waitingZoneQueue.remove(vehicleId);
        return true;
    }
    
    /**
     * Procesa la cola de intersección cuando se libera
     */
    private void processIntersectionQueue() {
        if (!intersectionQueue.isEmpty()) {
            String nextVehicle = intersectionQueue.peek();
            System.out.println("Siguiente vehículo en cola para intersección: " + nextVehicle);
        }
    }
    
    /**
     * Obtiene la posición de un vehículo en una cola
     */
    private int getQueuePosition(Queue<String> queue, String vehicleId) {
        int position = 1;
        for (String id : queue) {
            if (id.equals(vehicleId)) {
                return position;
            }
            position++;
        }
        return -1;
    }
    
    /**
     * Notifica que un vehículo salió de la intersección
     * NUEVO: Inicia el periodo de espera de 10 ticks para el siguiente vehículo
     */
    public void vehicleExitedIntersection(String vehicleId) {
        VehiclePosition pos = vehiclePositions.get(vehicleId);
        if (pos != null && !isInIntersectionZone(pos.x, pos.y)) {
            if (vehicleId.equals(vehicleInIntersection)) {
                vehicleInIntersection = null;
                // Si no hay emergencias activas, aplicar espera; de lo contrario, liberar inmediatamente
                if (emergencyLaneQueue.isEmpty()) {
                    postExitWaitTicks = POST_EXIT_WAIT_DURATION;
                    System.out.println("Vehículo " + vehicleId + " salió - espera " + POST_EXIT_WAIT_DURATION + " ticks");
                } else {
                    postExitWaitTicks = 0;
                    System.out.println("Vehículo " + vehicleId + " salió - prioridad emergencia continua, sin espera");
                }
                
                processIntersectionQueue(); // Procesar siguiente en cola (pero estará bloqueado por espera)
            }
        }
    }
    
    /**
     * Método que debe ser llamado en cada tick para decrementar el contador de espera
     * NUEVO: Controla el tiempo de espera post-salida de intersección
     */
    public void onTick() {
        if (postExitWaitTicks > 0) {
            postExitWaitTicks--;
            if (postExitWaitTicks == 0) {
                System.out.println("Periodo de espera completado - siguientes vehículos pueden avanzar");
            }
        }
    }
    
    /**
     * Obtiene el estado actual de espera
     */
    public boolean isInPostExitWait() {
        return postExitWaitTicks > 0;
    }
    
    public int getPostExitWaitTicks() {
        return postExitWaitTicks;
    }

    // Métodos de compatibilidad para VehicleController
    public void removeVehicleFromTracking(String vehicleId) {
        unregisterVehicle(vehicleId);
    }
    
    public String getIntersectionStatus() {
        return "Intersección: " + (vehicleInIntersection != null ? vehicleInIntersection : "libre") + 
               " | Cola intersección: " + intersectionQueue.size() + 
               " | Cola espera: " + waitingZoneQueue.size();
    }
    
    public void resetIntersectionState() {
        vehicleInIntersection = null;
        intersectionQueue.clear();
        waitingZoneQueue.clear();
    emergencyLaneQueue.clear();
    emergencyLaneIndex.clear();
    emergencyCountPerLane.clear();
    emergencyFlag.clear();
    vehicleLane.clear();
        System.out.println("Intersección liberada forzosamente");
    }
    
    public boolean isVehicleWaiting(String vehicleId) {
        return intersectionQueue.contains(vehicleId) || waitingZoneQueue.contains(vehicleId);
    }
    
    public Set<String> getVehiclesInIntersection() {
        Set<String> result = new HashSet<>();
        if (vehicleInIntersection != null) {
            result.add(vehicleInIntersection);
        }
        return result;
    }
    
    public Set<String> getVehiclesInWaitingZone() {
        return new HashSet<>(waitingZoneQueue);
    }
    
    /**
     * Clase interna para tracking de posiciones
     */
    private static class VehiclePosition {
        double x, y;
        
        VehiclePosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return String.format("(%.1f, %.1f)", x, y);
        }
    }
    
    /**
     * Debug del estado con información de colas
     */
    public void printStatus() {
        System.out.println("=== COLLISION MANAGER STATUS (FIFO) ===");
        System.out.println("Vehículo en intersección: " + vehicleInIntersection);
        System.out.println("Cola de intersección (" + intersectionQueue.size() + "): " + intersectionQueue);
        System.out.println("Cola de zona de espera (" + waitingZoneQueue.size() + "): " + waitingZoneQueue);
        System.out.println("Vehículos tracked: " + vehiclePositions.size());
        if (!emergencyLaneQueue.isEmpty()) {
            System.out.print("Lanes emergencia prioridad: [");
            emergencyLaneQueue.forEach(el -> System.out.print(el.lane + "(first=" + el.firstEmergencyArrivalOrder + ") "));
            System.out.println("]");
        }
        
        for (Map.Entry<String, VehiclePosition> entry : vehiclePositions.entrySet()) {
            VehiclePosition pos = entry.getValue();
            boolean inIntersection = isInIntersectionZone(pos.x, pos.y);
            boolean inWaiting = isInWaitingZone(pos.x, pos.y);
            Long arrival = vehicleArrivalTime.get(entry.getKey());
            
            System.out.println("  " + entry.getKey() + ": " + pos + 
                             " [Llegada: " + arrival + "]" +
                             (inIntersection ? " [INTERSECCIÓN]" : "") +
                             (inWaiting ? " [ESPERA]" : ""));
        }
        
        // Mostrar colas por spawn
        for (Map.Entry<String, Queue<String>> spawnEntry : spawnQueues.entrySet()) {
            if (!spawnEntry.getValue().isEmpty()) {
                System.out.println("Cola spawn " + spawnEntry.getKey() + ": " + spawnEntry.getValue());
            }
        }
        System.out.println("===============================");
    }
    
    /**
     * Representa un carril con vehículos de emergencia y su orden de prioridad.
     */
    private static class EmergencyLane {
        final String lane;
        final long firstEmergencyArrivalOrder;
        EmergencyLane(String lane, long order) {
            this.lane = lane;
            this.firstEmergencyArrivalOrder = order;
        }
        @Override
        public boolean equals(Object o) { return o instanceof EmergencyLane el && el.lane.equals(this.lane); }
        @Override
        public int hashCode() { return lane.hashCode(); }
        @Override
        public String toString() { return lane + "#" + firstEmergencyArrivalOrder; }
    }
}
