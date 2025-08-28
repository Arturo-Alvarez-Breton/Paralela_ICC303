package app.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import app.model.Intersection;
import app.model.Street;
import app.model.TrafficLight;
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
    private final TrafficController trafficController; // Nuevo campo para control de semáforos
    
    // Mapas para gestionar vehículos
    private final Map<String, Vehicle> activeVehicles;
    private final Map<String, VehicleView> vehicleViews;
    private final Map<String, VehiclePath> vehiclePaths;
    
    // Sistema de colisiones y control de intersección
    private final CollisionManager collisionManager;
    
    // NUEVO: Sistema de control de intersecciones
    private final Map<String, String> intersectionOccupancy; // intersectionId -> vehicleId que la ocupa
    private final Map<String, Queue<String>> intersectionQueues; // intersectionId -> cola de vehículos esperando
    
    // Control de simulación
    private boolean autoSpawnEnabled = false;
    
    // Configuración de spawn (basado en ticks)
    private static final int SPAWN_TICK_INTERVAL = 60; // Cada 60 ticks (3 segundos)
    private static final double EMERGENCY_PROBABILITY = 0.15; // 15% de vehículos de emergencia
    private long lastSpawnTick = 0;
    
    // Velocidades (píxeles por tick) - IGUALADAS PARA AMBOS TIPOS
    private static final double NORMAL_SPEED = 1.5;
    private static final double EMERGENCY_SPEED = 1.5;
    
    public VehicleController(Pane scene, ScenarioController scenarioController, TickController tickController, TrafficController trafficController) {
        this.scene = scene;
        this.scenarioController = scenarioController;
        this.tickController = tickController;
        this.trafficController = trafficController;
        this.activeVehicles = new ConcurrentHashMap<>();
        this.vehicleViews = new ConcurrentHashMap<>();
        this.vehiclePaths = new ConcurrentHashMap<>();
        this.collisionManager = new CollisionManager();
        
        // Inicializar sistema de control de intersecciones
        this.intersectionOccupancy = new ConcurrentHashMap<>();
        this.intersectionQueues = new ConcurrentHashMap<>();
        
        // Inicializar colas para intersection_2 e intersection_3 
        this.intersectionQueues.put("intersection_2", new ConcurrentLinkedQueue<>());
        this.intersectionQueues.put("intersection_3", new ConcurrentLinkedQueue<>());
        
        // Registrarse como listener del tick controller
        tickController.addTickListener(this);
        System.out.println("VehicleController registrado en TickController con sistema de intersecciones");
    }
    
    // Constructor para retrocompatibilidad (sin TrafficController)
    public VehicleController(Pane scene, ScenarioController scenarioController, TickController tickController) {
        this(scene, scenarioController, tickController, null);
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
            
            // Detectar si es Escenario 2
            String streetId = entryStreet.getId();
            boolean isScenario2 = streetId.contains("_segment") && streetId.contains("_lane");
            
            if (isScenario2) {
                // ESCENARIO 2: Usar la MISMA lógica que el sistema manual
                // 1. Seleccionar entrada aleatoria (East o West)
                String[] entries = {"East", "West"};
                String selectedEntry = entries[ThreadLocalRandom.current().nextInt(entries.length)];
                
                // 2. Generar destino aleatorio para esa entrada
                String specificDestination = generateRandomScenario2DestinationForEntry(selectedEntry);
                
                // 3. Obtener la calle específica usando la MISMA lógica que el manual
                Street specificStreet = getManualStyleEntryStreet(selectedEntry, specificDestination);
                
                if (specificStreet != null) {
                    // 4. Calcular dirección usando la MISMA lógica que el manual
                    DirectionEnum direction = getManualStyleDirection(selectedEntry, specificDestination);
                    
                    if (spawnVehicle(specificStreet, type, direction, specificDestination)) {
                        System.out.println("🚗 AUTO-SPAWN Escenario 2: Vehículo " + type + " desde " + selectedEntry + " hacia " + specificDestination + " en " + specificStreet.getId());
                        return;
                    }
                }
            } else {
                // ESCENARIO 1: Lógica original
                List<DirectionEnum> possibleDirections = getAllDirections();
                DirectionEnum direction = possibleDirections.get(
                    ThreadLocalRandom.current().nextInt(possibleDirections.size()));
                
                if (trySpawnVehicle(entryStreet, type, direction)) {
                    System.out.println("🚗 AUTO-SPAWN Escenario 1: Vehículo " + type + " creado");
                    return;
                }
            }
        }
        
        System.out.println("No se pudo crear vehículo - todas las calles de entrada están bloqueadas");
    }
    
    public List<DirectionEnum> getAllDirections(){
        List<DirectionEnum> allDirections = new ArrayList<>();
        Collections.addAll(allDirections, DirectionEnum.values());
        return allDirections;
    }
    
    // NUEVOS MÉTODOS: Replicar EXACTAMENTE la lógica del sistema manual
    private String generateRandomScenario2DestinationForEntry(String entry) {
        // Replica EXACTAMENTE la lógica del LaunchView para generar destinos por entrada
        String[] destinations;
        
        if ("East".equals(entry)) {
            destinations = new String[]{"West_Exit", "South_Exit", "North_Exit"};
        } else { // "West"
            destinations = new String[]{"East_Exit", "South_Exit", "North_Exit"};
        }
        
        return destinations[ThreadLocalRandom.current().nextInt(destinations.length)];
    }
    
    private Street getManualStyleEntryStreet(String entry, String destination) {
        // Replica EXACTAMENTE la lógica del LaunchView para seleccionar calles por entrada/destino
        String[] laneIds;
        
        if ("East".equals(entry)) {
            laneIds = new String[]{"east_center_lane_segment1", "east_left_lane_segment1", "east_right_lane_segment1"};
        } else { // "West" 
            laneIds = new String[]{"west_center_lane_segment3", "west_left_lane_segment3", "west_right_lane_segment3"};
        }
        
        // Seleccionar carril aleatorio (igual que manual)
        String selectedLaneId = laneIds[ThreadLocalRandom.current().nextInt(laneIds.length)];
        
        // Buscar y retornar la calle usando scenarioController
        for (Street street : scenarioController.getAllStreets()) {
            if (selectedLaneId.equals(street.getId())) {
                return street;
            }
        }
        return null;
    }
    
    private DirectionEnum getManualStyleDirection(String entry, String destination) {
        // Replica EXACTAMENTE la lógica del IntersectionView para dirección por entrada/destino
        if (destination.equals("West_Exit") || destination.equals("East_Exit")) {
            // Destino recto
            return DirectionEnum.STRAIGHT;
        } else if (destination.equals("South_Exit")) {
            // Destino sur
            if ("East".equals(entry)) {
                return DirectionEnum.RIGHT; // East gira derecha para ir al sur
            } else { // "West"
                return DirectionEnum.LEFT; // West gira izquierda para ir al sur
            }
        } else if (destination.equals("North_Exit")) {
            // Destino norte
            if ("East".equals(entry)) {
                return DirectionEnum.LEFT; // East gira izquierda para ir al norte
            } else { // "West"
                return DirectionEnum.RIGHT; // West gira derecha para ir al norte
            }
        }
        
        // Fallback
        return DirectionEnum.STRAIGHT;
    }
    
    /**
     * CORREGIDO: Genera un destino específico aleatorio para el Escenario 2 
     * usando las mismas opciones que el sistema manual
     */
    private String generateRandomScenario2Destination(Street entryStreet) {
        String streetId = entryStreet.getId().toLowerCase();
        
        // Destinos disponibles según la entrada - IGUALES AL MANUAL
        List<String> availableDestinations = new ArrayList<>();
        
        if (streetId.contains("east")) {
            // East puede ir a: Recto, S1-1, S1-2, N2-1, N2-2, U-Turn (igual al manual)
            availableDestinations.add("Recto");
            availableDestinations.add("S1-1");
            availableDestinations.add("S1-2"); 
            availableDestinations.add("N2-1");
            availableDestinations.add("N2-2");
            availableDestinations.add("U-Turn");
        } else if (streetId.contains("west")) {
            // West puede ir a: Recto, S2-1, S2-2, N2-1, N2-2, U-Turn (igual al manual)
            availableDestinations.add("Recto");
            availableDestinations.add("S2-1");
            availableDestinations.add("S2-2");
            availableDestinations.add("N2-1");
            availableDestinations.add("N2-2");
            availableDestinations.add("U-Turn");
        } else {
            // Fallback para otras entradas
            availableDestinations.add("Recto");
        }
        
        // Seleccionar destino aleatorio
        int randomIndex = ThreadLocalRandom.current().nextInt(availableDestinations.size());
        String selectedDestination = availableDestinations.get(randomIndex);
        
        System.out.println("🎯 AUTO-SPAWN: Destino generado: " + selectedDestination + " para calle " + streetId);
        return selectedDestination;
    }
    
    /**
     * Determina la DirectionEnum apropiada basada en el destino y la calle de entrada
     */
    private DirectionEnum getDirectionForDestination(String destination, Street entryStreet) {
        String streetId = entryStreet.getId().toLowerCase();
        boolean isWest = streetId.contains("west");
        
        DirectionEnum result;
        
        switch (destination) {
            case "Recto":
                result = DirectionEnum.STRAIGHT;
                break;
            case "U-Turn":
                result = DirectionEnum.U_TURN;
                break;
            default:
                if (destination.contains("S1") || destination.contains("S2")) {
                    // Destinos Sur
                    result = isWest ? DirectionEnum.LEFT : DirectionEnum.RIGHT;
                } else if (destination.contains("N1") || destination.contains("N2")) {
                    // Destinos Norte
                    result = isWest ? DirectionEnum.RIGHT : DirectionEnum.LEFT;
                } else {
                    result = DirectionEnum.STRAIGHT;
                }
        }
        
        System.out.println("🧭 MAPEO DIRECCIÓN AUTO-SPAWN:");
        System.out.println("    🏁 Calle: " + streetId + " (West: " + isWest + ")");
        System.out.println("    🎯 Destino: " + destination);
        System.out.println("    ➡️  DirectionEnum: " + result);
        
        return result;
    }
    
    /**
     * Intenta crear un vehículo, retorna true si tuvo éxito
     */
    private boolean trySpawnVehicle(Street entryStreet, VehicleTypeEnum type, DirectionEnum direction) {
        return trySpawnVehicle(entryStreet, type, direction, null);
    }
    
    /**
     * Intenta crear un vehículo con destino específico, retorna true si tuvo éxito
     */
    private boolean trySpawnVehicle(Street entryStreet, VehicleTypeEnum type, DirectionEnum direction, String specificDestination) {
        System.out.println("🚀 === CREANDO VEHÍCULO ===");
        System.out.println("    🏁 Calle de entrada: " + entryStreet.getId());
        System.out.println("    🚗 Tipo: " + type);
        System.out.println("    ➡️  Dirección: " + direction);
        System.out.println("    🎯 Destino específico: " + specificDestination);
        
        // Calcular posición inicial y ruta completa
        VehiclePath path = calculateVehiclePath(entryStreet, direction, specificDestination);
        if (path == null) {
            System.out.println("❌ Error: No se pudo calcular VehiclePath");
            return false;
        }
        
        System.out.println("    📍 Posición inicial: (" + path.getStartX() + ", " + path.getStartY() + ")");
        System.out.println("    🛤️  Waypoints totales: " + path.getWaypoints().size());
        
        // Determinar dirección de spawn
        String spawnDirection = parseDirectionFromStreetId(entryStreet.getId());
        System.out.println("    🧭 Dirección spawn: " + spawnDirection);
        
        // Verificar si es seguro crear el vehículo en esta posición
        if (!collisionManager.canSpawnVehicle(spawnDirection, path.getStartX(), path.getStartY())) {
            System.out.println("Spawn bloqueado para dirección " + spawnDirection + " - vehículos demasiado cerca");
            return false; // No crear el vehículo si hay conflicto
        }
        
        // Crear modelo de vehículo con destino específico
        Vehicle vehicle;
        if (specificDestination != null) {
            vehicle = new Vehicle(type, direction, specificDestination);
        } else {
            vehicle = new Vehicle(type, direction);
        }
        
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
        return trySpawnVehicle(entryStreet, type, direction, null);
    }
    
    /**
     * Método público para crear un vehículo específico con destino específico
     */
    public boolean spawnVehicle(Street entryStreet, VehicleTypeEnum type, DirectionEnum direction, String specificDestination) {
        return trySpawnVehicle(entryStreet, type, direction, specificDestination);
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
        return calculateVehiclePath(entryStreet, direction, null);
    }
    
    /**
     * Calcula la ruta completa que debe seguir un vehículo con destino específico
     */
    private VehiclePath calculateVehiclePath(Street entryStreet, DirectionEnum direction, String specificDestination) {
        String streetId = entryStreet.getId();
        String entryDirection = parseDirectionFromStreetId(streetId);
        
        // Posición inicial (extremo de la calle de entrada)
        double startX, startY;
        
        // Detectar si es Escenario 2 (autopista) por el formato del ID
        if (streetId.contains("_lane_segment")) {
            // ESCENARIO 2: Autopista
            System.out.println("Calculando spawn para Escenario 2 - Calle: " + streetId);
            
            if (streetId.startsWith("east_")) {
                // Carril East: va de oeste hacia este (West→East) - banda inferior
                // Vehículo aparece en el extremo IZQUIERDO del segmento
                startX = entryStreet.getPosX(); // Extremo izquierdo
                startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0; // Centro del carril
                entryDirection = "west"; // Viene desde el oeste hacia el este
                System.out.println("Vehículo East creado en: (" + startX + ", " + startY + ") - va hacia el este");
            } else if (streetId.startsWith("west_")) {
                // Carril West: va de este hacia oeste (East→West) - banda superior
                // Vehículo aparece en el extremo DERECHO del segmento
                startX = entryStreet.getPosX() + entryStreet.getWidth(); // Extremo derecho
                startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0; // Centro del carril
                entryDirection = "east"; // Viene desde el este hacia el oeste
                System.out.println("Vehículo West creado en: (" + startX + ", " + startY + ") - va hacia el oeste");
            } else {
                // Fallback para otros casos
                startX = entryStreet.getPosX() + entryStreet.getWidth() / 2.0;
                startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0;
                System.out.println("Vehículo spawn fallback en: (" + startX + ", " + startY + ")");
            }
        } else {
            // ESCENARIO 1: Intersección tradicional (código original)
            switch (entryDirection) {
                case "north":
                    startX = entryStreet.getPosX() + entryStreet.getWidth() / 2.0;
                    startY = entryStreet.getPosY();
                    System.out.println("Vehículo Norte creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                    break;
                case "south":
                    startX = entryStreet.getPosX() + entryStreet.getWidth() / 2.0;
                    startY = entryStreet.getPosY() + entryStreet.getHeight();
                    System.out.println("Vehículo Sur creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                    break;
                case "east":
                    startX = entryStreet.getPosX() + entryStreet.getWidth();
                    startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0;
                    System.out.println("Vehículo Este creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                    break;
                case "west":
                    startX = entryStreet.getPosX();
                    startY = entryStreet.getPosY() + entryStreet.getHeight() / 2.0;
                    System.out.println("Vehículo Oeste creado en: (" + startX + ", " + startY + ") - Calle: " + entryStreet.getId());
                    break;
                default:
                    return null;
            }
        }
        
        // Calcular ruta completa basada en la dirección elegida
        if (specificDestination != null) {
            System.out.println("Creando VehiclePath con destino específico: " + specificDestination);
            return new VehiclePath(startX, startY, entryDirection, direction, scenarioController, specificDestination);
        } else {
            return new VehiclePath(startX, startY, entryDirection, direction, scenarioController);
        }
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
            
            // CORREGIDO: Calcular la próxima posición exacta donde se moveráel vehículo
            double nextX = currentX;
            double nextY = currentY;
            
            // Usar el mismo cálculo que path.moveAlongPath para obtener la próxima posición exacta
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
            
            // ESCENARIO 2: Verificación ESTRICTA de semáforos antes de moverse
            boolean canMoveCollision = collisionManager.canMove(vehicleId, nextX, nextY, speed);
            boolean canMoveTrafficLight = canMoveWithTrafficLight(vehicleId, vehicle, currentX, currentY, nextX, nextY);
            
            // AMBAS condiciones deben ser verdaderas para que el vehículo se mueva
            if (canMoveCollision && canMoveTrafficLight) {
                // Solo ahora permitir el movimiento real
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
                // El vehículo debe esperar - NO MOVER
                if (!canMoveTrafficLight) {
                    System.out.println("🚫 SEMÁFORO: Vehículo " + vehicleId + " DETENIDO por semáforo en rojo");
                } else {
                    System.out.println("⚠️ COLISIÓN: Vehículo " + vehicleId + " esperando por colisión/intersección");
                }
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
     * NUEVO: Sistema simple de control de intersecciones
     * Solo un vehículo por intersección, orden de llegada
     */
    private boolean canMoveWithIntersectionControl(String vehicleId, double currentX, double currentY, double nextX, double nextY) {
        // Verificar si el vehículo ya está dentro de una intersección
        String currentIntersection = getCurrentIntersection(currentX, currentY);
        if (currentIntersection != null) {
            // Si ya está dentro, puede continuar hasta salir
            System.out.println("✅ DENTRO: Vehículo " + vehicleId + " puede continuar en " + currentIntersection);
            return true;
        }
        
        // Verificar si se está acercando a una intersección
        String upcomingIntersection = getUpcomingIntersection(vehicleId, currentX, currentY, nextX, nextY);
        if (upcomingIntersection == null) {
            return true; // No hay intersección adelante, puede moverse
        }
        
        // Verificar si la intersección está ocupada
        String occupyingVehicle = intersectionOccupancy.get(upcomingIntersection);
        if (occupyingVehicle == null) {
            // Intersección libre, puede entrar
            intersectionOccupancy.put(upcomingIntersection, vehicleId);
            System.out.println("🟢 ENTRADA: Vehículo " + vehicleId + " entra a " + upcomingIntersection);
            return true;
        }
        
        // Intersección ocupada, verificar si es el primero en la cola
        Queue<String> queue = intersectionQueues.get(upcomingIntersection);
        if (queue != null) {
            if (!queue.contains(vehicleId)) {
                // Agregar a la cola si no está ya
                queue.offer(vehicleId);
                System.out.println("🔄 COLA: Vehículo " + vehicleId + " agregado a cola de " + upcomingIntersection + " (posición " + queue.size() + ")");
            }
            
            // Solo puede pasar si es el primero en la cola
            String nextInQueue = queue.peek();
            if (vehicleId.equals(nextInQueue)) {
                System.out.println("⏳ ESPERANDO: Vehículo " + vehicleId + " es siguiente en cola de " + upcomingIntersection);
                return false; // Debe esperar a que se libere
            } else {
                System.out.println("🚫 BLOQUEADO: Vehículo " + vehicleId + " espera en cola de " + upcomingIntersection);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Libera la intersección cuando un vehículo sale
     */
    private void releaseIntersection(String vehicleId) {
        // Buscar en qué intersección estaba el vehículo
        for (Map.Entry<String, String> entry : intersectionOccupancy.entrySet()) {
            if (vehicleId.equals(entry.getValue())) {
                String intersectionId = entry.getKey();
                intersectionOccupancy.remove(intersectionId);
                
                // Permitir que el siguiente en la cola entre
                Queue<String> queue = intersectionQueues.get(intersectionId);
                if (queue != null && !queue.isEmpty()) {
                    String nextVehicle = queue.poll();
                    if (nextVehicle.equals(vehicleId)) {
                        // El vehículo que sale era el primero en cola, tomar el siguiente
                        if (!queue.isEmpty()) {
                            nextVehicle = queue.peek();
                        }
                    }
                    System.out.println("🔄 LIBERACIÓN: " + intersectionId + " liberada por " + vehicleId + 
                                     (queue.isEmpty() ? "" : ", siguiente: " + queue.peek()));
                } else {
                    System.out.println("🔄 LIBERACIÓN: " + intersectionId + " liberada por " + vehicleId);
                }
                break;
            }
        }
    }
    
    /**
     * Determina si el vehículo está actualmente dentro de una intersección
     */
    private String getCurrentIntersection(double x, double y) {
        List<Intersection> intersections = scenarioController.getAllIntersections();
        
        for (Intersection intersection : intersections) {
            String intersectionId = intersection.getId();
            if (!intersectionId.equals("intersection_2") && !intersectionId.equals("intersection_3")) {
                continue;
            }
            
            double leftBound = intersection.getPosX();
            double rightBound = intersection.getPosX() + intersection.getWidth();
            double topBound = intersection.getPosY();
            double bottomBound = intersection.getPosY() + intersection.getHeight();
            
            if (x >= leftBound && x <= rightBound && y >= topBound && y <= bottomBound) {
                return intersectionId;
            }
        }
        return null;
    }
    
    /**
     * Verifica si un vehículo salió de una intersección que estaba ocupando y la libera
     */
    private void checkAndReleaseIntersection(String vehicleId, double x, double y) {
        // Verificar si el vehículo está ocupando alguna intersección
        for (Map.Entry<String, String> entry : intersectionOccupancy.entrySet()) {
            if (vehicleId.equals(entry.getValue())) {
                String intersectionId = entry.getKey();
                
                // Verificar si ya no está dentro de la intersección
                if (getCurrentIntersection(x, y) == null) {
                    // El vehículo salió de la intersección, liberarla
                    intersectionOccupancy.remove(intersectionId);
                    
                    // Permitir que el siguiente vehículo en cola entre
                    Queue<String> queue = intersectionQueues.get(intersectionId);
                    if (queue != null && !queue.isEmpty()) {
                        // Remover el vehículo que acaba de salir de la cola si está ahí
                        queue.remove(vehicleId);
                        System.out.println("🔄 SALIDA: Vehículo " + vehicleId + " salió de " + intersectionId + 
                                         (queue.isEmpty() ? "" : ", siguiente en cola puede proceder"));
                    } else {
                        System.out.println("🔄 SALIDA: Vehículo " + vehicleId + " salió de " + intersectionId);
                    }
                    break;
                }
            }
        }
    }
    
    /**
     * SIMPLIFICADO: Verifica si un vehículo puede avanzar basado SOLO en semáforos
     */
    private boolean canMoveWithTrafficLight(String vehicleId, Vehicle vehicle, double currentX, double currentY, double nextX, double nextY) {
        if (trafficController == null) {
            System.out.println("⚠️ TrafficController es null para vehículo " + vehicleId);
            return true; // Si no hay controlador de semáforos, permitir movimiento
        }
        
        // IMPORTANTE: Si el vehículo ya está dentro de una intersección, puede continuar
        if (isVehicleInsideIntersection(currentX, currentY)) {
            System.out.println("✅ Vehículo " + vehicleId + " DENTRO de intersección - puede continuar");
            return true;
        }
        
        // ESCENARIO 2: Verificar si hay una intersección INMEDIATAMENTE adelante
        String upcomingIntersection = getImmediateIntersectionAhead(vehicleId, currentX, currentY, nextX, nextY);
        if (upcomingIntersection == null) {
            // No hay intersección inmediatamente adelante - puede moverse libremente
            return true;
        }
        
        // Hay una intersección adelante - verificar semáforo antes de entrar
        VehiclePath path = vehiclePaths.get(vehicleId);
        if (path == null) {
            System.out.println("⚠️ Path es null para vehículo " + vehicleId);
            return true;
        }
        
        // Obtener la dirección principal del movimiento
        String vehicleDirection = getVehicleMainDirection(path);
        
        // Construir el ID del semáforo relevante
        String trafficLightId = "traffic_light_" + upcomingIntersection + "_" + vehicleDirection;
        
        // Verificar el estado del semáforo
        List<TrafficLight> trafficLights = scenarioController.getAllTrafficLights();
        
        for (TrafficLight light : trafficLights) {
            if (light.getId().equals(trafficLightId)) {
                boolean isGreen = light.isGreen();
                
                if (!isGreen) {
                    System.out.println("🚫 SEMÁFORO ROJO: Vehículo " + vehicleId + " debe PARAR antes de " + upcomingIntersection + 
                                     " (semáforo: " + trafficLightId + ") pos:(" + Math.round(currentX) + "," + Math.round(currentY) + ")");
                    return false; // DETENER - semáforo en rojo
                } else {
                    System.out.println("✅ SEMÁFORO VERDE: Vehículo " + vehicleId + " puede entrar a " + upcomingIntersection + 
                                     " (semáforo: " + trafficLightId + ") pos:(" + Math.round(currentX) + "," + Math.round(currentY) + ")");
                    return true; // PUEDE ENTRAR - semáforo en verde
                }
            }
        }
        
        return true; // Si no se encuentra el semáforo, permitir movimiento
    }
    
    /**
     * Determina qué intersección está cerca del vehículo
     */
    private String getNearbyIntersection(double x, double y) {
        double proximityThreshold = 150; // Distancia para considerar "cerca"
        
        // Obtener intersecciones desde el scenario controller
        List<Intersection> intersections = scenarioController.getAllIntersections();
        
        for (Intersection intersection : intersections) {
            double intersectionX = intersection.getPosX() + intersection.getWidth() / 2.0;
            double intersectionY = intersection.getPosY() + intersection.getHeight() / 2.0;
            
            double distance = Math.sqrt(Math.pow(x - intersectionX, 2) + Math.pow(y - intersectionY, 2));
            
            if (distance < proximityThreshold) {
                return intersection.getId();
            }
        }
        
        return null; // No está cerca de ninguna intersección
    }
    
    /**
     * Determina la dirección principal del vehículo (ew para East→West, we para West→East)
     */
    private String getVehicleMainDirection(VehiclePath path) {
        // Obtener waypoints para analizar la dirección general
        if (path.getWaypoints().size() < 2) {
            return "we"; // Por defecto
        }
        
        // ESCENARIO 2: Analizar la dirección INICIAL del vehículo (primeros waypoints)
        // Los primeros waypoints determinan el flujo de semáforo correcto
        VehiclePath.PathPoint first = path.getWaypoints().get(0);
        VehiclePath.PathPoint second = path.getWaypoints().size() > 1 ? path.getWaypoints().get(1) : first;
        
        // Usar los primeros dos waypoints para determinar la dirección inicial
        double initialDeltaX = second.x - first.x;
        
        System.out.println("🔄 ANÁLISIS DIRECCIÓN: Primer waypoint (" + Math.round(first.x) + "," + Math.round(first.y) + 
                         ") → Segundo waypoint (" + Math.round(second.x) + "," + Math.round(second.y) + ")");
        System.out.println("🔄 InitialDeltaX: " + String.format("%.2f", initialDeltaX));
        
        if (initialDeltaX > 5) {
            // Se mueve inicialmente hacia la derecha = West→East
            // Estos vehículos (incluyendo West hacia Sur) usan semáforos "ew" 
            System.out.println("🔄 Vehículo WEST→EAST (incluye giros a Sur/Norte) usa semáforo (ew)");
            return "ew";
        } else if (initialDeltaX < -5) {
            // Se mueve inicialmente hacia la izquierda = East→West  
            // Estos vehículos (incluyendo East hacia Sur/Norte) usan semáforos "we"
            System.out.println("🔄 Vehículo EAST→WEST (incluye giros a Sur/Norte) usa semáforo (we)");
            return "we";
        } else {
            // Sin movimiento horizontal inicial claro - usar posición de inicio
            if (first.x < 400) {
                // Inicia desde el lado izquierdo (West)
                System.out.println("🔄 Vehículo desde WEST (sin deltaX claro) usa semáforo (ew)");
                return "ew";
            } else {
                // Inicia desde el lado derecho (East)
                System.out.println("🔄 Vehículo desde EAST (sin deltaX claro) usa semáforo (we)");
                return "we";
            }
        }
    }
    
    /**
     * Verifica si el vehículo está actualmente dentro de una intersección
     */
    private boolean isVehicleInsideIntersection(double x, double y) {
        List<Intersection> intersections = scenarioController.getAllIntersections();
        
        for (Intersection intersection : intersections) {
            // Solo verificar intersection_2 e intersection_3 (las del escenario 2)
            String intersectionId = intersection.getId();
            if (!intersectionId.equals("intersection_2") && !intersectionId.equals("intersection_3")) {
                continue;
            }
            
            double leftBound = intersection.getPosX();
            double rightBound = intersection.getPosX() + intersection.getWidth();
            double topBound = intersection.getPosY();
            double bottomBound = intersection.getPosY() + intersection.getHeight();
            
            // Verificar si el vehículo está dentro de los límites de la intersección
            if (x >= leftBound && x <= rightBound && y >= topBound && y <= bottomBound) {
                System.out.println("🏁 Vehículo DENTRO de " + intersectionId + " en (" + Math.round(x) + ", " + Math.round(y) + ")");
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determina qué intersección está adelante del vehículo en su ruta
     */
    private String getUpcomingIntersection(String vehicleId, double currentX, double currentY, double nextX, double nextY) {
        // Obtener intersecciones del scenario
        List<Intersection> intersections = scenarioController.getAllIntersections();
        
        // ESCENARIO 2: Los vehículos simplemente detectan si hay una intersección adelante
        double maxDetectionDistance = 100; // Detectar intersecciones cercanas
        
        for (Intersection intersection : intersections) {
            // Solo verificar intersection_2 e intersection_3 (las del escenario 2)
            String intersectionId = intersection.getId();
            if (!intersectionId.equals("intersection_2") && !intersectionId.equals("intersection_3")) {
                continue;
            }
            
            double intersectionX = intersection.getPosX() + intersection.getWidth() / 2.0;
            double intersectionY = intersection.getPosY() + intersection.getHeight() / 2.0;
            
            // Calcular distancia a la intersección
            double distanceToIntersection = Math.sqrt(Math.pow(currentX - intersectionX, 2) + Math.pow(currentY - intersectionY, 2));
            
            // Para vehículos East→West: verificar si la intersección está adelante (X menor)
            // Para vehículos West→East: verificar si la intersección está adelante (X mayor)
            boolean isAhead = false;
            
            if (nextX > currentX) {
                // Vehículo se mueve hacia la derecha (West→East)
                isAhead = intersectionX > currentX;
            } else if (nextX < currentX) {
                // Vehículo se mueve hacia la izquierda (East→West)
                isAhead = intersectionX < currentX;
            }
            
            // Si la intersección está adelante y cerca, reportarla
            if (isAhead && distanceToIntersection <= maxDetectionDistance) {
                System.out.println("🚦 INTERSECCIÓN ADELANTE: Vehículo " + vehicleId + " detecta " + intersectionId + 
                                 " a " + Math.round(distanceToIntersection) + "px - verificando semáforo");
                return intersectionId;
            }
        }
        
        return null;
    }
    
    /**
     * NUEVO: Detecta si hay una intersección INMEDIATAMENTE adelante del vehículo
     * Distancia consistente y precisa para todos los casos
     */
    private String getImmediateIntersectionAhead(String vehicleId, double currentX, double currentY, double nextX, double nextY) {
        List<Intersection> intersections = scenarioController.getAllIntersections();
        
        // ESCENARIO 2: Los vehículos deben llegar AL BORDE de la intersección antes de detenerse
        final double DETECTION_DISTANCE = 200.0; // Detectar desde lejos
        final double STOP_DISTANCE = 15.0; // Detenerse muy cerca del borde
        
        System.out.println("🔍 BUSCANDO intersección para vehículo " + vehicleId + " en (" + Math.round(currentX) + "," + Math.round(currentY) + ")");
        
        for (Intersection intersection : intersections) {
            // Solo verificar intersection_2 e intersection_3 (las del escenario 2)
            String intersectionId = intersection.getId();
            if (!intersectionId.equals("intersection_2") && !intersectionId.equals("intersection_3")) {
                continue;
            }
            
            // Coordenadas del BORDE de la intersección (no centro)
            double intersectionLeft = intersection.getPosX();
            double intersectionRight = intersection.getPosX() + intersection.getWidth();
            double intersectionTop = intersection.getPosY();
            double intersectionBottom = intersection.getPosY() + intersection.getHeight();
            
            // Calcular distancia al BORDE más cercano según la dirección de movimiento
            double distanceToBorder = Double.MAX_VALUE;
            double deltaX = nextX - currentX;
            double deltaY = nextY - currentY;
            
            System.out.println("    🎯 " + intersectionId + " bounds: (" + Math.round(intersectionLeft) + "," + Math.round(intersectionTop) + 
                             ") a (" + Math.round(intersectionRight) + "," + Math.round(intersectionBottom) + ")");
            System.out.println("    ➡️ Movimiento: deltaX=" + String.format("%.2f", deltaX) + ", deltaY=" + String.format("%.2f", deltaY));
            
            // Determinar qué borde es relevante según la dirección de movimiento
            boolean isMovingTowardIntersection = false;
            
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                // Movimiento principalmente horizontal
                if (deltaX > 0) {
                    // Moviéndose hacia la derecha (West→East)
                    if (currentX < intersectionLeft) {
                        distanceToBorder = intersectionLeft - currentX;
                        isMovingTowardIntersection = true;
                        System.out.println("    🔄 WEST→EAST: distancia al borde izquierdo = " + Math.round(distanceToBorder) + "px");
                    }
                } else if (deltaX < 0) {
                    // Moviéndose hacia la izquierda (East→West)
                    if (currentX > intersectionRight) {
                        distanceToBorder = currentX - intersectionRight;
                        isMovingTowardIntersection = true;
                        System.out.println("    🔄 EAST→WEST: distancia al borde derecho = " + Math.round(distanceToBorder) + "px");
                    }
                }
            } else {
                // Movimiento principalmente vertical (giros hacia Norte/Sur)
                if (deltaY > 0) {
                    // Moviéndose hacia abajo (hacia Sur)
                    if (currentY < intersectionTop) {
                        distanceToBorder = intersectionTop - currentY;
                        isMovingTowardIntersection = true;
                        System.out.println("    🔄 HACIA SUR: distancia al borde superior = " + Math.round(distanceToBorder) + "px");
                    }
                } else if (deltaY < 0) {
                    // Moviéndose hacia arriba (hacia Norte)
                    if (currentY > intersectionBottom) {
                        distanceToBorder = currentY - intersectionBottom;
                        isMovingTowardIntersection = true;
                        System.out.println("    🔄 HACIA NORTE: distancia al borde inferior = " + Math.round(distanceToBorder) + "px");
                    }
                }
            }
            
            // Si se está moviendo hacia la intersección y está dentro del rango
            if (isMovingTowardIntersection && distanceToBorder <= DETECTION_DISTANCE && distanceToBorder > STOP_DISTANCE) {
                System.out.println("    ✅ Puede continuar - aún lejos del borde (" + Math.round(distanceToBorder) + "px > " + STOP_DISTANCE + "px)");
                continue; // Puede continuar moviéndose hacia la intersección
            } else if (isMovingTowardIntersection && distanceToBorder <= STOP_DISTANCE) {
                System.out.println("⚠️ INTERSECCIÓN EN EL BORDE: Vehículo " + vehicleId + 
                                 " a " + Math.round(distanceToBorder) + "px del borde de " + intersectionId + 
                                 " - VERIFICANDO SEMÁFORO");
                return intersectionId;
            }
        }
        
        System.out.println("❌ NO hay intersección en el borde para vehículo " + vehicleId);
        return null; // No hay intersección en el borde
    }
    
    /**
     * Calcula la distancia exacta a una intersección específica
     */
    private double getDistanceToIntersection(String intersectionId, double vehicleX, double vehicleY) {
        List<Intersection> intersections = scenarioController.getAllIntersections();
        
        for (Intersection intersection : intersections) {
            if (intersection.getId().equals(intersectionId)) {
                double intersectionX = intersection.getPosX() + intersection.getWidth() / 2.0;
                double intersectionY = intersection.getPosY() + intersection.getHeight() / 2.0;
                
                return Math.sqrt(Math.pow(vehicleX - intersectionX, 2) + Math.pow(vehicleY - intersectionY, 2));
            }
        }
        
        return Double.MAX_VALUE; // Si no encuentra la intersección, retornar distancia máxima
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
     * CORREGIDO: Obtiene las calles de entrada EXACTAS que usa el sistema manual
     * Escenario 1: calles con "entrada"
     * Escenario 2: SOLO las calles específicas que usa el manual
     */
    private List<Street> getEntryStreets() {
        List<Street> allStreets = scenarioController.getAllStreets();
        
        // Detectar si es Escenario 1 o 2
        boolean hasEntradaSalida = allStreets.stream()
            .anyMatch(street -> street.getId().contains("entrada"));
        
        if (hasEntradaSalida) {
            // Escenario 1: usar calles con "entrada"
            return allStreets.stream()
                .filter(street -> street.getId().contains("entrada"))
                .toList();
        } else {
            // ESCENARIO 2: Usar EXACTAMENTE las mismas calles que el sistema manual
            List<String> manualEntryStreetIds = List.of(
                "east_center_lane_segment1",
                "east_left_lane_segment1", 
                "east_right_lane_segment1",
                "west_center_lane_segment3",
                "west_left_lane_segment3",
                "west_right_lane_segment3"
            );
            
            List<Street> entryStreets = new ArrayList<>();
            for (String streetId : manualEntryStreetIds) {
                allStreets.stream()
                    .filter(street -> street.getId().equals(streetId))
                    .findFirst()
                    .ifPresent(entryStreets::add);
            }
            
            System.out.println("🚗 AUTO-SPAWN: Usando " + entryStreets.size() + " calles de entrada específicas del manual");
            return entryStreets;
        }
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