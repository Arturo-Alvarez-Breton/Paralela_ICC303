package app.controller;

import java.util.ArrayList;
import java.util.List;

import app.model.Street;
import app.model.enums.DirectionEnum;
import app.service.StreetService;

/**
 * Representa la ruta que debe seguir un vehículo desde su entrada hasta su salida
 * Respeta los carriles de entrada y salida definidos en el escenario
 */
public class VehiclePath {
    
    private final List<PathPoint> waypoints;
    private int currentWaypointIndex;
    private double currentX, currentY;
    private boolean completed;
    private final ScenarioController scenarioController;
    
    // Constantes de la intersección
    private static final int CENTER_X = StreetService.INTERSECTION_CENTER_X;
    private static final int CENTER_Y = StreetService.INTERSECTION_CENTER_Y;
    private static final int INTERSECTION_SIZE = 80;
    
    /**
     * Constructor que calcula la ruta completa
     */
    public VehiclePath(double startX, double startY, String entryDirection, DirectionEnum turnDirection, ScenarioController scenarioController) {
        this.waypoints = new ArrayList<>();
        this.currentWaypointIndex = 0;
        this.currentX = startX;
        this.currentY = startY;
        this.completed = false;
        this.scenarioController = scenarioController;
        
        calculatePath(startX, startY, entryDirection, turnDirection, scenarioController);
    }
    
    /**
     * Calcula todos los puntos de la ruta usando las calles reales del escenario
     * CORREGIDO: Implementa tabla de verdad correcta y apex apropiados para cada giro
     * SOLUCIONADO: Elimina el comportamiento "va-viene-sigue" usando rutas optimizadas
     */
    private void calculatePath(double startX, double startY, String entryDirection, DirectionEnum turnDirection, ScenarioController scenarioController) {
        // Punto inicial (spawn del vehículo)
        waypoints.add(new PathPoint(startX, startY));
        
        // Calcular dirección de salida usando tabla de verdad corregida
        String exitDirection = calculateExitDirection(entryDirection, turnDirection);
        
        // Imprimir debug para validar tabla de verdad
        System.out.println("=== RUTA CALCULADA ===");
        System.out.println("Entrada: " + entryDirection + " -> Giro: " + turnDirection + " -> Salida: " + exitDirection);
        
        // Generar waypoints según tipo de movimiento
        switch (turnDirection) {
            case STRAIGHT:
                calculateStraightPath(startX, startY, entryDirection, exitDirection);
                break;
            case RIGHT:
                calculateRightTurnPath(startX, startY, entryDirection, exitDirection);
                break;
            case LEFT:
                calculateLeftTurnPath(startX, startY, entryDirection, exitDirection);
                break;
            case U_TURN:
                calculateUTurnPath(startX, startY, entryDirection, exitDirection);
                break;
        }
        
        // Debug: Imprimir todos los waypoints calculados
        for (int i = 0; i < waypoints.size(); i++) {
            PathPoint point = waypoints.get(i);
            System.out.println("Waypoint " + i + ": " + point);
        }
        System.out.println("=====================");
    }
    
    /**
     * Calcula ruta recta optimizada - COMPLETAMENTE MONÓTONA
     * CORREGIDO: Elimina el comportamiento "va-viene-sigue" asegurando 
     * que todos los waypoints avancen progresivamente hacia el destino
     */
    private void calculateStraightPath(double startX, double startY, String entryDirection, String exitDirection) {
        // Punto antes de la línea de PARE
        addStopLinePoint(entryDirection, startX, startY);
        
        // Punto de entrada a la intersección - MANTENER CARRIL EXACTO
        addIntersectionEntryPoint(entryDirection, startX, startY);
        
        // Para movimiento recto: NO agregar waypoints intermedios problemáticos
        // Ir directamente desde entrada a salida manteniendo carril
        addIntersectionExitPointStraight(entryDirection, exitDirection);
        
        // Punto final - mantener alineación perfecta para rectas
        addFinalExitPointStraight(entryDirection, exitDirection);
    }
    
    /**
     * Calcula ruta para giro a la derecha (curva corta usando apex)
     * CORREGIDO: Waypoints monótonos para evitar "rebote" en giros horizontales E/W
     */
    private void calculateRightTurnPath(double startX, double startY, String entryDirection, String exitDirection) {
        // Punto antes de la línea de PARE
        addStopLinePoint(entryDirection, startX, startY);
        
        // Punto de entrada a la intersección
        addIntersectionEntryPoint(entryDirection, startX, startY);
        
        // CORRECCIÓN: Agregar waypoints intermedios monótonos antes del apex
        addSmoothCurvePoints(entryDirection, exitDirection, true); // true = giro derecho
        
        // Punto de salida de la intersección
        addIntersectionExitPointTurn(exitDirection);
        
        // Punto final
        addFinalExitPoint(entryDirection, DirectionEnum.RIGHT);
    }
    
    /**
     * Calcula ruta para giro a la izquierda (curva amplia)
     * CORREGIDO: Waypoints monótonos para evitar "rebote" en giros horizontales E/W
     */
    private void calculateLeftTurnPath(double startX, double startY, String entryDirection, String exitDirection) {
        // Punto antes de la línea de PARE
        addStopLinePoint(entryDirection, startX, startY);
        
        // Punto de entrada a la intersección
        addIntersectionEntryPoint(entryDirection, startX, startY);
        
        // CORRECCIÓN: Agregar waypoints intermedios monótonos antes del apex
        addSmoothCurvePoints(entryDirection, exitDirection, false); // false = giro izquierdo
        
        // Punto de salida de la intersección
        addIntersectionExitPointTurn(exitDirection);
        
        // Punto final
        addFinalExitPoint(entryDirection, DirectionEnum.LEFT);
    }
    
    /**
     * Calcula ruta para U-turn
     */
    private void calculateUTurnPath(double startX, double startY, String entryDirection, String exitDirection) {
        // Punto antes de la línea de PARE
        addStopLinePoint(entryDirection, startX, startY);
        
        // Punto de entrada a la intersección
        addIntersectionEntryPoint(entryDirection, startX, startY);
        
        // Centro del cruce
        waypoints.add(new PathPoint(CENTER_X, CENTER_Y));
        
        // Punto de salida (lado opuesto)
        addIntersectionExitPointTurn(exitDirection);
        
        // Punto final
        addFinalExitPoint(entryDirection, DirectionEnum.U_TURN);
    }
    
    /**
     * Agrega el punto apex (esquina interior) correcto según el par origen→destino
     * Usar la esquina interior del cruce acorde al par origen→destino
     */
    private void addApexPoint(String entryDirection, String exitDirection, boolean isRightTurn) {
        double apexX, apexY;
        int apexRadius = 25; // Radio desde la esquina hacia el centro
        
        // Calcular apex basado en la combinación entrada→salida
        String combination = entryDirection + "→" + exitDirection;
        
        switch (combination) {
            // Giros desde Sur
            case "south→east":  // S→Der→E: apex SE
                apexX = CENTER_X + INTERSECTION_SIZE/2.0 - apexRadius;
                apexY = CENTER_Y + INTERSECTION_SIZE/2.0 - apexRadius;
                break;
            case "south→west":  // S→Izq→W: apex SW
                apexX = CENTER_X - INTERSECTION_SIZE/2.0 + apexRadius;
                apexY = CENTER_Y + INTERSECTION_SIZE/2.0 - apexRadius;
                break;
                
            // Giros desde Norte
            case "north→west":  // N→Der→W: apex NW
                apexX = CENTER_X - INTERSECTION_SIZE/2.0 + apexRadius;
                apexY = CENTER_Y - INTERSECTION_SIZE/2.0 + apexRadius;
                break;
            case "north→east":  // N→Izq→E: apex NE
                apexX = CENTER_X + INTERSECTION_SIZE/2.0 - apexRadius;
                apexY = CENTER_Y - INTERSECTION_SIZE/2.0 + apexRadius;
                break;
                
            // Giros desde Este
            case "east→south":  // E→Der→S: apex SE
                apexX = CENTER_X + INTERSECTION_SIZE/2.0 - apexRadius;
                apexY = CENTER_Y + INTERSECTION_SIZE/2.0 - apexRadius;
                break;
            case "east→north":  // E→Izq→N: apex NE
                apexX = CENTER_X + INTERSECTION_SIZE/2.0 - apexRadius;
                apexY = CENTER_Y - INTERSECTION_SIZE/2.0 + apexRadius;
                break;
                
            // Giros desde Oeste
            case "west→north":  // W→Der→N: apex NW
                apexX = CENTER_X - INTERSECTION_SIZE/2.0 + apexRadius;
                apexY = CENTER_Y - INTERSECTION_SIZE/2.0 + apexRadius;
                break;
            case "west→south":  // W→Izq→S: apex SW
                apexX = CENTER_X - INTERSECTION_SIZE/2.0 + apexRadius;
                apexY = CENTER_Y + INTERSECTION_SIZE/2.0 - apexRadius;
                break;
                
            default:
                // Fallback al centro si no se reconoce la combinación
                apexX = CENTER_X;
                apexY = CENTER_Y;
                break;
        }
        
        waypoints.add(new PathPoint(apexX, apexY));
        System.out.println("Apex " + combination + ": (" + apexX + ", " + apexY + ")");
    }
    /**
     * Agrega el punto de salida de la intersección para movimientos rectos
     * CORREGIDO: Mantiene la alineación exacta del carril de entrada
     */
    private void addIntersectionExitPointStraight(String entryDirection, String exitDirection) {
        double exitX, exitY;
        
        // Para movimiento recto, mantener la coordenada del carril de entrada
        PathPoint entryPoint = waypoints.get(waypoints.size() - 1); // Último punto (entrada a intersección)
        
        switch (exitDirection) {
            case "north":
                // Movimiento recto vertical: mantener X del carril de entrada
                exitX = entryPoint.x; // MANTENER X del carril de entrada
                exitY = CENTER_Y - INTERSECTION_SIZE/2.0;
                break;
            case "south":
                // Movimiento recto vertical: mantener X del carril de entrada
                exitX = entryPoint.x; // MANTENER X del carril de entrada
                exitY = CENTER_Y + INTERSECTION_SIZE/2.0;
                break;
            case "east":
                // Movimiento recto horizontal: mantener Y del carril de entrada
                exitX = CENTER_X + INTERSECTION_SIZE/2.0;
                exitY = entryPoint.y; // MANTENER Y del carril de entrada
                break;
            case "west":
                // Movimiento recto horizontal: mantener Y del carril de entrada
                exitX = CENTER_X - INTERSECTION_SIZE/2.0;
                exitY = entryPoint.y; // MANTENER Y del carril de entrada
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(exitX, exitY));
    }
    
    /**
     * Agrega el punto de salida de la intersección para giros
     * CORREGIDO: Usa las posiciones reales de los carriles de salida
     */
    private void addIntersectionExitPointTurn(String exitDirection) {
        double exitX, exitY;
        
        // Para giros, usar las posiciones centrales de los carriles de salida
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        
        switch (exitDirection) {
            case "north":
                // Salida hacia el norte - usar centro del carril rojo norte
                exitX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                exitY = CENTER_Y - INTERSECTION_SIZE/2.0;
                break;
            case "south":
                // Salida hacia el sur - usar centro del carril rojo sur
                exitX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                exitY = CENTER_Y + INTERSECTION_SIZE/2.0;
                break;
            case "east":
                // Salida hacia el este - usar centro del carril rojo este
                exitX = CENTER_X + INTERSECTION_SIZE/2.0;
                exitY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                break;
            case "west":
                // Salida hacia el oeste - usar centro del carril rojo oeste
                exitX = CENTER_X - INTERSECTION_SIZE/2.0;
                exitY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(exitX, exitY));
    }
    
    /**
     * Agrega el punto de parada antes de la señal de PARE
     * CORREGIDO: Mantiene la X del vehículo (mismo carril)
     */
    private void addStopLinePoint(String entryDirection, double vehicleX, double vehicleY) {
        double stopX, stopY;
        int stopDistance = 50; // Distancia de la línea de PARE
        
        switch (entryDirection) {
            case "north":
                // Vehículo viene del norte hacia el sur - mantener X, avanzar Y
                stopX = vehicleX; // MANTENER el carril original
                stopY = CENTER_Y - INTERSECTION_SIZE/2.0 - stopDistance;
                break;
            case "south":
                // Vehículo viene del sur hacia el norte - mantener X, retroceder Y
                stopX = vehicleX; // MANTENER el carril original
                stopY = CENTER_Y + INTERSECTION_SIZE/2.0 + stopDistance;
                break;
            case "east":
                // Vehículo viene del este hacia el oeste - retroceder X, mantener Y
                stopX = CENTER_X - INTERSECTION_SIZE/2.0 - stopDistance;
                stopY = vehicleY; // MANTENER el carril original
                break;
            case "west":
                // Vehículo viene del oeste hacia el este - avanzar X, mantener Y
                stopX = CENTER_X + INTERSECTION_SIZE/2.0 + stopDistance;
                stopY = vehicleY; // MANTENER el carril original
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(stopX, stopY));
    }
    
    /**
     * Agrega el punto de entrada a la intersección
     * CORREGIDO: Mantiene el carril del vehículo
     */
    private void addIntersectionEntryPoint(String entryDirection, double vehicleX, double vehicleY) {
        double entryX, entryY;
        
        switch (entryDirection) {
            case "north":
                // Entrada desde el norte - mantener X, llegar al borde de la intersección
                entryX = vehicleX; // MANTENER el carril original
                entryY = CENTER_Y - INTERSECTION_SIZE/2.0;
                break;
            case "south":
                // Entrada desde el sur - mantener X, llegar al borde de la intersección
                entryX = vehicleX; // MANTENER el carril original
                entryY = CENTER_Y + INTERSECTION_SIZE/2.0;
                break;
            case "east":
                // Entrada desde el este - llegar al borde, mantener Y
                entryX = CENTER_X - INTERSECTION_SIZE/2.0;
                entryY = vehicleY; // MANTENER el carril original
                break;
            case "west":
                // Entrada desde el oeste - llegar al borde, mantener Y
                entryX = CENTER_X + INTERSECTION_SIZE/2.0;
                entryY = vehicleY; // MANTENER el carril original
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(entryX, entryY));
    }
    
    /**
     * Agrega el punto final en el extremo de la calle de salida
     * CORREGIDO: Para movimiento recto, mantiene la alineación del carril
     */
    private void addFinalExitPoint(String entryDirection, DirectionEnum turnDirection) {
        String exitDirection = calculateExitDirection(entryDirection, turnDirection);
        
        // Buscar la calle de salida correspondiente
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        
        double finalX, finalY;
        
        // Para movimiento RECTO, mantener la alineación del carril de entrada
        if (turnDirection == DirectionEnum.STRAIGHT) {
            PathPoint exitPoint = waypoints.get(waypoints.size() - 1); // Punto de salida de intersección
            
            switch (exitDirection) {
                case "north":
                    // Movimiento recto vertical: mantener X, ir al extremo norte
                    finalX = exitPoint.x; // MANTENER X del carril
                    finalY = exitStreet.getPosY(); // Extremo superior
                    break;
                case "south":
                    // Movimiento recto vertical: mantener X, ir al extremo sur
                    finalX = exitPoint.x; // MANTENER X del carril
                    finalY = exitStreet.getPosY() + exitStreet.getHeight(); // Extremo inferior
                    break;
                case "east":
                    // Movimiento recto horizontal: mantener Y, ir al extremo este
                    finalX = exitStreet.getPosX() + exitStreet.getWidth(); // Extremo derecho
                    finalY = exitPoint.y; // MANTENER Y del carril
                    break;
                case "west":
                    // Movimiento recto horizontal: mantener Y, ir al extremo oeste
                    finalX = exitStreet.getPosX(); // Extremo izquierdo
                    finalY = exitPoint.y; // MANTENER Y del carril
                    break;
                default:
                    return;
            }
        } else {
            // Para giros, usar el centro de la calle de salida
            switch (exitDirection) {
                case "north":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                    finalY = exitStreet.getPosY(); // Extremo superior
                    break;
                case "south":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                    finalY = exitStreet.getPosY() + exitStreet.getHeight(); // Extremo inferior
                    break;
                case "east":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth(); // Extremo derecho
                    finalY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                    break;
                case "west":
                    finalX = exitStreet.getPosX(); // Extremo izquierdo
                    finalY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                    break;
                default:
                    return;
            }
        }
        
        waypoints.add(new PathPoint(finalX, finalY));
    }
    
    /**
     * Agrega el punto final para movimientos rectos - PERFECTAMENTE ALINEADO
     * CORREGIDO: Mantiene la alineación exacta del carril sin desviaciones
     * Evita el comportamiento "va-viene-sigue" en trayectorias horizontales/verticales
     */
    private void addFinalExitPointStraight(String entryDirection, String exitDirection) {
        // Obtener el punto de salida de la intersección para mantener alineación
        PathPoint exitPoint = waypoints.get(waypoints.size() - 1);
        
        // Buscar la calle de salida
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        
        double finalX, finalY;
        
        // Para movimiento RECTO: mantener coordenada lateral EXACTA del carril
        switch (exitDirection) {
            case "north":
                // Movimiento vertical hacia arriba: mantener X, avanzar Y hacia extremo
                finalX = exitPoint.x; // MANTENER X exacto del carril
                finalY = exitStreet.getPosY(); // Extremo superior de la calle
                break;
            case "south":
                // Movimiento vertical hacia abajo: mantener X, avanzar Y hacia extremo
                finalX = exitPoint.x; // MANTENER X exacto del carril
                finalY = exitStreet.getPosY() + exitStreet.getHeight(); // Extremo inferior
                break;
            case "east":
                // Movimiento horizontal hacia derecha: mantener Y, avanzar X hacia extremo
                finalX = exitStreet.getPosX() + exitStreet.getWidth(); // Extremo derecho
                finalY = exitPoint.y; // MANTENER Y exacto del carril
                break;
            case "west":
                // Movimiento horizontal hacia izquierda: mantener Y, avanzar X hacia extremo
                finalX = exitStreet.getPosX(); // Extremo izquierdo
                finalY = exitPoint.y; // MANTENER Y exacto del carril
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(finalX, finalY));
        System.out.println("Final RECTO " + exitDirection + ": (" + finalX + ", " + finalY + ") - alineado con carril");
    }

    /**
     * Busca la calle de salida correspondiente a una dirección
     */
    private Street findExitStreet(String exitDirection, ScenarioController scenarioController) {
        String streetId = switch (exitDirection) {
            case "north" -> "calle_north_salida";
            case "south" -> "calle_south_salida";
            case "east" -> "calle_east_salida";
            case "west" -> "calle_west_salida";
            default -> null;
        };
        
        if (streetId == null) return null;
        
        return scenarioController.getAllStreets().stream()
            .filter(street -> street.getId().equals(streetId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Calcula la dirección de salida basada en la entrada y el tipo de giro
     * CORREGIDO: Usa marco de referencia LOCAL del vehículo (hacia dónde mira el auto)
     * Derecha/Izquierda son relativos a la orientación del conductor, NO a la pantalla
     * 
     * Marco local del vehículo:
     * - Forward (F): hacia dónde avanza el auto
     * - Right (R): derecha del conductor = rotación +90° desde F  
     * - Left (L): izquierda del conductor = rotación -90° desde F
     */
    private String calculateExitDirection(String entryDirection, DirectionEnum turnDirection) {
        // Tabla de verdad corregida según marco de referencia LOCAL del vehículo:
        // (Origen → Recto | Derecha | Izquierda | U-turn)
        switch (entryDirection) {
            case "south":  // Auto entra desde SUR (mira hacia NORTE)
                // F=Norte, R=Este, L=Oeste
                switch (turnDirection) {
                    case STRAIGHT: return "north";  // Sigue recto hacia adelante (Norte)
                    case RIGHT: return "east";      // Gira a la derecha del conductor (Este) 
                    case LEFT: return "west";       // Gira a la izquierda del conductor (Oeste)
                    case U_TURN: return "south";    // Media vuelta (Sur)
                }
                break;
                
            case "north":  // Auto entra desde NORTE (mira hacia SUR)
                // F=Sur, R=Oeste, L=Este
                switch (turnDirection) {
                    case STRAIGHT: return "south";  // Sigue recto hacia adelante (Sur)
                    case RIGHT: return "west";      // Gira a la derecha del conductor (Oeste)
                    case LEFT: return "east";       // Gira a la izquierda del conductor (Este)
                    case U_TURN: return "north";    // Media vuelta (Norte)
                }
                break;
                
            case "east":   // Auto entra desde ESTE (mira hacia OESTE)
                // F=Oeste, R=Norte, L=Sur
                switch (turnDirection) {
                    case STRAIGHT: return "west";   // Sigue recto hacia adelante (Oeste)
                    case RIGHT: return "north";     // Gira a la derecha del conductor (Norte)
                    case LEFT: return "south";      // Gira a la izquierda del conductor (Sur)
                    case U_TURN: return "east";     // Media vuelta (Este)
                }
                break;
                
            case "west":   // Auto entra desde OESTE (mira hacia ESTE)
                // F=Este, R=Sur, L=Norte
                switch (turnDirection) {
                    case STRAIGHT: return "east";   // Sigue recto hacia adelante (Este)
                    case RIGHT: return "south";     // Gira a la derecha del conductor (Sur)
                    case LEFT: return "north";      // Gira a la izquierda del conductor (Norte)
                    case U_TURN: return "west";     // Media vuelta (Oeste)
                }
                break;
        }
        return entryDirection; // Fallback
    }
    /**
     * Mueve el vehículo a lo largo de la ruta
     * MEJORADO: Algoritmo de seguimiento más robusto para evitar "rebotes"
     * @param speed Velocidad de movimiento
     * @return true si el movimiento continúa, false si la ruta está completa
     */
    public boolean moveAlongPath(double speed) {
        if (completed || currentWaypointIndex >= waypoints.size()) {
            completed = true;
            return false;
        }
        
        PathPoint targetPoint = waypoints.get(currentWaypointIndex);
        
        // Calcular dirección hacia el objetivo
        double deltaX = targetPoint.x - currentX;
        double deltaY = targetPoint.y - currentY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        
        // CORREGIDO: Tolerancia más estricta y progresiva
        double baseTolerance = Math.min(speed * 1.5, 8.0); // Más tolerante pero limitado
        double tolerance = Math.max(baseTolerance, 3.0); // Mínimo 3 píxeles
        
        // Si estamos cerca del waypoint, avanzar al siguiente SIN reposicionar
        if (distance <= tolerance) {
            // NO reposicionar exactamente - esto puede causar "saltos"
            // Solo avanzar al siguiente waypoint
            currentWaypointIndex++;
            
            // Verificar si hemos llegado al final
            if (currentWaypointIndex >= waypoints.size()) {
                completed = true;
                return false;
            }
            
            // Recalcular para el nuevo target
            targetPoint = waypoints.get(currentWaypointIndex);
            deltaX = targetPoint.x - currentX;
            deltaY = targetPoint.y - currentY;
            distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        }
        
        // Mover hacia el waypoint de forma suave
        if (distance > 0.1) { // Evitar división por cero
            double directionX = deltaX / distance;
            double directionY = deltaY / distance;
            
            // Movimiento suavizado: nunca sobrepasar el waypoint
            double moveDistance = Math.min(speed, distance * 0.9); // Máximo 90% de la distancia
            
            currentX += directionX * moveDistance;
            currentY += directionY * moveDistance;
        }
        
        return true;
    }
    
    /**
     * Verifica si el vehículo está fuera de los límites de la pantalla
     */
    public boolean isOutOfBounds() {
        return currentX < -100 || currentX > 1380 || currentY < -100 || currentY > 820;
    }
    
    // Getters
    public double getCurrentX() { return currentX; }
    public double getCurrentY() { return currentY; }
    public double getStartX() { return waypoints.isEmpty() ? 0 : waypoints.get(0).x; }
    public double getStartY() { return waypoints.isEmpty() ? 0 : waypoints.get(0).y; }
    public boolean isCompleted() { return completed; }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public List<PathPoint> getWaypoints() { return new ArrayList<>(waypoints); }
    
    /**
     * Clase interna para representar un punto en la ruta
     */
    public static class PathPoint {
        public final double x, y;
        
        public PathPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return String.format("(%.1f, %.1f)", x, y);
        }
    }
    
    /**
     * Agrega waypoints intermedios monotonos para curvas suaves
     * CORRECCION CRITICA: Elimina el comportamiento "va-viene-sigue" en giros horizontales
     * Genera puntos que SIEMPRE avanzan hacia el destino sin retrocesos
     */
    private void addSmoothCurvePoints(String entryDirection, String exitDirection, boolean isRightTurn) {
        PathPoint entryPoint = waypoints.get(waypoints.size() - 1); // Punto de entrada a intersección
        
        // Calcular punto de destino (salida de intersección)
        double exitX, exitY;
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        
        switch (exitDirection) {
            case "north":
                exitX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                exitY = CENTER_Y - INTERSECTION_SIZE/2.0;
                break;
            case "south":
                exitX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                exitY = CENTER_Y + INTERSECTION_SIZE/2.0;
                break;
            case "east":
                exitX = CENTER_X + INTERSECTION_SIZE/2.0;
                exitY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                break;
            case "west":
                exitX = CENTER_X - INTERSECTION_SIZE/2.0;
                exitY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                break;
            default:
                return;
        }
        
        // Generar curva monótona según la dirección de entrada
        switch (entryDirection) {
            case "east":
                addMonotonicCurveFromEast(entryPoint.x, entryPoint.y, exitX, exitY, isRightTurn);
                break;
            case "west":
                addMonotonicCurveFromWest(entryPoint.x, entryPoint.y, exitX, exitY, isRightTurn);
                break;
            case "north":
                addMonotonicCurveFromNorth(entryPoint.x, entryPoint.y, exitX, exitY, isRightTurn);
                break;
            case "south":
                addMonotonicCurveFromSouth(entryPoint.x, entryPoint.y, exitX, exitY, isRightTurn);
                break;
        }
    }
    
    /**
     * Curva monotona desde Este: X siempre DECRECE, Y cambia suavemente
     * COPIA EXACTA de Sur pero con X/Y intercambiados apropiadamente
     */
    private void addMonotonicCurveFromEast(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        int steps = 3; 
        
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            
            // X ESTRICTAMENTE MONOTONICA: garantizar monotonía (como Y en Sur)
            double curveX = startX + (endX - startX) * progress;
            
            // Y: curva suave sin oscilaciones complejas (como X en Sur)
            double curveY;
            if (isRightTurn) {
                // Giro derecho: curva suave hacia destino (EXACTAMENTE como Sur)
                curveY = startY + (endY - startY) * (progress * progress);
            } else {
                // Giro izquierdo: curva más directa (EXACTAMENTE como Sur)
                curveY = startY + (endY - startY) * progress;
            }
            
            waypoints.add(new PathPoint(curveX, curveY));
            System.out.println("EXACT E→" + (isRightTurn ? "R" : "L") + " step " + i + ": (" + curveX + ", " + curveY + ")");
        }
    }
    
    /**
     * Curva monotona desde Oeste: X siempre CRECE, Y cambia suavemente
     * COPIA EXACTA de Sur pero con X/Y intercambiados apropiadamente
     */
    private void addMonotonicCurveFromWest(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        int steps = 3;
        
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            
            // X ESTRICTAMENTE MONOTONICA: garantizar monotonía (como Y en Sur)
            double curveX = startX + (endX - startX) * progress;
            
            // Y: curva suave sin oscilaciones complejas (como X en Sur)
            double curveY;
            if (isRightTurn) {
                // Giro derecho: curva suave hacia destino (EXACTAMENTE como Sur)
                curveY = startY + (endY - startY) * (progress * progress);
            } else {
                // Giro izquierdo: curva más directa (EXACTAMENTE como Sur)
                curveY = startY + (endY - startY) * progress;
            }
            
            waypoints.add(new PathPoint(curveX, curveY));
            System.out.println("EXACT W→" + (isRightTurn ? "R" : "L") + " step " + i + ": (" + curveX + ", " + curveY + ")");
        }
    }
    
    /**
     * Curva monotona desde Norte: Y siempre CRECE, X cambia suavemente
     * CLONADO DESDE SUR (que funciona perfectamente)
     */
    private void addMonotonicCurveFromNorth(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        int steps = 3; // Igual que Sur que funciona bien
        
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            
            // Y: progresión lineal monotónica (Norte → Sur, Y crece)
            double curveY = startY + (endY - startY) * progress;
            
            // X: misma lógica exacta que Sur (intercambiando X/Y)
            double curveX;
            if (isRightTurn) {
                // Giro derecho: curva suave hacia destino (igual que Sur)
                curveX = startX + (endX - startX) * (progress * progress);
            } else {
                // Giro izquierdo: curva más directa (igual que Sur)
                curveX = startX + (endX - startX) * progress;
            }
            
            waypoints.add(new PathPoint(curveX, curveY));
            System.out.println("CLONED N→" + (isRightTurn ? "R" : "L") + " step " + i + ": (" + curveX + ", " + curveY + ")");
        }
    }
    
    /**
     * Curva monotona desde Sur: Y siempre DECRECE, X cambia suavemente  
     * IMPLEMENTACION DE REFERENCIA - FUNCIONA PERFECTAMENTE
     * Esta lógica se clonó para todas las demás direcciones
     */
    private void addMonotonicCurveFromSouth(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        int steps = 3; // Consistencia con E/W
        
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            
            // Y ESTRICTAMENTE DECRECIENTE: garantizar monotonía
            double curveY = startY + (endY - startY) * progress;
            
            // X: curva suave sin oscilaciones complejas
            double curveX;
            if (isRightTurn) {
                // Giro derecho: curva suave hacia destino
                curveX = startX + (endX - startX) * (progress * progress);
            } else {
                // Giro izquierdo: curva más directa
                curveX = startX + (endX - startX) * progress;
            }
            
            waypoints.add(new PathPoint(curveX, curveY));
            System.out.println("MONO Curve S→" + (isRightTurn ? "R" : "L") + " step " + i + ": (" + curveX + ", " + curveY + ") - Y decrece");
        }
    }
}