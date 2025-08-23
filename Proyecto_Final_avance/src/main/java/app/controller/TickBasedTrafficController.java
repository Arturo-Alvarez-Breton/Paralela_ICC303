package app.controller;

import app.model.*;
import app.model.VehicleState;
import app.enums.DirectionEnum;
import app.enums.VehicleTypeEnum;

import java.util.*;
import java.util.concurrent.*;

/**
 * Controlador principal basado en ticks que maneja todo el sistema de tráfico
 * Implementa:
 * - Loop principal con ticks constantes
 * - Estados de vehículos ya en movimiento
 * - Validación de múltiples vehículos simultáneos
 * - Velocidad constante por ticks
 * - Detección y prevención de colisiones
 */
public class TickBasedTrafficController {
    
    // === CONFIGURACIÓN DEL SISTEMA ===
    private static final int TICKS_PER_SECOND = 30; // 30 FPS
    private static final long TICK_DURATION_MS = 1000 / TICKS_PER_SECOND;
    // private static final long MAX_WAITING_TICKS = TICKS_PER_SECOND * 10; // 10 segundos máximo esperando - Reservado para uso futuro
    
    // === COMPONENTES DEL SISTEMA ===
    private ScheduledExecutorService tickScheduler;
    private final List<TickBasedVehicle> activeVehicles;
    private final Map<String, Queue<TickBasedVehicle>> entryQueues; // Colas por punto de entrada
    // private final IntersectionManager intersectionManager; // Reservado para uso futuro en validaciones avanzadas
    
    // === ESTADO DEL SISTEMA ===
    private volatile boolean running;
    private long currentTick;
    private int vehicleIdCounter;
    
    // === CALLBACKS PARA UI ===
    private VehicleUpdateCallback vehicleUpdateCallback;
    private LogCallback logCallback;
    
    // === ESTADÍSTICAS ===
    private int totalVehiclesProcessed;
    private int emergencyVehiclesProcessed;
    private long totalWaitingTime;
    
    public TickBasedTrafficController() {
        this.activeVehicles = new CopyOnWriteArrayList<>();
        this.entryQueues = new ConcurrentHashMap<>();
        // this.intersectionManager = new IntersectionManager(); // Comentado - reservado para uso futuro
        
        // Inicializar colas para cada punto de entrada
        for (Movement.Entry entry : Movement.Entry.values()) {
            entryQueues.put(entry.name().toLowerCase(), new ConcurrentLinkedQueue<>());
        }
        
        this.running = false;
        this.currentTick = 0;
        this.vehicleIdCounter = 1;
        this.totalVehiclesProcessed = 0;
        this.emergencyVehiclesProcessed = 0;
        this.totalWaitingTime = 0;
        
        // Crear el primer executor
        createNewExecutor();
    }
    
    /**
     * Crea un nuevo ScheduledExecutorService
     */
    private void createNewExecutor() {
        this.tickScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TickController-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    // === INTERFACES PARA CALLBACKS ===
    
    @FunctionalInterface
    public interface VehicleUpdateCallback {
        void onVehicleUpdate(String vehicleId, Position position, VehicleState state);
    }
    
    @FunctionalInterface
    public interface LogCallback {
        void onLog(String message);
    }
    
    // === CONTROL DEL SISTEMA ===
    
    /**
     * Inicia el loop principal del sistema basado en ticks
     */
    public void start() {
        if (running) return;
        
        // Verificar si necesitamos crear un nuevo executor
        if (tickScheduler.isShutdown() || tickScheduler.isTerminated()) {
            log("🔄 Recreando executor terminado...");
            createNewExecutor();
        }
        
        running = true;
        currentTick = 0;
        
        log("🚦 Sistema de tráfico basado en ticks iniciado (" + TICKS_PER_SECOND + " FPS)");
        
        // DEBUG: Mostrar coordenadas del área de intersección para verificar corrección
        IntersectionCoordinates.debugIntersectionArea();
        
        try {
            // Iniciar el loop principal
            tickScheduler.scheduleAtFixedRate(this::mainTickLoop, 0, TICK_DURATION_MS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            log("⚠️ Error iniciando scheduler, recreando executor...");
            createNewExecutor();
            tickScheduler.scheduleAtFixedRate(this::mainTickLoop, 0, TICK_DURATION_MS, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Detiene el sistema
     */
    public void stop() {
        if (!running) return;
        
        running = false;
        tickScheduler.shutdown();
        
        log("🛑 Sistema de tráfico detenido");
        
        try {
            if (!tickScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                tickScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            tickScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // === LOOP PRINCIPAL ===
    
    /**
     * Loop principal ejecutado en cada tick - OPTIMIZADO para mejor rendimiento
     */
    private void mainTickLoop() {
        try {
            currentTick++;
            
            // 1. SIEMPRE: Actualizar todos los vehículos activos (crítico para movimiento)
            updateAllVehicles();
            
            // 2. CADA TICK: Operaciones de seguridad CRÍTICAS (autorización debe ser inmediata)
            handleCollisions();
            processAuthorizationRequests();
            promoteAuthorizedVehicles(); // NUEVO: Promover vehículos autorizados a CROSSING de manera segura
            
            // 3. CADA 5 TICKS (~6 veces por segundo): Operaciones de gestión
            if (currentTick % 5 == 0) {
                processEmergencyPriority();
                cleanupCompletedVehicles();
            }
            
            // 4. CADA 10 TICKS (~3 veces por segundo): Estadísticas
            if (currentTick % 10 == 0) {
                updateStatistics();
            }
            
            // 5. CADA SEGUNDO: Reportes
            if (currentTick % TICKS_PER_SECOND == 0) {
                logSystemStatus();
            }
            
        } catch (Exception e) {
            // Logging de errores simplificado para mejor rendimiento
            log("❌ Error en tick " + currentTick + ": " + e.getClass().getSimpleName());
            
            // Solo logging detallado cada 30 ticks para evitar spam
            if (currentTick % TICKS_PER_SECOND == 0) {
                log("🔍 Detalles del error: " + e.getMessage());
                if (e.getStackTrace().length > 0) {
                    log("📍 En: " + e.getStackTrace()[0].getClassName() + "." + e.getStackTrace()[0].getMethodName() + ":" + e.getStackTrace()[0].getLineNumber());
                }
                log("📊 Estado actual: " + activeVehicles.size() + " vehículos activos");
            }
        }
    }
    
    // === ACTUALIZACIÓN DE VEHÍCULOS ===
    
    /**
     * Actualiza todos los vehículos activos en el sistema de manera segura
     */
    private void updateAllVehicles() {
        // Crear una copia para evitar ConcurrentModificationException
        List<TickBasedVehicle> vehiclesCopy = new ArrayList<>(activeVehicles);
        
        for (TickBasedVehicle vehicle : vehiclesCopy) {
            try {
                // Verificar que el vehículo no sea null
                if (vehicle == null) continue;
                
                boolean stillActive = vehicle.updateTick();
                
                // Notificar cambios de posición a la UI (con verificaciones de null)
                if (vehicleUpdateCallback != null && vehicle.getCurrentPosition() != null) {
                    vehicleUpdateCallback.onVehicleUpdate(
                        vehicle.getId(), 
                        vehicle.getCurrentPosition(), 
                        vehicle.getState()
                    );
                }
                
                // Si el vehículo ya no está activo, marcarlo para limpieza
                if (!stillActive && vehicle.getState() == VehicleState.COMPLETED) {
                    // La limpieza se hace en cleanupCompletedVehicles()
                }
                
            } catch (Exception e) {
                log("⚠️ Error actualizando vehículo " + 
                    (vehicle != null ? vehicle.getId() : "null") + ": " + e.getMessage());
                
                // Si hay error con este vehículo, removerlo para evitar problemas futuros
                if (vehicle != null) {
                    activeVehicles.remove(vehicle);
                }
            }
        }
    }
    
    // === DETECCIÓN Y MANEJO DE COLISIONES ===
    
    /**
     * Detecta y maneja colisiones entre vehículos de manera simple y efectiva
     */
    private void handleCollisions() {
        try {
            Set<String> collisions = CollisionDetector.detectCurrentCollisions(activeVehicles);
            
            if (!collisions.isEmpty()) {
                log("🚨 COLISIÓN DETECTADA (" + collisions.size() + " colisiones) - Resolviendo automáticamente...");
                
                // Estrategia simple: remover inmediatamente los vehículos en colisión
                Set<String> vehiclesToRemove = new HashSet<>();
                
                for (String collision : collisions) {
                    String[] vehicleIds = collision.split("-");
                    if (vehicleIds.length == 2) {
                        vehiclesToRemove.add(vehicleIds[0]);
                        vehiclesToRemove.add(vehicleIds[1]);
                        log("💥 Colisión entre " + vehicleIds[0] + " y " + vehicleIds[1] + " - Marcados para remoción");
                    }
                }
                
                log("🧹 Removiendo " + vehiclesToRemove.size() + " vehículos involucrados en colisiones...");
                
                // Remover vehículos en colisión de manera segura
                removeVehiclesSafely(vehiclesToRemove);
                
                log("✅ Sistema de colisiones: " + vehiclesToRemove.size() + " vehículos removidos - Intersección despejada");
            }
        } catch (Exception e) {
            log("⚠️ Error manejando colisiones: " + e.getMessage() + " - Continuando...");
            // No rethrow la excepción para evitar romper el loop principal
        }
    }
    
    /**
     * Remueve vehículos de manera segura del sistema
     */
    private void removeVehiclesSafely(Set<String> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) return;
        
        try {
            // Encontrar y marcar vehículos para remoción
            List<TickBasedVehicle> vehiclesToRemove = new ArrayList<>();
            
            for (TickBasedVehicle vehicle : activeVehicles) {
                if (vehicle != null && vehicleIds.contains(vehicle.getId())) {
                    vehiclesToRemove.add(vehicle);
                }
            }
            
            // Remover de todas las estructuras
            for (TickBasedVehicle vehicle : vehiclesToRemove) {
                if (vehicle != null) {
                    // Remover de vehículos activos
                    activeVehicles.remove(vehicle);
                    
                    // Remover de la cola de entrada correspondiente
                    String entryPointStr = vehicle.getEntryPoint().name().toLowerCase();
                    Queue<TickBasedVehicle> entryQueue = entryQueues.get(entryPointStr);
                    if (entryQueue != null) {
                        entryQueue.remove(vehicle);
                    }
                    
                    // Notificar a la UI para remover la vista visual
                    if (vehicleUpdateCallback != null) {
                        vehicleUpdateCallback.onVehicleUpdate(
                            vehicle.getId(),
                            vehicle.getCurrentPosition(),
                            VehicleState.COMPLETED
                        );
                    }
                    
                    log("🗑️ Vehículo " + vehicle.getId() + " removido por colisión");
                }
            }
            
            // Actualizar posiciones en TODAS las colas después de remoción
            updateAllQueuePositions();
            
        } catch (Exception e) {
            log("⚠️ Error removiendo vehículos: " + e.getMessage());
        }
    }
    
    // === AUTORIZACIÓN DE CRUCES ===
    
    /**
     * Procesa las solicitudes de autorización para cruzar la intersección
     * CORREGIDO: VERIFICACIÓN FÍSICA DE INTERSECCIÓN - Solo UN vehículo físicamente presente
     */
    private void processAuthorizationRequests() {
        // NUEVA LÓGICA: Verificar si hay vehículos FÍSICAMENTE en la intersección
        boolean intersectionOccupied = false;
        String occupyingVehicleId = null;
        
        for (TickBasedVehicle vehicle : activeVehicles) {
            if (vehicle != null && vehicle.getCurrentPosition() != null) {
                // Verificar posición FÍSICA en lugar de estado
                if (IntersectionCoordinates.isVehicleInIntersection(vehicle.getCurrentPosition())) {
                    intersectionOccupied = true;
                    occupyingVehicleId = vehicle.getId();
                    break; // Solo necesitamos saber si hay al menos uno en la intersección
                }
            }
        }
        
        // REGLA FÍSICA ESTRICTA: Solo autorizar si la intersección está FÍSICAMENTE libre
        if (intersectionOccupied) {
            // CRÍTICO: Revocar autorizaciones si alguien ya está en la intersección
            revokeAllAuthorizations("Intersección ocupada por " + occupyingVehicleId);
            
            // Debug log cada 2 segundos
            if (currentTick % (TICKS_PER_SECOND * 2) == 0) {
                log("🚧 Intersección ocupada por vehículo " + occupyingVehicleId + " - esperando...");
            }
            return; // Esperar a que la intersección esté físicamente libre
        }
        
        // Encontrar el vehículo esperando que llegó PRIMERO (FIFO global)
        TickBasedVehicle nextToAuthorize = null;
        
        for (TickBasedVehicle vehicle : activeVehicles) {
            if (vehicle != null && 
                vehicle.getState() == VehicleState.WAITING && 
                !vehicle.isAuthorizedToCross()) {
                
                // Si no tenemos candidato O este llegó antes
                if (nextToAuthorize == null || 
                    compareVehiclePriority(vehicle, nextToAuthorize) < 0) {
                    
                    // Verificar que sea el primero en su dirección
                    String entryPoint = vehicle.getEntryPoint().name().toLowerCase();
                    if (isFirstInDirectionQueue(vehicle, entryPoint)) {
                        nextToAuthorize = vehicle;
                    }
                }
            }
        }
        
        // Autorizar solo UN vehículo por tick (FIFO estricto)
        if (nextToAuthorize != null) {
            // NUEVA VERIFICACIÓN: Asegurar que NO hay vehículos en estado CROSSING
            int vehiclesCrossing = 0;
            for (TickBasedVehicle v : activeVehicles) {
                if (v != null && v.getState() == VehicleState.CROSSING) {
                    vehiclesCrossing++;
                }
            }
            
            if (vehiclesCrossing > 0) {
                log("🚫 NO autorizar: " + vehiclesCrossing + " vehículo(s) ya en estado CROSSING");
                return; // BLOQUEAR autorización si alguien ya está cruzando
            }
            
            if (CollisionDetector.isSafeToAuthorize(nextToAuthorize, activeVehicles)) {
                nextToAuthorize.setAuthorizedToCross(true);
                String entryPoint = nextToAuthorize.getEntryPoint().name().toLowerCase();
                
                // Log detallado para mostrar el comportamiento FIFO
                log("✅ Vehículo " + nextToAuthorize.getId() + " (" + nextToAuthorize.getType() + 
                    ") autorizado para cruzar desde " + entryPoint + " (FIFO orden: " + nextToAuthorize.getArrivalOrder() + ")");
                
                // Debug: mostrar otros vehículos esperando (solo cada 5 segundos para evitar spam)
                if (currentTick % (TICKS_PER_SECOND * 5) == 0) {
                    logWaitingVehicles();
                }
            } else {
                // Log cuando no es seguro autorizar
                if (currentTick % (TICKS_PER_SECOND * 2) == 0) {
                    log("⏳ Vehículo " + nextToAuthorize.getId() + " debe esperar (no es seguro autorizar)");
                }
            }
        }
    }
    
    /**
     * NUEVO: Promover vehículos autorizados a CROSSING de manera segura
     * Solo permite UN vehículo en estado CROSSING a la vez
     */
    private void promoteAuthorizedVehicles() {
        try {
            // 1. Verificar si ya hay vehículos en estado CROSSING
            int vehiclesCrossing = 0;
            for (TickBasedVehicle vehicle : activeVehicles) {
                if (vehicle != null && vehicle.getState() == VehicleState.CROSSING) {
                    vehiclesCrossing++;
                }
            }
            
            if (vehiclesCrossing > 0) {
                // Ya hay vehículo(s) cruzando - NO promover más
                return;
            }
            
            // 2. Buscar UN vehículo autorizado listo para promover
            TickBasedVehicle toPromote = null;
            for (TickBasedVehicle vehicle : activeVehicles) {
                if (vehicle != null && 
                    vehicle.getState() == VehicleState.WAITING && 
                    vehicle.isAuthorizedToCross()) {
                    
                    // Usar el mismo criterio de prioridad (arrivalOrder)
                    if (toPromote == null || vehicle.getArrivalOrder() < toPromote.getArrivalOrder()) {
                        toPromote = vehicle;
                    }
                }
            }
            
            // 3. Promover solo UN vehículo
            if (toPromote != null) {
                if (toPromote.promoteToTraversing()) {
                    log("🎯 Vehículo " + toPromote.getId() + " promovido a CROSSING (único en intersección)");
                } else {
                    log("❌ Error promoviendo vehículo " + toPromote.getId() + " a CROSSING");
                    // Revocar autorización si falla la promoción
                    toPromote.setAuthorizedToCross(false);
                }
            }
            
        } catch (Exception e) {
            log("❌ Error en promoteAuthorizedVehicles: " + e.getMessage());
        }
    }
    
    /**
     * CRÍTICO: Revoca TODAS las autorizaciones pendientes si hay conflicto
     * Evita que múltiples vehículos entren simultáneamente
     */
    private void revokeAllAuthorizations(String reason) {
        int revokedCount = 0;
        for (TickBasedVehicle vehicle : activeVehicles) {
            if (vehicle != null && vehicle.isAuthorizedToCross() && vehicle.getState() == VehicleState.WAITING) {
                vehicle.setAuthorizedToCross(false);
                revokedCount++;
            }
        }
        
        if (revokedCount > 0) {
            log("🚫 " + revokedCount + " autorizaciones revocadas: " + reason);
        }
    }
    
    /**
     * Verifica si un vehículo es realmente el primero en la cola de su dirección
     * OPTIMIZADO: Evita streams para mejor rendimiento
     */
    private boolean isFirstInDirectionQueue(TickBasedVehicle vehicle, String entryPoint) {
        try {
            // Contar cuántos vehículos están delante de este en la misma dirección
            int vehiclesAhead = 0;
            
            for (TickBasedVehicle v : activeVehicles) {
                if (v != null && v != vehicle &&
                    v.getEntryPoint().name().toLowerCase().equals(entryPoint) &&
                    (v.getState() == VehicleState.WAITING || v.getState() == VehicleState.APPROACHING) &&
                    compareVehiclePriority(v, vehicle) < 0) {
                    vehiclesAhead++;
                }
            }
            
            return vehiclesAhead == 0; // Es el primero si no hay vehículos delante
            
        } catch (Exception e) {
            log("⚠️ Error verificando posición en cola: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Comparador de prioridad de vehículos
     * 1. Emergencias primero
     * 2. Menor tiempo de arribo (FIFO)
     */
    private int compareVehiclePriority(TickBasedVehicle v1, TickBasedVehicle v2) {
        // Prioridad por tipo
        if (v1.isEmergency() && !v2.isEmergency()) return -1;
        if (!v1.isEmergency() && v2.isEmergency()) return 1;
        
        // Si son del mismo tipo, por orden de llegada
        return Long.compare(v1.getArrivalOrder(), v2.getArrivalOrder());
    }
    
    // === REGLA ESPECÍFICA DE EMERGENCIAS ===
    
    /**
     * Implementa la regla específica: para que un vehículo de emergencia avance,
     * todos los que están delante deben avanzar primero
     */
    private void processEmergencyPriority() {
        List<TickBasedVehicle> emergencyVehicles = activeVehicles.stream()
            .filter(TickBasedVehicle::isEmergency)
            .filter(v -> v.getState() == VehicleState.WAITING)
            .toList();
        
        for (TickBasedVehicle emergency : emergencyVehicles) {
            List<TickBasedVehicle> blockingVehicles = 
                CollisionDetector.findVehiclesBlockingEmergency(emergency, activeVehicles);
            
            if (!blockingVehicles.isEmpty()) {
                log("🚑 Vehículo de emergencia " + emergency.getId() + 
                    " bloqueado por " + blockingVehicles.size() + " vehículos. Despejando...");
                
                // Autorizar a los vehículos bloqueantes para que despejen el camino
                for (TickBasedVehicle blocking : blockingVehicles) {
                    if (CollisionDetector.isSafeToAuthorize(blocking, activeVehicles)) {
                        blocking.setAuthorizedToCross(true);
                        log("🔀 Despejando vehículo " + blocking.getId() + " para emergencia");
                    }
                }
            }
        }
    }
    
    // === GESTIÓN DE VEHÍCULOS ===
    
    /**
     * Agrega un nuevo vehículo al sistema
     */
    public String addVehicle(String entryPoint, DirectionEnum direction, VehicleTypeEnum type) {
        String vehicleId = "v" + (vehicleIdCounter++);
        
        try {
            Movement.Entry entryEnum = Movement.Entry.valueOf(entryPoint.toUpperCase());
            
            TickBasedVehicle vehicle = new TickBasedVehicle(vehicleId, type, direction, entryEnum);
            
            // Calcular posición en la cola considerando TODOS los vehículos en esa dirección
            Queue<TickBasedVehicle> entryQueue = entryQueues.get(entryPoint);
            
            // Contar vehículos activos en la misma dirección de manera optimizada
            int vehiclesInSameDirection = 0;
            for (TickBasedVehicle v : activeVehicles) {
                if (v != null && 
                    v.getEntryPoint().name().toLowerCase().equals(entryPoint) &&
                    v.getState() != VehicleState.COMPLETED) {
                    vehiclesInSameDirection++;
                }
            }
            
            int queuePosition = vehiclesInSameDirection; // Nueva posición basada en vehículos activos
            vehicle.updateQueuePosition(queuePosition);
            
            // Agregar a la cola y a vehículos activos
            entryQueue.offer(vehicle);
            activeVehicles.add(vehicle);
            
            log("🚗 Nuevo vehículo " + vehicleId + " (" + type + ") desde " + entryPoint + 
                " hacia " + direction + " (pos. cola: " + queuePosition + ")");
            
            return vehicleId;
            
        } catch (IllegalArgumentException e) {
            log("❌ Error: punto de entrada inválido: " + entryPoint);
            return null;
        }
    }
    
    /**
     * Limpia vehículos que han completado su recorrido
     * ARREGLADO: No usa iterator.remove() que no es soportado por CopyOnWriteArrayList
     */
    private void cleanupCompletedVehicles() {
        // Recolectar vehículos completados para remover
        List<TickBasedVehicle> completedVehicles = new ArrayList<>();
        
        for (TickBasedVehicle vehicle : activeVehicles) {
            if (vehicle != null && vehicle.getState() == VehicleState.COMPLETED) {
                completedVehicles.add(vehicle);
            }
        }
        
        // Remover vehículos completados de manera segura
        for (TickBasedVehicle vehicle : completedVehicles) {
            try {
                // Remover de vehículos activos
                activeVehicles.remove(vehicle);
                
                // Remover de la cola de entrada correspondiente
                String entryPointStr = vehicle.getEntryPoint().name().toLowerCase();
                Queue<TickBasedVehicle> entryQueue = entryQueues.get(entryPointStr);
                if (entryQueue != null) {
                    entryQueue.remove(vehicle);
                }
                
                log("🏁 Vehículo " + vehicle.getId() + " completado y removido del sistema");
                
                // Actualizar estadísticas
                totalVehiclesProcessed++;
                if (vehicle.isEmergency()) {
                    emergencyVehiclesProcessed++;
                }
                totalWaitingTime += vehicle.getTicksWaiting();
                
            } catch (Exception e) {
                log("⚠️ Error removiendo vehículo completado " + vehicle.getId() + ": " + e.getMessage());
            }
        }
        
        // Solo actualizar posiciones si se removieron vehículos
        if (!completedVehicles.isEmpty()) {
            updateAllQueuePositions();
        }
    }
    
    /**
     * Actualiza las posiciones en cola para todas las direcciones
     * OPTIMIZADO: Solo cuando sea realmente necesario
     */
    private void updateAllQueuePositions() {
        try {
            for (String entryPoint : entryQueues.keySet()) {
                updateQueuePositions(entryPoint);
            }
        } catch (Exception e) {
            log("⚠️ Error actualizando posiciones globales: " + e.getMessage());
        }
    }
    
    /**
     * Actualiza las posiciones en cola después de que un vehículo sale
     * OPTIMIZADO: Evita streams costosos, ordena manualmente
     */
    private void updateQueuePositions(String entryPoint) {
        try {
            // Recolectar vehículos de esta dirección de manera optimizada
            List<TickBasedVehicle> vehiclesInDirection = new ArrayList<>();
            
            for (TickBasedVehicle v : activeVehicles) {
                if (v != null && 
                    v.getEntryPoint().name().toLowerCase().equals(entryPoint) &&
                    v.getState() != VehicleState.COMPLETED) {
                    vehiclesInDirection.add(v);
                }
            }
            
            // Ordenar por prioridad
            vehiclesInDirection.sort(this::compareVehiclePriority);
            
            // Actualizar las posiciones en cola secuencialmente
            for (int i = 0; i < vehiclesInDirection.size(); i++) {
                TickBasedVehicle vehicle = vehiclesInDirection.get(i);
                if (vehicle != null) {
                    vehicle.updateQueuePosition(i);
                }
            }
            
            // También actualizar la cola física
            Queue<TickBasedVehicle> queue = entryQueues.get(entryPoint);
            if (queue != null) {
                // Limpiar la cola y re-agregar los vehículos activos
                queue.clear();
                for (TickBasedVehicle vehicle : vehiclesInDirection) {
                    if (vehicle != null && vehicle.getState() != VehicleState.COMPLETED) {
                        queue.offer(vehicle);
                    }
                }
            }
            
        } catch (Exception e) {
            log("⚠️ Error actualizando posiciones en cola para " + entryPoint + ": " + e.getMessage());
        }
    }
    
    // === ESTADÍSTICAS Y LOGGING ===
    
    private void updateStatistics() {
        // Las estadísticas se actualizan en cleanupCompletedVehicles()
    }
    
    /**
     * Log de debugging para mostrar vehículos esperando y orden FIFO
     */
    private void logWaitingVehicles() {
        try {
            List<TickBasedVehicle> waitingVehicles = new ArrayList<>();
            
            for (TickBasedVehicle vehicle : activeVehicles) {
                if (vehicle != null && 
                    vehicle.getState() == VehicleState.WAITING && 
                    !vehicle.isAuthorizedToCross()) {
                    waitingVehicles.add(vehicle);
                }
            }
            
            if (!waitingVehicles.isEmpty()) {
                // Ordenar por prioridad FIFO
                waitingVehicles.sort(this::compareVehiclePriority);
                
                StringBuilder sb = new StringBuilder("🚦 Cola FIFO: ");
                for (int i = 0; i < Math.min(waitingVehicles.size(), 5); i++) {
                    TickBasedVehicle v = waitingVehicles.get(i);
                    sb.append(v.getId())
                      .append("(").append(v.getEntryPoint().name().toLowerCase()).append(",orden:")
                      .append(v.getArrivalOrder()).append(")");
                    if (i < Math.min(waitingVehicles.size(), 5) - 1) sb.append(" → ");
                }
                if (waitingVehicles.size() > 5) {
                    sb.append(" ... (+").append(waitingVehicles.size() - 5).append(" más)");
                }
                
                log(sb.toString());
            }
        } catch (Exception e) {
            log("⚠️ Error en logWaitingVehicles: " + e.getMessage());
        }
    }
    
    /**
     * OPTIMIZADO: Evita múltiples streams, cuenta en una sola pasada
     * MEJORADO: Incluye conteo FÍSICO de vehículos en intersección y autorizados
     */
    private void logSystemStatus() {
        int totalActive = activeVehicles.size();
        int waiting = 0, crossing = 0, approaching = 0, physicallyInIntersection = 0, authorized = 0;
        
        for (TickBasedVehicle vehicle : activeVehicles) {
            if (vehicle != null) {
                // Conteo por estado
                switch (vehicle.getState()) {
                    case WAITING -> waiting++;
                    case CROSSING -> crossing++;
                    case APPROACHING -> approaching++;
                    case EXITING -> { /* No contamos los que están saliendo */ }
                    case COMPLETED -> { /* No contamos los completados */ }
                }
                
                // NUEVO: Conteo FÍSICO en intersección
                if (vehicle.getCurrentPosition() != null && 
                    IntersectionCoordinates.isVehicleInIntersection(vehicle.getCurrentPosition())) {
                    physicallyInIntersection++;
                }
                
                // NUEVO: Conteo de vehículos autorizados
                if (vehicle.isAuthorizedToCross()) {
                    authorized++;
                }
            }
        }
        
        // Log mejorado con información completa
        if (physicallyInIntersection > 0 || authorized > 0) {
            log(String.format("📊 Tick %d | Activos: %d | Esperando: %d | Cruzando: %d | Acercándose: %d | 🚧 En intersección: %d | ✅ Autorizados: %d", 
                currentTick / TICKS_PER_SECOND, totalActive, waiting, crossing, approaching, physicallyInIntersection, authorized));
        } else {
            log(String.format("📊 Tick %d | Activos: %d | Esperando: %d | Cruzando: %d | Acercándose: %d", 
                currentTick / TICKS_PER_SECOND, totalActive, waiting, crossing, approaching));
        }
    }
    
    private void log(String message) {
        if (logCallback != null) {
            logCallback.onLog(message);
        } else {
            System.out.println(message);
        }
    }
    
    // === SETTERS PARA CALLBACKS ===
    
    public void setVehicleUpdateCallback(VehicleUpdateCallback callback) {
        this.vehicleUpdateCallback = callback;
    }
    
    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }
    
    // === GETTERS PARA INFORMACIÓN DEL SISTEMA ===
    
    public List<TickBasedVehicle> getActiveVehicles() {
        return new ArrayList<>(activeVehicles);
    }
    
    public int getTotalVehiclesProcessed() {
        return totalVehiclesProcessed;
    }
    
    public int getEmergencyVehiclesProcessed() {
        return emergencyVehiclesProcessed;
    }
    
    public double getAverageWaitingTime() {
        return totalVehiclesProcessed > 0 ? 
            (double) totalWaitingTime / totalVehiclesProcessed / TICKS_PER_SECOND : 0.0;
    }
    
    public long getCurrentTick() {
        return currentTick;
    }
    
    public boolean isRunning() {
        return running && !tickScheduler.isShutdown() && !tickScheduler.isTerminated();
    }
}
