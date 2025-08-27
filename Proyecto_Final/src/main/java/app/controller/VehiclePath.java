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
    
    // Para debug: almacenar la dirección de entrada
    private final String entryDirection;
    
    // Contador para logs cada 5 ticks
    private int tickCounter = 0;
    
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
        
        // Almacenar la dirección de entrada para debug
        this.entryDirection = entryDirection;
        
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
     * CORREGIDO: Elimina waypoints duplicados que causan rebote "va-viene-sigue"
     * FIX CRÍTICO: Para movimientos rectos, solo usar STOP → DESTINO (sin waypoints intermedios)
     */
    private void calculateStraightPath(double startX, double startY, String entryDirection, String exitDirection) {
        System.out.println("=== CALCULATE STRAIGHT PATH ===");
        System.out.println("Start: (" + startX + ", " + startY + ") " + entryDirection + "→" + exitDirection);
        // Para North/South mantenemos la lógica existente (Stop -> Final) que ya funciona
        if (entryDirection.equals("north") || entryDirection.equals("south")) {
            addStopLinePoint(entryDirection, startX, startY);
            PathPoint stopPoint = waypoints.get(waypoints.size() - 1);
            System.out.println("Stop Point: " + stopPoint + " - Y=" + stopPoint.y);
            addFinalExitPointStraight(entryDirection, exitDirection);
            PathPoint finalPoint = waypoints.get(waypoints.size() - 1);
            System.out.println("Final Point: " + finalPoint + " - Y=" + finalPoint.y);
            System.out.println("WAYPOINTS RECTOS (N/S): " + waypoints.size());
        } else { // East / West: adaptamos a patrón simétrico: Stop -> Entry -> Salida intersección -> Final
            addStopLinePoint(entryDirection, startX, startY);
            PathPoint stopPoint = waypoints.get(waypoints.size() - 1);
            System.out.println("Stop Point (E/W): " + stopPoint);
            addIntersectionEntryPoint(entryDirection, stopPoint.x, stopPoint.y);
            addIntersectionExitPointStraight(entryDirection, exitDirection);
            addFinalExitPointStraight(entryDirection, exitDirection);
            PathPoint finalPoint = waypoints.get(waypoints.size() - 1);
            System.out.println("Final Point (E/W): " + finalPoint);
            System.out.println("WAYPOINTS RECTOS (E/W): " + waypoints.size());
        }
        System.out.println("===========================");
    }
    
    /**
     * Calcula ruta para giro a la derecha (curva corta usando apex)
     * REIMPLEMENTADO: Lógica específica para Este/Oeste completamente nueva
     */
    private void calculateRightTurnPath(double startX, double startY, String entryDirection, String exitDirection) {
        // Unificamos: mismo flujo para todas las direcciones (N/S ya funcionaba)
        addStopLinePoint(entryDirection, startX, startY);
        PathPoint stopPoint = waypoints.get(waypoints.size() - 1);
        addIntersectionEntryPoint(entryDirection, stopPoint.x, stopPoint.y);
        addSmoothCurvePoints(entryDirection, exitDirection, true);
        addIntersectionExitPointTurn(exitDirection);
        addFinalExitPoint(entryDirection, DirectionEnum.RIGHT);
    }
    
    /**
     * Calcula ruta para giro a la izquierda (curva amplia)
     * REIMPLEMENTADO: Lógica específica para Este/Oeste completamente nueva
     */
    private void calculateLeftTurnPath(double startX, double startY, String entryDirection, String exitDirection) {
        addStopLinePoint(entryDirection, startX, startY);
        PathPoint stopPoint = waypoints.get(waypoints.size() - 1);
        addIntersectionEntryPoint(entryDirection, stopPoint.x, stopPoint.y);
        addSmoothCurvePoints(entryDirection, exitDirection, false);
        addIntersectionExitPointTurn(exitDirection);
        addFinalExitPoint(entryDirection, DirectionEnum.LEFT);
    }
    
    /**
     * Calcula ruta para U-turn
     * CORREGIDO: Para Este/Oeste usa la nueva lógica que calcula PARE basado en destino real
     */
    private void calculateUTurnPath(double startX, double startY, String entryDirection, String exitDirection) {
        // Mismo patrón para todas las direcciones para consistencia
        addStopLinePoint(entryDirection, startX, startY);
        PathPoint stopPoint = waypoints.get(waypoints.size() - 1);
        addIntersectionEntryPoint(entryDirection, stopPoint.x, stopPoint.y);
        waypoints.add(new PathPoint(CENTER_X, CENTER_Y));
        addIntersectionExitPointTurn(exitDirection); // exitDirection == entryDirection en U-turn
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
     * ESPECIAL: Fix crítico para Este/Oeste que causaba rebote en horizontales
     */
    private void addIntersectionExitPointStraight(String entryDirection, String exitDirection) {
        double exitX, exitY;
        
        // Para movimiento recto, mantener EXACTAMENTE la coordenada del carril de entrada
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
                // CORREGIDO: Movimiento recto horizontal hacia este - USAR ENTRADA EXACTA
                exitX = CENTER_X + INTERSECTION_SIZE/2.0;
                exitY = entryPoint.y; // CRÍTICO: mantener Y EXACTO del punto de entrada
                break;
            case "west":
                // CORREGIDO: Movimiento recto horizontal hacia oeste - USAR ENTRADA EXACTA
                exitX = CENTER_X - INTERSECTION_SIZE/2.0;
                exitY = entryPoint.y; // CRÍTICO: mantener Y EXACTO del punto de entrada
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(exitX, exitY));
        System.out.println("Salida intersección RECTO " + exitDirection + ": (" + exitX + ", " + exitY + ") - Y exacto: " + entryPoint.y);
    }
    
    /**
     * Agrega el punto de salida de la intersección para giros
     * CORREGIDO: Usa las posiciones reales de los carriles de salida
     * ESPECIAL U-TURN: Para U-turns Este/Oeste, usar carril opuesto de la MISMA calle
     */
    private void addIntersectionExitPointTurn(String exitDirection) {
        double exitX, exitY;
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        
        System.out.println("EXIT POINT DEBUG - Direction: " + exitDirection + ", Street: " + exitStreet.getId());
        // Unificado: siempre usar la geometría de la calle de salida
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
        
        System.out.println("EXIT POINT DEBUG - Calculated: (" + exitX + ", " + exitY + ")");
        
        // MONOTONICIDAD CORREGIDA: Solo para giros normales (no U-turns)
    if (!(entryDirection.equals(exitDirection))) { // No aplicar cuando es U-turn
            PathPoint last = waypoints.get(waypoints.size()-1);
            System.out.println("MONOTONIC CHECK - Last waypoint: " + last + ", calculated exitX: " + exitX + ", entryDirection: " + entryDirection);
            
            if (entryDirection.equals("east")) {
                // Para giros a Norte/Sur desde Este: permitir ajuste final al centro del carril de destino
                if (exitDirection.equals("south") || exitDirection.equals("north")) {
                    // Permitir incremento moderado para llegar al centro del carril (max +20 píxeles)
                    double maxIncrement = last.x + 20.0;
                    if (exitX > maxIncrement) {
                        System.out.println("MONOTONIC FIX - East rebote grande detectado! exitX " + exitX + " > maxAllowed " + maxIncrement + " -> clamping to " + maxIncrement);
                        exitX = maxIncrement;
                    } else {
                        System.out.println("MONOTONIC OK - East permite ajuste al carril: " + exitX);
                    }
                }
            } else if (entryDirection.equals("west")) {
                // Para giros a Norte/Sur desde Oeste: permitir ajuste final al centro del carril
                if (exitDirection.equals("south") || exitDirection.equals("north")) {
                    // Permitir decremento moderado para llegar al centro del carril (max -20 píxeles)
                    double minDecrement = last.x - 20.0;
                    if (exitX < minDecrement) {
                        System.out.println("MONOTONIC FIX - West rebote grande detectado! exitX " + exitX + " < minAllowed " + minDecrement + " -> clamping to " + minDecrement);
                        exitX = minDecrement;
                    } else {
                        System.out.println("MONOTONIC OK - West permite ajuste al carril: " + exitX);
                    }
                }
            }
        }
        
        waypoints.add(new PathPoint(exitX, exitY));
        System.out.println("WAYPOINT ADDED - " + exitDirection.toUpperCase() + " Exit: (" + exitX + ", " + exitY + ")");
    }
    
    /**
     * Agrega el punto de parada antes de la señal de PARE
     * CORREGIDO: Mantiene la X del vehículo (mismo carril)
     * ESPECIAL: Para U-turns desde Este/Oeste, limitar al centro para evitar overshooting
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
                // Vehículo viene del este avanzando hacia el oeste (X decrece) -> STOP antes del borde ESTE
                stopX = CENTER_X + INTERSECTION_SIZE/2.0 + stopDistance; // 680 + 50 = 730 (antes de entrar)
                stopY = vehicleY;
                break;
            case "west":
                // Vehículo viene del oeste avanzando hacia el este (X crece) -> STOP antes del borde OESTE
                stopX = CENTER_X - INTERSECTION_SIZE/2.0 - stopDistance; // 600 - 50 = 550 (antes de entrar)
                stopY = vehicleY;
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(stopX, stopY));
    }
    
    /**
     * Agrega el punto de entrada a la intersección
     * CORRECCIÓN FINAL: Para Este/Oeste, solo agregar entrada si NO causa rebote
     * ESPECIAL U-TURN: Para U-turns, omitir entrada para evitar ir más allá del centro
     */
    private void addIntersectionEntryPoint(String entryDirection, double vehicleX, double vehicleY) {
        double entryX, entryY;
        
        switch (entryDirection) {
            case "north":
                // Entrada desde el norte - mantener X, llegar al borde de la intersección
                entryX = vehicleX; // MANTENER el carril original
                entryY = CENTER_Y - INTERSECTION_SIZE/2.0;
                waypoints.add(new PathPoint(entryX, entryY));
                System.out.println("WAYPOINT ADDED - " + entryDirection.toUpperCase() + " Entry: (" + entryX + ", " + entryY + ")");
                break;
            case "south":
                // Entrada desde el sur - mantener X
                entryX = vehicleX;
                entryY = CENTER_Y + INTERSECTION_SIZE/2.0;
                waypoints.add(new PathPoint(entryX, entryY));
                System.out.println("WAYPOINT ADDED - " + entryDirection.toUpperCase() + " Entry: (" + entryX + ", " + entryY + ")");
                break;
                case "east":
                // Entrada correcta borde ESTE de la intersección
                entryX = CENTER_X + INTERSECTION_SIZE/2.0; // 680
                entryY = vehicleY;
                waypoints.add(new PathPoint(entryX, entryY));
                System.out.println("WAYPOINT ADDED - EAST Entry: (" + entryX + ", " + entryY + ")");
                break;
            case "west":
                // Entrada correcta borde OESTE de la intersección
                entryX = CENTER_X - INTERSECTION_SIZE/2.0; // 600
                entryY = vehicleY;
                waypoints.add(new PathPoint(entryX, entryY));
                System.out.println("WAYPOINT ADDED - WEST Entry: (" + entryX + ", " + entryY + ")");
                break;
            default:
                return;
        }
    }
    
    /**
     * Agrega el punto final en el extremo de la calle de salida
     * CORREGIDO: Para movimiento recto, mantiene la alineación del carril
     */
    private void addFinalExitPoint(String entryDirection, DirectionEnum turnDirection) {
        String exitDirection = calculateExitDirection(entryDirection, turnDirection);
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        double finalX, finalY;
        if (turnDirection == DirectionEnum.STRAIGHT) {
            PathPoint exitPoint = waypoints.get(waypoints.size() - 1);
            switch (exitDirection) {
                case "north":
                    finalX = exitPoint.x; finalY = exitStreet.getPosY(); break;
                case "south":
                    finalX = exitPoint.x; finalY = exitStreet.getPosY() + exitStreet.getHeight(); break;
                case "east":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth(); finalY = exitPoint.y; break;
                case "west":
                    finalX = exitStreet.getPosX(); finalY = exitPoint.y; break;
                default: return;
            }
        } else {
            switch (exitDirection) {
                case "north":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                    finalY = exitStreet.getPosY();
                    break;
                case "south":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth() / 2.0;
                    finalY = exitStreet.getPosY() + exitStreet.getHeight();
                    break;
                case "east":
                    finalX = exitStreet.getPosX() + exitStreet.getWidth();
                    finalY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                    break;
                case "west":
                    finalX = exitStreet.getPosX();
                    finalY = exitStreet.getPosY() + exitStreet.getHeight() / 2.0;
                    break;
                default: return;
            }
            // MONOTONICIDAD para giros desde ESTE/OESTE: no invertir X
            PathPoint last = waypoints.get(waypoints.size()-1);
            if (turnDirection != DirectionEnum.U_TURN) {
                if (this.entryDirection.equals("east") && finalX > last.x) {
                    finalX = last.x; // mantener o seguir decreciendo
                } else if (this.entryDirection.equals("west") && finalX < last.x) {
                    finalX = last.x; // mantener o seguir creciendo
                }
            }
        }
        waypoints.add(new PathPoint(finalX, finalY));
    }
    
    /**
     * Agrega el punto final para movimientos rectos - PERFECTAMENTE ALINEADO
     * CORREGIDO: Mantiene la alineación exacta del carril sin desviaciones
     * ESPECIAL: Fix para Este/Oeste que tenían rebote en movimientos horizontales
     */
    private void addFinalExitPointStraight(String entryDirection, String exitDirection) {
        // Obtener el punto de salida de la intersección para mantener alineación
        PathPoint exitPoint = waypoints.get(waypoints.size() - 1);
        
        // Buscar la calle de salida
        Street exitStreet = findExitStreet(exitDirection, this.scenarioController);
        if (exitStreet == null) return;
        
        double finalX, finalY;
        
        // CRÍTICO: Para movimiento RECTO mantener coordenada lateral EXACTA del carril
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
                // Movimiento horizontal hacia derecha: usar extremo de la calle de salida
                finalX = exitStreet.getPosX() + exitStreet.getWidth();
                finalY = exitPoint.y;
                break;
            case "west":
                // Movimiento horizontal hacia izquierda: usar extremo de la calle de salida
                finalX = exitStreet.getPosX();
                finalY = exitPoint.y;
                break;
            default:
                return;
        }
        
        waypoints.add(new PathPoint(finalX, finalY));
        System.out.println("Final RECTO " + exitDirection + ": (" + finalX + ", " + finalY + ") - alineado PERFECTO con carril");
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
     * DEBUG: Imprime coordenadas X para vehículos Este/Oeste en cada tick
     * @param speed Velocidad de movimiento
     * @return true si el movimiento continúa, false si la ruta está completa
     */
    public boolean moveAlongPath(double speed) {
        if (completed || currentWaypointIndex >= waypoints.size()) {
            completed = true;
            return false;
        }
        
        PathPoint targetPoint = waypoints.get(currentWaypointIndex);
        
        // LOG cada 5 ticks para depuración
        tickCounter++;
        if (tickCounter % 5 == 0) {
            System.out.println("TICK " + tickCounter + " - " + entryDirection.toUpperCase() + " vehículo en (" + String.format("%.1f", currentX) + ", " + String.format("%.1f", currentY) + ") -> Target W" + currentWaypointIndex + ": " + targetPoint);
        }
        
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
            
            // DEBUG: Notificar cambio de waypoint para vehículos horizontales
            if (entryDirection.equals("east") || entryDirection.equals("west")) {
                System.out.println("DEBUG " + entryDirection.toUpperCase() + " - CAMBIO a Waypoint[" + currentWaypointIndex + "]");
            }
            
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
        
        System.out.println("SMOOTH CURVE DEBUG - Entry Point: " + entryPoint + " (from " + entryDirection + " to " + exitDirection + ", isRightTurn=" + isRightTurn + ")");
        
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
        
        System.out.println("SMOOTH CURVE DEBUG - Exit Point CALCULADO: (" + exitX + ", " + exitY + ")");
        System.out.println("SMOOTH CURVE DEBUG - Street info: " + exitStreet.getId() + " posX=" + exitStreet.getPosX() + " posY=" + exitStreet.getPosY() + " width=" + exitStreet.getWidth() + " height=" + exitStreet.getHeight());
        
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
     * CORRECCIÓN EXTREMA: Eliminar waypoints intermedios en giros desde Este
     * Aplicar la misma lógica exitosa de calculateStraightPath: IR DIRECTO sin puntos intermedios
     * SOLO para Este - Norte y Sur mantienen su lógica original
     */
    private void addMonotonicCurveFromEast(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        // FIX v2: Puede ocurrir que endX > startX (salida Norte/Sur está más al centro/derecha)
        // En ese caso la X debe ser MONÓTONA CRECIENTE; si endX < startX, entonces decreciente.
        int steps = 3;
        boolean increasing = endX > startX;
        double lastX = startX;
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            double eased = isRightTurn ? (progress * progress) : progress; // easing sólo para giro derecho
            double curveX = startX + (endX - startX) * eased;
            if (increasing) {
                if (curveX <= lastX) curveX = lastX + 0.5; // asegurar crecimiento estricto
            } else { // decreciente
                if (curveX >= lastX) curveX = lastX - 0.5; // asegurar decrecimiento estricto
            }
            lastX = curveX;
            double curveY = startY + (endY - startY) * progress; // Y siempre progresa linealmente
            waypoints.add(new PathPoint(curveX, curveY));
        }
        System.out.println("ESTE GIRO->" + (isRightTurn ? "R" : "L") + " - Waypoints suaves agregados: " + steps + (increasing ? " (X creciente)" : " (X decreciente)"));
    }
    
    private void addMonotonicCurveFromWest(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        // FIX v2: Para giros desde Oeste normalmente endX < startX (hacia centro). Si no, adaptamos.
        int steps = 3;
        boolean increasing = endX > startX; // normalmente false
        double lastX = startX;
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            double eased = isRightTurn ? (progress * progress) : progress;
            double curveX = startX + (endX - startX) * eased;
            if (increasing) {
                if (curveX <= lastX) curveX = lastX + 0.5; // asegurar crecimiento
            } else { // decreciente esperado
                if (curveX >= lastX) curveX = lastX - 0.5; // asegurar decrecimiento
            }
            lastX = curveX;
            double curveY = startY + (endY - startY) * progress;
            waypoints.add(new PathPoint(curveX, curveY));
        }
        System.out.println("OESTE GIRO->" + (isRightTurn ? "R" : "L") + " - Waypoints suaves agregados: " + steps + (increasing ? " (X creciente)" : " (X decreciente)"));
    }

    /**
     * RESTAURADO: Curva original desde Norte - LÓGICA CLONADA DEL SUR
     * Norte y Sur usan la misma lógica simple que funcionaba perfectamente
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
        }
    }
    
    /**
     * RESTAURADO: Curva original desde Sur - LÓGICA DE REFERENCIA
     * Esta implementación simple y efectiva funciona perfectamente
     */
    private void addMonotonicCurveFromSouth(double startX, double startY, double endX, double endY, boolean isRightTurn) {
        int steps = 3; // Consistencia con Norte
        
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / (steps + 1);
            
            // Y: progresión lineal monotónica (Sur → Norte, Y decrece)
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
        }
    }
    
    /**
     * NUEVA IMPLEMENTACIÓN: Trayectoria monótona para giros desde Este/Oeste
     * CORRECCIÓN CRÍTICA: Ajustar punto de PARE para estar más cerca del destino real
     * PROBLEMA: PARE en X=550 muy lejos de destino X=620, causa movimiento ineficiente
     * SOLUCIÓN: PARE más cerca del destino para trayectoria directa
     */
    // Eliminadas implementaciones especiales East/West. Ahora toda la lógica comparte el mismo patrón.
}