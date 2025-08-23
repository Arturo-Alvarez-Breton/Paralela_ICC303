package app.model;

import app.enums.DirectionEnum;
import app.ui.LaunchView;

/**
 * Coordenadas exactas del sistema JavaFX original - PRESERVADAS EXACTAMENTE
 * No modificar estas constantes, están calibradas para el correcto funcionamiento visual
 */
public class IntersectionCoordinates {
    
    // === COORDENADAS ORIGINALES EXACTAS - NO MODIFICAR ===
    public static final int SIZE = LaunchView.HEIGHT;
    public static final int ROAD_WIDTH = SIZE / 4;
    public static final int HALF_ROAD_WIDTH = ROAD_WIDTH / 2;
    public static final int QUARTER_ROAD_WIDTH = HALF_ROAD_WIDTH / 2;
    public static final double LINE_WIDTH = SIZE * 0.008;
    public static final double LINE_LENGTH = SIZE * 0.02;

    public static final double CENTER = SIZE / 2.0;
    public static final double CAR_OFFSET = ROAD_WIDTH / 2.0;
    public static final double STOP_SIGN_OFFSET = 25;
    public static final double VEHICLE_SPACING = 35.0; // Aumentado para mejor spacing entre vehículos
    
    // === ÁREA DE LA INTERSECCIÓN ===
    // Coordenadas del área rectangular donde solo puede haber un vehículo
    // CORREGIDO: Área más pequeña para NO incluir las líneas de parada
    public static final double INTERSECTION_MIN_X = CENTER - QUARTER_ROAD_WIDTH;
    public static final double INTERSECTION_MAX_X = CENTER + QUARTER_ROAD_WIDTH;
    public static final double INTERSECTION_MIN_Y = CENTER - QUARTER_ROAD_WIDTH;
    public static final double INTERSECTION_MAX_Y = CENTER + QUARTER_ROAD_WIDTH;
    
    /**
     * Calcula la posición de inicio según el punto de entrada
     * COORDENADAS EXACTAS DEL SISTEMA ORIGINAL
     */
    public static Position getStartPosition(String entryPoint) {
        return switch (entryPoint) {
            case "norte" -> new Position(CENTER - QUARTER_ROAD_WIDTH, 0);
            case "sur" -> new Position(CENTER + QUARTER_ROAD_WIDTH, SIZE);
            case "este" -> new Position(SIZE, CENTER - QUARTER_ROAD_WIDTH);
            case "oeste" -> new Position(0, CENTER + QUARTER_ROAD_WIDTH);
            default -> new Position(CENTER, CENTER);
        };
    }
    
    /**
     * Calcula la posición de parada antes del cruce
     * COORDENADAS EXACTAS DEL SISTEMA ORIGINAL
     */
    public static Position getStopLinePosition(String entryPoint) {
        return switch (entryPoint) {
            case "norte" -> new Position(CENTER - QUARTER_ROAD_WIDTH, CENTER - HALF_ROAD_WIDTH);
            case "sur" -> new Position(CENTER + QUARTER_ROAD_WIDTH, CENTER + HALF_ROAD_WIDTH);
            case "este" -> new Position(CENTER + HALF_ROAD_WIDTH, CENTER - QUARTER_ROAD_WIDTH);
            case "oeste" -> new Position(CENTER - HALF_ROAD_WIDTH, CENTER + QUARTER_ROAD_WIDTH);
            default -> new Position(CENTER, CENTER);
        };
    }
    
    /**
     * Calcula la posición de parada con spacing para evitar colisiones
     * COORDENADAS EXACTAS DEL SISTEMA ORIGINAL con spacing
     */
    public static Position getStopLinePositionWithSpacing(String entryPoint, int queuePosition) {
        Position basePos = getStopLinePosition(entryPoint);
        double x = basePos.getX();
        double y = basePos.getY();
        
        double offset = queuePosition * VEHICLE_SPACING;
        
        switch (entryPoint) {
            case "norte" -> y = basePos.getY() - offset;
            case "sur" -> y = basePos.getY() + offset;
            case "este" -> x = basePos.getX() + offset;
            case "oeste" -> x = basePos.getX() - offset;
        }
        
        return new Position(x, y);
    }
    
    /**
     * Calcula la posición de salida según entrada y giro
     * COORDENADAS EXACTAS DEL SISTEMA ORIGINAL
     */
    public static Position getExitPosition(String entryPoint, DirectionEnum turn) {
        return switch (entryPoint) {
            case "norte" -> switch (turn) {
                case STRAIGHT -> new Position(CENTER - QUARTER_ROAD_WIDTH, SIZE);
                case RIGHT -> new Position(0, CENTER + QUARTER_ROAD_WIDTH);
                case LEFT -> new Position(SIZE, CENTER - QUARTER_ROAD_WIDTH);
                case U_TURN -> new Position(CENTER + QUARTER_ROAD_WIDTH, 0);
            };
            case "sur" -> switch (turn) {
                case STRAIGHT -> new Position(CENTER + QUARTER_ROAD_WIDTH, 0);
                case RIGHT -> new Position(SIZE, CENTER - QUARTER_ROAD_WIDTH);
                case LEFT -> new Position(0, CENTER + QUARTER_ROAD_WIDTH);
                case U_TURN -> new Position(CENTER - QUARTER_ROAD_WIDTH, SIZE);
            };
            case "este" -> switch (turn) {
                case STRAIGHT -> new Position(0, CENTER - QUARTER_ROAD_WIDTH);
                case RIGHT -> new Position(CENTER - QUARTER_ROAD_WIDTH, 0);
                case LEFT -> new Position(CENTER + QUARTER_ROAD_WIDTH, SIZE);
                case U_TURN -> new Position(SIZE, CENTER + QUARTER_ROAD_WIDTH);
            };
            case "oeste" -> switch (turn) {
                case STRAIGHT -> new Position(SIZE, CENTER + QUARTER_ROAD_WIDTH);
                case RIGHT -> new Position(CENTER + QUARTER_ROAD_WIDTH, SIZE);
                case LEFT -> new Position(CENTER - QUARTER_ROAD_WIDTH, 0);
                case U_TURN -> new Position(0, CENTER - QUARTER_ROAD_WIDTH);
            };
            default -> new Position(CENTER, CENTER);
        };
    }
    
    /**
     * Calcula posiciones intermedias para movimientos complejos (giros, U-turns)
     * COORDENADAS EXACTAS DEL SISTEMA ORIGINAL
     */
    public static Position[] getIntermediatePositions(String entryPoint, DirectionEnum turn) {
        Position stopPos = getStopLinePosition(entryPoint);
        
        // Para movimientos directos, no hay posiciones intermedias
        if (turn == DirectionEnum.STRAIGHT) {
            return new Position[0];
        }
        
        // Para giros y U-turns, calcular posiciones intermedias
        return switch (entryPoint) {
            case "norte" -> switch (turn) {
                case RIGHT -> new Position[]{
                    new Position(stopPos.getX(), CENTER - QUARTER_ROAD_WIDTH)
                };
                case LEFT -> new Position[]{
                    new Position(stopPos.getX(), CENTER + QUARTER_ROAD_WIDTH)
                };
                case U_TURN -> new Position[]{
                    new Position(CENTER + QUARTER_ROAD_WIDTH, stopPos.getY()),
                    new Position(CENTER + QUARTER_ROAD_WIDTH, CENTER + QUARTER_ROAD_WIDTH)
                };
                default -> new Position[0];
            };
            case "sur" -> switch (turn) {
                case RIGHT -> new Position[]{
                    new Position(stopPos.getX(), CENTER + QUARTER_ROAD_WIDTH)
                };
                case LEFT -> new Position[]{
                    new Position(stopPos.getX(), CENTER - QUARTER_ROAD_WIDTH)
                };
                case U_TURN -> new Position[]{
                    new Position(CENTER - QUARTER_ROAD_WIDTH, stopPos.getY()),
                    new Position(CENTER - QUARTER_ROAD_WIDTH, CENTER - QUARTER_ROAD_WIDTH)
                };
                default -> new Position[0];
            };
            case "este" -> switch (turn) {
                case RIGHT -> new Position[]{
                    new Position(CENTER + QUARTER_ROAD_WIDTH, stopPos.getY())
                };
                case LEFT -> new Position[]{
                    new Position(CENTER - QUARTER_ROAD_WIDTH, stopPos.getY())
                };
                case U_TURN -> new Position[]{
                    new Position(stopPos.getX(), CENTER + QUARTER_ROAD_WIDTH),
                    new Position(CENTER - QUARTER_ROAD_WIDTH, CENTER + QUARTER_ROAD_WIDTH)
                };
                default -> new Position[0];
            };
            case "oeste" -> switch (turn) {
                case RIGHT -> new Position[]{
                    new Position(CENTER - QUARTER_ROAD_WIDTH, stopPos.getY())
                };
                case LEFT -> new Position[]{
                    new Position(CENTER + QUARTER_ROAD_WIDTH, stopPos.getY())
                };
                case U_TURN -> new Position[]{
                    new Position(stopPos.getX(), CENTER - QUARTER_ROAD_WIDTH),
                    new Position(CENTER + QUARTER_ROAD_WIDTH, CENTER - QUARTER_ROAD_WIDTH)
                };
                default -> new Position[0];
            };
            default -> new Position[0];
        };
    }
    
    /**
     * Verifica si una posición está dentro del área de la intersección
     * ÁREA CRÍTICA: Solo un vehículo puede estar aquí a la vez
     */
    public static boolean isInsideIntersection(Position position) {
        if (position == null) return false;
        
        return position.getX() >= INTERSECTION_MIN_X && 
               position.getX() <= INTERSECTION_MAX_X &&
               position.getY() >= INTERSECTION_MIN_Y && 
               position.getY() <= INTERSECTION_MAX_Y;
    }
    
    /**
     * Verifica si un vehículo está físicamente dentro de la intersección
     * basándose en su posición actual
     */
    public static boolean isVehicleInIntersection(Position vehiclePosition) {
        return isInsideIntersection(vehiclePosition);
    }
    
    /**
     * DEBUG: Muestra las coordenadas exactas del área de intersección
     * Para verificar que no incluye las líneas de parada
     */
    public static void debugIntersectionArea() {
        System.out.println("🚧 ÁREA DE INTERSECCIÓN:");
        System.out.println("   X: " + INTERSECTION_MIN_X + " → " + INTERSECTION_MAX_X);
        System.out.println("   Y: " + INTERSECTION_MIN_Y + " → " + INTERSECTION_MAX_Y);
        System.out.println("📍 LÍNEAS DE PARADA:");
        System.out.println("   Norte: " + getStopLinePosition("norte"));
        System.out.println("   Sur: " + getStopLinePosition("sur"));
        System.out.println("   Este: " + getStopLinePosition("este"));
        System.out.println("   Oeste: " + getStopLinePosition("oeste"));
    }
}
