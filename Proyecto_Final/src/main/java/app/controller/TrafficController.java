package app.controller;

import app.model.Intersection;
import app.model.OccupiedLane;
import app.model.TrafficLight;
import app.model.Vehicle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controlador de tráfico que gestiona intersecciones y carriles ocupados.
 * Utiliza una estrategia de peek() antes de poll() para validar movimientos seguros
 * y mantiene un registro de carriles ocupados con liberación automática.
 */
public class TrafficController {
    // Componentes principales del sistema
    private final List<Intersection> intersections;
    private final List<TrafficLight> trafficLights;
    private final IntersectionManager manager = new IntersectionManager();
    private final ScheduledExecutorService scheduler;
    
    // Referencia a la vista para verificar el estado de animaciones
    private final app.ui.IntersectionView intersectionView;
    
    // === GESTIÓN DE CARRILES OCUPADOS ===
    /**
     * Mapa que mantiene los carriles ocupados por cada intersección.
     * Es thread-safe para manejar acceso concurrente desde múltiples hilos.
     * Key: Intersección, Value: Set de carriles ocupados en esa intersección
     */
    private final Map<Intersection, Set<OccupiedLane>> occupiedLanes;
    
    /**
     * Tiempo en segundos que un vehículo ocupa un carril antes de liberarlo automáticamente.
     * Este valor simula el tiempo que toma a un vehículo cruzar completamente la intersección.
     */
    private static final int LANE_OCCUPATION_TIME_SECONDS = 5;

    public TrafficController(List<Intersection> intersections,
                             List<TrafficLight> trafficLights,
                             app.ui.IntersectionView intersectionView) {
        this.intersections = intersections;
        this.trafficLights = trafficLights;
        this.intersectionView = intersectionView;
        this.scheduler = Executors.newScheduledThreadPool(10);
        
        // Inicializar estructura de carriles ocupados - thread-safe
        this.occupiedLanes = new ConcurrentHashMap<>();
        
        // Inicializar sets vacíos para cada intersección
        for (Intersection intersection : intersections) {
            occupiedLanes.put(intersection, ConcurrentHashMap.newKeySet());
        }
    }
    
    // Constructor original mantenido para compatibilidad
    public TrafficController(List<Intersection> intersections,
                             List<TrafficLight> trafficLights) {
        this(intersections, trafficLights, null);
    }

    /**
     * Inicia el control de tráfico con tareas programadas.
     */
    public void startControl() {
        // TODO: Descomentar cuando TrafficLight.changeLight() esté implementado
        for(TrafficLight trafficLight : trafficLights) {
            // scheduler.scheduleAtFixedRate(trafficLight::changeLight, 0, 60, TimeUnit.SECONDS);
        }
        
        // Gestión principal de intersecciones - cada segundo
        scheduler.scheduleAtFixedRate(this::manageIntersections, 0, 1, TimeUnit.SECONDS);
        
        // Liberación automática de carriles ocupados - cada 2 segundos
        scheduler.scheduleAtFixedRate(this::cleanupOccupiedLanes, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * ALGORITMO PRINCIPAL DE GESTIÓN DE INTERSECCIONES
     * 
     * Para cada intersección:
     * 1. Usa peek() para obtener vehículos sin extraerlos de la cola
     * 2. Valida seguridad con IntersectionManager
     * 3. Solo si es seguro: usa poll() y marca carril como ocupado
     * 4. Procesa un vehículo por iteración para evitar saturación
     */
    private void manageIntersections() {
        for (Intersection intersection : intersections) {
            processIntersectionSafely(intersection);
        }
    }
    
    /**
     * Procesa una intersección individual de forma segura.
     * Implementa la estrategia peek-validate-poll.
     * 
     * @param intersection Intersección a procesar
     */
    private void processIntersectionSafely(Intersection intersection) {
        // === VERIFICAR SI HAY ANIMACIONES EN PROGRESO ===
        if (intersectionView != null && intersectionView.isCrossingAnimationInProgress()) {
            // Hay una animación de cruce en progreso, esperar a que termine
            return;
        }
        
        // === PASO 1: PEEK - Obtener vehículos sin extraerlos ===
        List<Vehicle> queuedVehicles = intersection.peekAllVehicles();
        
        if (queuedVehicles.isEmpty()) {
            return; // No hay vehículos esperando
        }
        
        // === PASO 2: BUSCAR VEHÍCULO SEGURO PARA PROCESAR ===
        for (Vehicle candidateVehicle : queuedVehicles) {
            // Verificar que el vehículo no esté ya en la intersección
            if (candidateVehicle.isInIntersection()) {
                continue; // Ya está procesándose
            }
            
            // === VERIFICAR QUE EL VEHÍCULO ESTÉ LISTO PARA CRUZAR ===
            // Solo procesar vehículos que han llegado a la línea de parada
            if (!candidateVehicle.isReadyToCross()) {
                continue; // Todavía está aproximándose a la intersección
            }
            
            // === PASO 3: VALIDAR SEGURIDAD CON INTERSECTION MANAGER ===
            if (isVehicleMovementSafe(candidateVehicle, queuedVehicles, intersection)) {
                // === PASO 4: POLL Y PROCESAR VEHÍCULO SEGURO ===
                processVehicleSafely(candidateVehicle, intersection);
                break; // Solo procesar un vehículo por ciclo para mantener control
            }
        }
    }
    
    /**
     * Valida si un vehículo puede moverse de forma segura.
     * Considera tanto conflictos de movimiento como carriles ocupados.
     * 
     * @param vehicle Vehículo a validar
     * @param otherVehicles Otros vehículos en la cola
     * @param intersection Intersección donde se mueve el vehículo
     * @return true si el movimiento es seguro
     */
    private boolean isVehicleMovementSafe(Vehicle vehicle, List<Vehicle> otherVehicles, 
                                        Intersection intersection) {
        // Validar conflictos con IntersectionManager
        boolean noMovementConflicts = manager.isSafeToProceed(vehicle, otherVehicles);
        
        // Validar que el carril de destino no esté ocupado
        boolean laneNotOccupied = !isLaneOccupied(vehicle, intersection);
        
        return noMovementConflicts && laneNotOccupied;
    }
    
    /**
     * Verifica si el carril que usaría un vehículo está actualmente ocupado.
     * 
     * @param vehicle Vehículo a verificar
     * @param intersection Intersección donde se mueve
     * @return true si el carril está ocupado
     */
    private boolean isLaneOccupied(Vehicle vehicle, Intersection intersection) {
        Set<OccupiedLane> lanes = occupiedLanes.get(intersection);
        String targetLaneId = vehicle.getMovement().getEntry() + "_" + vehicle.getMovement().getTurn();
        
        return lanes.stream()
                   .anyMatch(lane -> lane.getLaneId().equals(targetLaneId));
    }
    
    /**
     * Procesa un vehículo que ha sido validado como seguro.
     * Realiza poll() de la cola y marca el carril como ocupado.
     * 
     * @param vehicle Vehículo a procesar
     * @param intersection Intersección donde se procesa
     */
    private void processVehicleSafely(Vehicle vehicle, Intersection intersection) {
        try {
            // === EXTRAER VEHÍCULO DE LA COLA ===
            intersection.removeVehicle(vehicle);
            
            // === MARCAR VEHÍCULO COMO EN INTERSECCIÓN ===
            vehicle.setInIntersection(true);
            
            // === MARCAR CARRIL COMO OCUPADO ===
            OccupiedLane occupiedLane = new OccupiedLane(vehicle, vehicle.getMovement());
            occupiedLanes.get(intersection).add(occupiedLane);
            
            // === PROGRAMAR LIBERACIÓN AUTOMÁTICA DEL CARRIL ===
            scheduleAutomaticLaneRelease(occupiedLane, intersection);
            
            // Log para debugging
            System.out.println("✓ Vehículo " + vehicle.getId() + " procesado en " + 
                             intersection.getId() + " - Carril: " + occupiedLane.getLaneId());
                             
        } catch (Exception e) {
            System.err.println("✗ Error procesando vehículo " + vehicle.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Programa la liberación automática de un carril después del tiempo especificado.
     * 
     * @param occupiedLane Carril ocupado a liberar
     * @param intersection Intersección donde está el carril
     */
    private void scheduleAutomaticLaneRelease(OccupiedLane occupiedLane, Intersection intersection) {
        scheduler.schedule(() -> {
            releaseLane(occupiedLane, intersection);
        }, LANE_OCCUPATION_TIME_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Libera un carril específico, marcándolo como disponible.
     * 
     * @param occupiedLane Carril a liberar
     * @param intersection Intersección donde está el carril
     */
    private void releaseLane(OccupiedLane occupiedLane, Intersection intersection) {
        Set<OccupiedLane> lanes = occupiedLanes.get(intersection);
        boolean removed = lanes.remove(occupiedLane);
        
        if (removed) {
            // Marcar vehículo como fuera de la intersección
            occupiedLane.getVehicle().setInIntersection(false);
            
            System.out.println("🚦 Carril liberado: " + occupiedLane.getLaneId() + 
                             " - Vehículo: " + occupiedLane.getVehicle().getId());
        }
    }
    
    /**
     * Tarea de limpieza que libera carriles que han estado ocupados por mucho tiempo.
     * Actúa como un mecanismo de respaldo para evitar carriles permanentemente bloqueados.
     */
    private void cleanupOccupiedLanes() {
        for (Map.Entry<Intersection, Set<OccupiedLane>> entry : occupiedLanes.entrySet()) {
            Intersection intersection = entry.getKey();
            Set<OccupiedLane> lanes = entry.getValue();
            
            // Crear lista de carriles a liberar para evitar modificación concurrente
            List<OccupiedLane> lanesToRelease = new ArrayList<>();
            
            for (OccupiedLane lane : lanes) {
                // Liberar carriles que han estado ocupados más tiempo del esperado
                if (lane.hasBeenOccupiedFor(LANE_OCCUPATION_TIME_SECONDS + 2)) {
                    lanesToRelease.add(lane);
                }
            }
            
            // Liberar carriles identificados
            for (OccupiedLane lane : lanesToRelease) {
                releaseLane(lane, intersection);
                System.out.println("🧹 Limpieza automática: Carril " + lane.getLaneId() + 
                                 " liberado por timeout");
            }
        }
    }
    
    /**
     * Método público para obtener información de carriles ocupados (para debugging/monitoring).
     * 
     * @param intersection Intersección a consultar
     * @return Set de carriles ocupados (copia defensiva)
     */
    public Set<OccupiedLane> getOccupiedLanes(Intersection intersection) {
        Set<OccupiedLane> lanes = occupiedLanes.get(intersection);
        return lanes != null ? new HashSet<>(lanes) : Collections.emptySet();
    }
    
    /**
     * Detiene el control de tráfico y libera recursos.
     */
    public void stopControl() {
        System.out.println("🛑 Deteniendo control de tráfico...");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                System.out.println("⚠️  Timeout: Forzando cierre del scheduler");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Limpiar carriles ocupados
        occupiedLanes.clear();
        System.out.println("✓ Control de tráfico detenido exitosamente");
    }
}
