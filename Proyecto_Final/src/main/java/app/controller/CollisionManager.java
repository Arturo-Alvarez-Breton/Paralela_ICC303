package app.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import app.service.StreetService;

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
    
    // Colas de prioridad para diferentes zonas
    private final PriorityBlockingQueue<PriorityVehicle> intersectionQueue = new PriorityBlockingQueue<>();
    private final PriorityBlockingQueue<PriorityVehicle> waitingZoneQueue = new PriorityBlockingQueue<>();
    
    // Colas de prioridad para spawn por dirección
    private final Map<String, PriorityBlockingQueue<PriorityVehicle>> spawnQueues = new ConcurrentHashMap<>();
    
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
    
    // === NUEVO: CONTROL AVANZADO DE AMBULANCIAS ===
    // Lista de vehículos que YA INICIARON el cruce (deben completarlo sin importar ambulancias)
    private final Set<String> vehiclesAlreadyCrossing = new HashSet<>();
    // Por cada ambulancia, lista de vehículos que estaban delante y deben cruzar primero
    private final Map<String, List<String>> vehiclesAheadOfAmbulance = new ConcurrentHashMap<>();
    // Orden de ambulancias para procesar una a la vez
    private final Queue<String> ambulanceProcessingQueue = new LinkedBlockingQueue<>();
    // NUEVO: Vehículos que han salido de la intersección y son completamente libres
    private final Set<String> vehiclesPostIntersection = new HashSet<>();
    
    /**
     * Elemento de cola con prioridad para intersección
     */
    private static class PriorityVehicle implements Comparable<PriorityVehicle> {
        final String vehicleId;
        final long arrivalOrder;
        final boolean isEmergency;
        final boolean hasVehiclesAhead; // Si es ambulancia con vehículos delante
        
        PriorityVehicle(String vehicleId, long arrivalOrder, boolean isEmergency, boolean hasVehiclesAhead) {
            this.vehicleId = vehicleId;
            this.arrivalOrder = arrivalOrder;
            this.isEmergency = isEmergency;
            this.hasVehiclesAhead = hasVehiclesAhead;
        }
        
        @Override
        public int compareTo(PriorityVehicle other) {
            // 1. Vehículos normales que estaban delante de ambulancia (prioridad máxima)
            if (!this.isEmergency && this.hasVehiclesAhead && other.isEmergency) {
                return -1; // Este vehículo normal tiene prioridad sobre ambulancia
            }
            if (this.isEmergency && !other.isEmergency && other.hasVehiclesAhead) {
                return 1; // El otro vehículo normal tiene prioridad
            }
            
            // 2. Entre ambulancias: por orden de llegada
            if (this.isEmergency && other.isEmergency) {
                return Long.compare(this.arrivalOrder, other.arrivalOrder);
            }
            
            // 3. Ambulancias tienen prioridad sobre vehículos normales (que no estaban delante)
            if (this.isEmergency && !other.isEmergency && !other.hasVehiclesAhead) {
                return -1;
            }
            if (!this.isEmergency && !this.hasVehiclesAhead && other.isEmergency) {
                return 1;
            }
            
            // 4. Entre vehículos normales: FIFO (orden de llegada)
            return Long.compare(this.arrivalOrder, other.arrivalOrder);
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj instanceof PriorityVehicle pv && pv.vehicleId.equals(this.vehicleId);
        }
        
        @Override
        public int hashCode() {
            return vehicleId.hashCode();
        }
        
        @Override
        public String toString() {
            return vehicleId + "(arr=" + arrivalOrder + 
                   (isEmergency ? ",EMG" : "") + 
                   (hasVehiclesAhead ? ",AHEAD" : "") + ")";
        }
    }
    
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
        spawnQueues.computeIfAbsent(spawnDirection, k -> new PriorityBlockingQueue<>());

        if (isEmergency) {
            emergencyCountPerLane.merge(spawnDirection, 1, Integer::sum);
            
            // NUEVO: Capturar vehículos que estaban delante de esta ambulancia en su carril
            List<String> vehiclesAhead = new ArrayList<>();
            for (Map.Entry<String, String> entry : vehicleLane.entrySet()) {
                String otherVehicleId = entry.getKey();
                String otherLane = entry.getValue();
                
                // Si está en el mismo carril y llegó antes que la ambulancia
                if (spawnDirection.equals(otherLane)) {
                    Long otherArrival = vehicleArrivalTime.get(otherVehicleId);
                    if (otherArrival != null && otherArrival < order && 
                        !emergencyFlag.getOrDefault(otherVehicleId, false)) {
                        vehiclesAhead.add(otherVehicleId);
                    }
                }
            }
            
            vehiclesAheadOfAmbulance.put(vehicleId, vehiclesAhead);
            ambulanceProcessingQueue.offer(vehicleId);
            
            System.out.println("[EMERGENCIA] Ambulancia " + vehicleId + " registrada en " + spawnDirection + 
                             " (orden " + order + ") - Vehículos delante: " + vehiclesAhead);
            
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
        
        // NUEVO: Limpiar estructuras de ambulancias
        vehiclesAlreadyCrossing.remove(vehicleId);
        vehiclesPostIntersection.remove(vehicleId); // NUEVO: Limpiar vehículos post-intersección
        vehiclesAheadOfAmbulance.remove(vehicleId); // Si era ambulancia
        ambulanceProcessingQueue.remove(vehicleId);
        
        // Remover de las listas de vehículos delante de ambulancias
        for (List<String> vehiclesAhead : vehiclesAheadOfAmbulance.values()) {
            vehiclesAhead.remove(vehicleId);
        }
        
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
        removePriorityVehicle(intersectionQueue, vehicleId);
        removePriorityVehicle(waitingZoneQueue, vehicleId);
        spawnQueues.values().forEach(queue -> removePriorityVehicle(queue, vehicleId));
        
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
        // NUEVO: Si el vehículo ya salió de la intersección, es completamente libre
        if (vehiclesPostIntersection.contains(vehicleId)) {
            return true; // Movimiento completamente libre después de cruzar
        }
        
        // NUEVO: Si el vehículo ya está en la intersección, puede moverse libremente
        // para evitar que se detenga en medio del cruce
        VehiclePosition currentPos = vehiclePositions.get(vehicleId);
        if (currentPos != null && isInIntersectionZone(currentPos.x, currentPos.y)) {
            // Vehículo ya está en la intersección - permitir movimiento sin verificar colisiones
            System.out.println("Vehículo " + vehicleId + " ya en intersección - movimiento libre");
            return true;
        }
        
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
                // NUEVO: Si ambos vehículos están en la intersección, permitir movimiento 
                // (cada uno sigue su ruta sin detenerse)
                boolean meInIntersection = isInIntersectionZone(x, y);
                boolean otherInIntersection = isInIntersectionZone(otherPos.x, otherPos.y);
                
                if (meInIntersection && otherInIntersection) {
                    System.out.println("Ambos vehículos en intersección: " + vehicleId + " y " + otherId + " - permitir movimiento");
                    return false; // No hay colisión, permitir movimiento
                }
                
                // Si hay colisión fuera de la intersección, verificar quién llegó primero
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
     * Verifica si puede entrar a la intersección con la nueva lógica de ambulancias
     * REGLAS:
     * 1. Vehículos que ya están cruzando terminan sin interrupciones
     * 2. Cuando llega ambulancia: los que no han entrado al cruce esperan
     * 3. Los vehículos delante de ambulancia tienen prioridad sobre ella
     * 4. Ambulancias se procesan una a la vez por orden de llegada
     */
    private boolean canEnterIntersectionFIFO(String vehicleId) {
        boolean isEmergency = emergencyFlag.getOrDefault(vehicleId, false);
        
        // Si no hay emergencias activas, aplicar espera post-salida normal
        if (ambulanceProcessingQueue.isEmpty() && postExitWaitTicks > 0) {
            if (!containsPriorityVehicle(intersectionQueue, vehicleId)) {
                addToPriorityQueue(intersectionQueue, vehicleId);
                System.out.println("Vehículo " + vehicleId + " agregado a cola - esperando " + postExitWaitTicks + " ticks post-salida");
            }
            return false;
        }

        // === LÓGICA SIN AMBULANCIAS (normal FIFO) ===
        if (ambulanceProcessingQueue.isEmpty()) {
            if (vehicleInIntersection == null) {
                if (intersectionQueue.isEmpty() || vehicleId.equals(peekPriorityQueue(intersectionQueue))) {
                    vehicleInIntersection = vehicleId;
                    removePriorityVehicle(intersectionQueue, vehicleId);
                    vehiclesAlreadyCrossing.add(vehicleId); // Marcar como "ya cruzando"
                    System.out.println("Vehículo " + vehicleId + " entra a la intersección (FIFO normal)");
                    return true;
                }
            } else if (vehicleId.equals(vehicleInIntersection)) {
                return true; // Ya está dentro
            }

            // Agregar a cola si no está
            if (!containsPriorityVehicle(intersectionQueue, vehicleId)) {
                addToPriorityQueue(intersectionQueue, vehicleId);
                System.out.println("Vehículo " + vehicleId + " agregado a cola intersección pos " + getQueuePosition(intersectionQueue, vehicleId));
            }
            return false;
        }

        // === LÓGICA CON AMBULANCIAS ACTIVAS ===
        String currentAmbulance = ambulanceProcessingQueue.peek();
        if (currentAmbulance == null) return false;
        
        List<String> vehiclesAheadOfCurrentAmbulance = vehiclesAheadOfAmbulance.get(currentAmbulance);
        if (vehiclesAheadOfCurrentAmbulance == null) vehiclesAheadOfCurrentAmbulance = new ArrayList<>();
        
        // 1. Si el vehículo ya está cruzando, puede continuar sin restricciones
        if (vehiclesAlreadyCrossing.contains(vehicleId)) {
            if (vehicleId.equals(vehicleInIntersection)) {
                return true; // Continúa cruzando
            }
        }
        
        // 2. Si la intersección está libre
        if (vehicleInIntersection == null) {
            // 2a. Vehículos que estaban delante de la ambulancia actual tienen prioridad
            if (vehiclesAheadOfCurrentAmbulance.contains(vehicleId)) {
                vehicleInIntersection = vehicleId;
                removePriorityVehicle(intersectionQueue, vehicleId);
                vehiclesAlreadyCrossing.add(vehicleId);
                vehiclesAheadOfCurrentAmbulance.remove(vehicleId); // Ya no está delante
                System.out.println("[EMERGENCIA] Vehículo " + vehicleId + " cruza (estaba delante de ambulancia " + currentAmbulance + ")");
                return true;
            }
            
            // 2b. Si no quedan vehículos delante, la ambulancia puede cruzar
            if (vehiclesAheadOfCurrentAmbulance.isEmpty() && vehicleId.equals(currentAmbulance)) {
                vehicleInIntersection = vehicleId;
                removePriorityVehicle(intersectionQueue, vehicleId);
                vehiclesAlreadyCrossing.add(vehicleId);
                ambulanceProcessingQueue.poll(); // Esta ambulancia ya procesada
                System.out.println("[EMERGENCIA] Ambulancia " + vehicleId + " cruza (sin vehículos delante)");
                return true;
            }
        } else if (vehicleId.equals(vehicleInIntersection)) {
            return true; // Ya está dentro
        }

        // 3. Todos los demás vehículos deben esperar
        if (!containsPriorityVehicle(intersectionQueue, vehicleId)) {
            addToPriorityQueue(intersectionQueue, vehicleId);
            if (isEmergency && !vehicleId.equals(currentAmbulance)) {
                System.out.println("[EMERGENCIA] Ambulancia " + vehicleId + " espera (otra ambulancia " + currentAmbulance + " tiene prioridad)");
            } else {
                System.out.println("Vehículo " + vehicleId + " espera (ambulancia " + currentAmbulance + " tiene prioridad)");
            }
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
                        if (!containsPriorityVehicle(waitingZoneQueue, vehicleId)) {
                            addToPriorityQueue(waitingZoneQueue, vehicleId);
                            System.out.println("Vehículo " + vehicleId + " agregado a cola de espera (posición " + 
                                              getQueuePosition(waitingZoneQueue, vehicleId) + ")");
                        }
                        return false;
                    }
                }
            }
        }
        
        // Remover de la cola de espera si puede pasar
        removePriorityVehicle(waitingZoneQueue, vehicleId);
        return true;
    }
    
    /**
     * Procesa la cola de intersección cuando se libera
     */
    private void processIntersectionQueue() {
        if (!intersectionQueue.isEmpty()) {
            String nextVehicle = peekPriorityQueue(intersectionQueue);
            System.out.println("Siguiente vehículo en cola para intersección: " + nextVehicle);
        }
    }
    
    /**
     * Obtiene la posición de un vehículo en una cola de prioridad
     */
    private int getQueuePosition(PriorityBlockingQueue<PriorityVehicle> queue, String vehicleId) {
        int position = 1;
        for (PriorityVehicle pv : queue) {
            if (pv.vehicleId.equals(vehicleId)) {
                return position;
            }
            position++;
        }
        return -1;
    }
    
    /**
     * Notifica que un vehículo salió de la intersección
     * NUEVO: Maneja la lógica de ambulancias y vehículos que terminan de cruzar
     */
    public void vehicleExitedIntersection(String vehicleId) {
        VehiclePosition pos = vehiclePositions.get(vehicleId);
        if (pos != null && !isInIntersectionZone(pos.x, pos.y)) {
            if (vehicleId.equals(vehicleInIntersection)) {
                vehicleInIntersection = null;
                
                // NUEVO: El vehículo terminó de cruzar, ya no está en la ecuación
                vehiclesAlreadyCrossing.remove(vehicleId);
                
                // NUEVO: Marcar como vehículo post-intersección (completamente libre)
                vehiclesPostIntersection.add(vehicleId);
                System.out.println("Vehículo " + vehicleId + " ahora es libre post-intersección");
                
                // Si no hay ambulancias activas, aplicar espera normal
                if (ambulanceProcessingQueue.isEmpty()) {
                    postExitWaitTicks = POST_EXIT_WAIT_DURATION;
                    System.out.println("Vehículo " + vehicleId + " salió - espera " + POST_EXIT_WAIT_DURATION + " ticks");
                } else {
                    // Con ambulancias activas, no hay espera - procesamiento inmediato
                    postExitWaitTicks = 0;
                    System.out.println("Vehículo " + vehicleId + " salió - procesando siguiente inmediatamente (ambulancia activa)");
                }
                
                processIntersectionQueue();
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
        
        // NUEVO: Limpiar estructuras de ambulancias
        vehiclesAlreadyCrossing.clear();
        vehiclesPostIntersection.clear(); // NUEVO: Limpiar vehículos post-intersección
        vehiclesAheadOfAmbulance.clear();
        ambulanceProcessingQueue.clear();
        
        System.out.println("Intersección liberada forzosamente - sistema de ambulancias reiniciado");
    }
    
    public boolean isVehicleWaiting(String vehicleId) {
        return containsPriorityVehicle(intersectionQueue, vehicleId) || 
               containsPriorityVehicle(waitingZoneQueue, vehicleId);
    }
    
    public Set<String> getVehiclesInIntersection() {
        Set<String> result = new HashSet<>();
        if (vehicleInIntersection != null) {
            result.add(vehicleInIntersection);
        }
        return result;
    }
    
    public Set<String> getVehiclesInWaitingZone() {
        Set<String> result = new HashSet<>();
        for (PriorityVehicle pv : waitingZoneQueue) {
            result.add(pv.vehicleId);
        }
        return result;
    }
    
    // Métodos auxiliares para trabajar con PriorityVehicle
    private boolean containsPriorityVehicle(PriorityBlockingQueue<PriorityVehicle> queue, String vehicleId) {
        return queue.stream().anyMatch(pv -> pv.vehicleId.equals(vehicleId));
    }
    
    private void addToPriorityQueue(PriorityBlockingQueue<PriorityVehicle> queue, String vehicleId) {
        if (!containsPriorityVehicle(queue, vehicleId)) {
            boolean isEmergency = emergencyFlag.getOrDefault(vehicleId, false);
            long arrivalTime = vehicleArrivalTime.getOrDefault(vehicleId, System.currentTimeMillis());
            List<String> vehiclesAheadList = vehiclesAheadOfAmbulance.getOrDefault(vehicleId, new ArrayList<>());
            boolean hasVehiclesAhead = !vehiclesAheadList.isEmpty();
            
            queue.add(new PriorityVehicle(vehicleId, arrivalTime, isEmergency, hasVehiclesAhead));
        }
    }
    
    private String peekPriorityQueue(PriorityBlockingQueue<PriorityVehicle> queue) {
        PriorityVehicle pv = queue.peek();
        return pv != null ? pv.vehicleId : null;
    }
    
    private void removePriorityVehicle(PriorityBlockingQueue<PriorityVehicle> queue, String vehicleId) {
        queue.removeIf(pv -> pv.vehicleId.equals(vehicleId));
    }
    
    private String pollPriorityQueue(PriorityBlockingQueue<PriorityVehicle> queue) {
        PriorityVehicle pv = queue.poll();
        return pv != null ? pv.vehicleId : null;
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
     * Debug del estado con información de colas y ambulancias
     */
    public void printStatus() {
        System.out.println("=== COLLISION MANAGER STATUS (AMBULANCIAS MEJORADAS) ===");
        System.out.println("Vehículo en intersección: " + vehicleInIntersection);
        System.out.println("Cola de intersección (" + intersectionQueue.size() + "): " + intersectionQueue);
        System.out.println("Cola de zona de espera (" + waitingZoneQueue.size() + "): " + waitingZoneQueue);
        System.out.println("Vehículos tracked: " + vehiclePositions.size());
        
        // NUEVO: Información de ambulancias
        if (!ambulanceProcessingQueue.isEmpty()) {
            System.out.println("Ambulancias en cola: " + ambulanceProcessingQueue);
            String currentAmb = ambulanceProcessingQueue.peek();
            if (currentAmb != null) {
                List<String> ahead = vehiclesAheadOfAmbulance.get(currentAmb);
                System.out.println("Vehículos delante de " + currentAmb + ": " + (ahead != null ? ahead : "[]"));
            }
        }
        
        if (!vehiclesAlreadyCrossing.isEmpty()) {
            System.out.println("Vehículos ya cruzando (no se pueden interrumpir): " + vehiclesAlreadyCrossing);
        }
        
        if (!vehiclesPostIntersection.isEmpty()) {
            System.out.println("Vehículos post-intersección (completamente libres): " + vehiclesPostIntersection);
        }
        
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
            boolean isEmergency = emergencyFlag.getOrDefault(entry.getKey(), false);
            boolean alreadyCrossing = vehiclesAlreadyCrossing.contains(entry.getKey());
            boolean postIntersection = vehiclesPostIntersection.contains(entry.getKey());
            
            System.out.println("  " + entry.getKey() + ": " + pos + 
                             " [Llegada: " + arrival + "]" +
                             (inIntersection ? " [INTERSECCIÓN]" : "") +
                             (inWaiting ? " [ESPERA]" : "") +
                             (isEmergency ? " [AMBULANCIA]" : "") +
                             (alreadyCrossing ? " [CRUZANDO]" : "") +
                             (postIntersection ? " [LIBRE]" : ""));
        }
        
        // Mostrar colas por spawn
        for (Map.Entry<String, PriorityBlockingQueue<PriorityVehicle>> spawnEntry : spawnQueues.entrySet()) {
            if (!spawnEntry.getValue().isEmpty()) {
                List<String> vehicleIds = new ArrayList<>();
                for (PriorityVehicle pv : spawnEntry.getValue()) {
                    vehicleIds.add(pv.vehicleId);
                }
                System.out.println("Cola spawn " + spawnEntry.getKey() + ": " + vehicleIds);
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
