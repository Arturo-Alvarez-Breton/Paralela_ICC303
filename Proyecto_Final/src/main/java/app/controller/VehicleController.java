package app.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import app.model.Street;
import app.model.Vehicle;
import app.model.enums.DirectionEnum;
import app.model.enums.VehicleTypeEnum;
import app.ui.VehicleView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Controlador para manejar la creación, movimiento y comportamiento de vehículos
 * Basado en sistema de ticks para simulación precisa
 */
public class VehicleController implements TickController.TickListener {
    
    private final Pane scene;
    private final ScenarioController scenarioController;
    private final TickController tickController;
    
    // Mapas para gestionar vehículos
    private final Map<String, Vehicle> activeVehicles;
    private final Map<String, VehicleView> vehicleViews;
    private final Map<String, VehiclePath> vehiclePaths;
    
    // Sistema de colisiones y control de intersección
    private final CollisionManager collisionManager;
    
    // Control de simulación
    private boolean autoSpawnEnabled = false;
    
    // Configuración de spawn (basado en ticks)
    private static final int SPAWN_TICK_INTERVAL = 60; // Cada 60 ticks (3 segundos)
    private static final double EMERGENCY_PROBABILITY = 0.15; // 15% de vehículos de emergencia
    private long lastSpawnTick = 0;
    
    // Velocidades (píxeles por tick) - IGUALADAS PARA AMBOS TIPOS
    private static final double NORMAL_SPEED = 1.5;
    private static final double EMERGENCY_SPEED = 1.5;
    
    public VehicleController(Pane scene, ScenarioController scenarioController, TickController tickController) {
        this.scene = scene;
        this.scenarioController = scenarioController;
        this.tickController = tickController;
        this.activeVehicles = new ConcurrentHashMap<>();
        this.vehicleViews = new ConcurrentHashMap<>();
        this.vehiclePaths = new ConcurrentHashMap<>();
        this.collisionManager = new CollisionManager();
        
        // Registrarse como listener del tick controller
        tickController.addTickListener(this);
        System.out.println("VehicleController registrado en TickController");
    }
    
    /**
     * Implementación del TickListener - se ejecuta en cada tick
     */
    @Override
    public void onTick(long tickNumber) {
        // NUEVO: Actualizar el CollisionManager en cada tick
        collisionManager.onTick();
        
        // Spawn automático de vehículos si está habilitado
        if (autoSpawnEnabled && (tickNumber - lastSpawnTick) >= SPAWN_TICK_INTERVAL) {
            spawnRandomVehicle();
            lastSpawnTick = tickNumber;
        }
        
        // Actualizar movimiento de todos los vehículos
        updateVehicleMovement();
        
        // Limpiar vehículos que han salido del área
        cleanupVehicles();
    }
    
    /**
     * Crea un vehículo aleatorio en una calle de entrada aleatoria con retry logic
     */
    public void spawnRandomVehicle() {
        List<Street> entryStreets = getEntryStreets();
        if (entryStreets.isEmpty()) return;
        
        // Intentar spawn en calles aleatorias hasta encontrar una disponible
        List<Street> availableStreets = new ArrayList<>(entryStreets);
        Collections.shuffle(availableStreets);
        
        for (Street entryStreet : availableStreets) {
            // Determinar tipo de vehículo
            VehicleTypeEnum type = Math.random() < EMERGENCY_PROBABILITY ? 
                VehicleTypeEnum.EMERGENCY : VehicleTypeEnum.NORMAL;
            
            // Seleccionar dirección aleatoria de las permitidas en la calle
            List<DirectionEnum> possibleDirections = entryStreet.getPossibleDirections();
            DirectionEnum direction = possibleDirections.get(
                ThreadLocalRandom.current().nextInt(possibleDirections.size()));
            
            // Intentar crear el vehículo - si tiene éxito, terminar
            if (trySpawnVehicle(entryStreet, type, direction)) {
                return;
            }
        }
        
        System.out.println("No se pudo crear vehículo - todas las calles de entrada están bloqueadas");
    }
    
    /**
     * Intenta crear un vehículo, retorna true si tuvo éxito
     */
    private boolean trySpawnVehicle(Street entryStreet, VehicleTypeEnum type, DirectionEnum direction) {
        // Calcular posición inicial y ruta completa
        VehiclePath path = calculateVehiclePath(entryStreet, direction);
        if (path == null) return false;
        
        // Determinar dirección de spawn
        String spawnDirection = parseDirectionFromStreetId(entryStreet.getId());
        
        // Verificar si es seguro crear el vehículo en esta posición
        if (!collisionManager.canSpawnVehicle(spawnDirection, path.getStartX(), path.getStartY())) {
            System.out.println("Spawn bloqueado para dirección " + spawnDirection + " - vehículos demasiado cerca");
            return false; // No crear el vehículo si hay conflicto
        }
        
        // Crear modelo de vehículo
        Vehicle vehicle = new Vehicle(type, direction);
        
        // Crear vista visual
        VehicleView vehicleView = createVehicleView(vehicle, path.getStartX(), path.getStartY(), entryStreet);
        
        // Registrar vehículo
        activeVehicles.put(vehicle.getId(), vehicle);
        vehicleViews.put(vehicle.getId(), vehicleView);
        vehiclePaths.put(vehicle.getId(), path);
        
        // Registrar en el collision manager con dirección de spawn
    boolean isEmergency = vehicle.getType() == VehicleTypeEnum.EMERGENCY;
    collisionManager.registerVehicle(vehicle.getId(), path.getStartX(), path.getStartY(), spawnDirection, isEmergency);
        
        // Agregar a la escena
        scene.getChildren().add(vehicleView);
        
        System.out.println("Vehículo creado: " + vehicle.getId() + " (" + type + ") en " + 
                          entryStreet.getId() + " -> " + direction + " (spawn: " + spawnDirection + ")");
        return true;
    }
    
    /**
     * Método público para crear un vehículo específico
     */
    public boolean spawnVehicle(Street entryStreet, VehicleTypeEnum type, DirectionEnum direction) {
        return trySpawnVehicle(entryStreet, type, direction);
    }
    
    /**
     * Crea la vista visual del vehículo
     */
    private VehicleView createVehicleView(Vehicle vehicle, double x, double y, Street entryStreet) {
        Color fillColor = vehicle.getType() == VehicleTypeEnum.EMERGENCY ? 
            Color.RED : Color.BLUE;
        Color strokeColor = vehicle.getType() == VehicleTypeEnum.EMERGENCY ? 
            Color.DARKRED : Color.DARKBLUE;
        
        String entryPoint = parseEntryPoint(entryStreet.getId());
        
        return new VehicleView(x, y, fillColor, strokeColor, 
            vehicle.getType(), vehicle.getDirection(), entryPoint);
    }
    
    /**
     * Calcula la ruta completa que debe seguir un vehículo
     */
    private VehiclePath calculateVehiclePath(Street entryStreet, DirectionEnum direction) {
        String entryDirection = parseDirectionFromStreetId(entryStreet.getId());
        
        // Posición inicial (extremo de la calle de entrada)
        double startX, startY;
        
        // Calcular posición inicial en el EXTREMO CORRECTO de la calle de entrada
        switch (entryDirection) {
            case "north":
                // Lado superior: carril azul va hacia el sur (↓)
                // Vehículo aparece en el carril azul (entrada) del lado norte
                startX = entryStreet.getPosX() + entryStreet.getWidth() / 2.0; // Centro del carril azul
                startY = entryStreet.getPosY(); // Extremo superior (más alejado de la intersección)
                System.out.println("Vehículo Norte creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                break;
            case "south":
                // Lado inferior: carril azul va hacia el norte (↑)
                // Vehículo aparece en el carril azul (entrada) del lado sur
                startX = entryStreet.getPosX() + entryStreet.getWidth() / 2.0; // Centro del carril azul
                startY = entryStreet.getPosY() + entryStreet.getHeight(); // Extremo inferior (más alejado de la intersección)
                System.out.println("Vehículo Sur creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                break;
            case "east":
                // Lado derecho: carril azul va hacia el oeste
                // Vehículo aparece en el carril azul (entrada) del lado este
                startX = entryStreet.getPosX() + entryStreet.getWidth(); // Extremo derecho (más alejado de la intersección)
                startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0; // Centro del carril azul
                System.out.println("Vehículo Este creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                break;
            case "west":
                // Lado izquierdo: carril azul va hacia el este (→)
                // Vehículo aparece en el carril azul (entrada) del lado oeste
                startX = entryStreet.getPosX(); // Extremo izquierdo (más alejado de la intersección)
                startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0; // Centro del carril azul
                System.out.println("Vehículo Oeste creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                break;
            default:
                return null;
        }
        
        // Calcular ruta completa basada en la dirección elegida
        return new VehiclePath(startX, startY, entryDirection, direction, scenarioController);
    }
    
    /**
     * Actualiza el movimiento de todos los vehículos con control de colisiones
     */
    private void updateVehicleMovement() {
        for (String vehicleId : new ArrayList<>(activeVehicles.keySet())) {
            Vehicle vehicle = activeVehicles.get(vehicleId);
            VehicleView view = vehicleViews.get(vehicleId);
            VehiclePath path = vehiclePaths.get(vehicleId);
            
            if (vehicle == null || view == null || path == null) continue;
            
            // Calcular velocidad según tipo de vehículo
            double speed = vehicle.getType() == VehicleTypeEnum.EMERGENCY ? 
                EMERGENCY_SPEED : NORMAL_SPEED;
            
            // Obtener posición actual
            double currentX = path.getCurrentX();
            double currentY = path.getCurrentY();
            
            // Calcular próxima posición planeada
            double nextX = currentX;
            double nextY = currentY;
            
            // Calcular dirección de movimiento hacia el siguiente waypoint
            if (path.getCurrentWaypointIndex() < path.getWaypoints().size()) {
                VehiclePath.PathPoint targetPoint = path.getWaypoints().get(path.getCurrentWaypointIndex());
                double deltaX = targetPoint.x - currentX;
                double deltaY = targetPoint.y - currentY;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                
                if (distance > 0) {
                    double directionX = deltaX / distance;
                    double directionY = deltaY / distance;
                    double moveDistance = Math.min(speed, distance);
                    nextX = currentX + directionX * moveDistance;
                    nextY = currentY + directionY * moveDistance;
                }
            }
            
            // Verificar si puede avanzar usando el collision manager
            if (collisionManager.canMove(vehicleId, nextX, nextY, speed)) {
                // Mover vehículo a lo largo de su ruta
                if (path.moveAlongPath(speed)) {
                    // Actualizar posición visual
                    view.setPosition(path.getCurrentX(), path.getCurrentY());
                    
                    // Actualizar posición en el collision manager
                    collisionManager.updateVehiclePosition(vehicleId, path.getCurrentX(), path.getCurrentY());
                    
                    // Notificar si salió de la intersección
                    collisionManager.vehicleExitedIntersection(vehicleId);
                } else {
                    // Vehículo ha completado su ruta
                    removeVehicle(vehicleId);
                }
            } else {
                // El vehículo debe esperar - no mover
                System.out.println("Vehículo " + vehicleId + " esperando por colisión/intersección");
            }
        }
    }
    
    /**
     * Elimina un vehículo de la simulación
     */
    private void removeVehicle(String vehicleId) {
        VehicleView view = vehicleViews.get(vehicleId);
        if (view != null) {
            scene.getChildren().remove(view);
        }
        
        // Limpiar tracking del collision manager
        collisionManager.removeVehicleFromTracking(vehicleId);
        
        activeVehicles.remove(vehicleId);
        vehicleViews.remove(vehicleId);
        vehiclePaths.remove(vehicleId);
        
        System.out.println("Vehículo removido: " + vehicleId);
    }
    
    /**
     * Limpia vehículos que han salido del área visible
     */
    private void cleanupVehicles() {
        List<String> toRemove = new ArrayList<>();
        
        for (String vehicleId : activeVehicles.keySet()) {
            VehiclePath path = vehiclePaths.get(vehicleId);
            if (path != null && path.isOutOfBounds()) {
                toRemove.add(vehicleId);
            }
        }
        
        for (String vehicleId : toRemove) {
            removeVehicle(vehicleId);
        }
    }
    
    /**
     * Obtiene las calles de entrada (azules)
     */
    private List<Street> getEntryStreets() {
        return scenarioController.getAllStreets().stream()
            .filter(street -> street.getId().contains("entrada"))
            .toList();
    }
    
    /**
     * Parsea la dirección desde el ID de la calle
     */
    private String parseDirectionFromStreetId(String streetId) {
        if (streetId.contains("north") || streetId.contains("norte")) return "north";
        if (streetId.contains("south") || streetId.contains("sur")) return "south";
        if (streetId.contains("east") || streetId.contains("este")) return "east";
        if (streetId.contains("west") || streetId.contains("oeste")) return "west";
        return "";
    }
    
    /**
     * Parsea el punto de entrada para VehicleView
     */
    private String parseEntryPoint(String streetId) {
        if (streetId.contains("north") || streetId.contains("norte")) return "norte";
        if (streetId.contains("south") || streetId.contains("sur")) return "sur";
        if (streetId.contains("east") || streetId.contains("este")) return "este";
        if (streetId.contains("west") || streetId.contains("oeste")) return "oeste";
        return "norte";
    }
    
    // Métodos de control
    public void cleanup() {
        // Desregistrarse del tick controller
        tickController.removeTickListener(this);
        
        // Limpiar todos los vehículos
        for (String vehicleId : new ArrayList<>(activeVehicles.keySet())) {
            removeVehicle(vehicleId);
        }
        
        System.out.println("VehicleController limpiado y desregistrado");
    }
    
    public void setAutoSpawn(boolean enabled) {
        this.autoSpawnEnabled = enabled;
        System.out.println("Auto-spawn de vehículos: " + (enabled ? "activado" : "desactivado"));
    }
    
    public boolean isAutoSpawnEnabled() {
        return autoSpawnEnabled;
    }
    
    public boolean isRunning() {
        return tickController.isRunning();
    }
    
    public int getActiveVehicleCount() {
        return activeVehicles.size();
    }
    
    public Map<String, Vehicle> getActiveVehicles() {
        return new HashMap<>(activeVehicles);
    }
    
    // === MÉTODOS PÚBLICOS PARA CONTROL DE COLISIONES ===
    
    /**
     * Obtiene el estado actual del sistema de colisiones
     */
    public String getCollisionSystemStatus() {
        return collisionManager.getIntersectionStatus() + " - Vehículos activos: " + activeVehicles.size();
    }
    
    /**
     * Reinicia el estado del sistema de colisiones (útil para debugging)
     */
    public void resetCollisionSystem() {
        collisionManager.resetIntersectionState();
        System.out.println("Sistema de colisiones reiniciado");
    }
    
    /**
     * Verifica si un vehículo específico está esperando
     */
    public boolean isVehicleWaiting(String vehicleId) {
        return collisionManager.isVehicleWaiting(vehicleId);
    }
    
    /**
     * Obtiene la lista de vehículos que están actualmente en la intersección
     */
    public Set<String> getVehiclesInIntersection() {
        return collisionManager.getVehiclesInIntersection();
    }
    
    /**
     * Obtiene la lista de vehículos que están esperando para entrar a la intersección
     */
    public Set<String> getVehiclesWaiting() {
        return collisionManager.getVehiclesInWaitingZone();
    }
}