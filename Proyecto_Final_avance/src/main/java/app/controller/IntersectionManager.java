package app.controller;

import app.model.Movement;
import app.model.Vehicle;

import java.util.*;

/**
 * Gestiona las reglas de conflicto de movimientos en una intersección.
 */
public class IntersectionManager {
    private static final Map<Movement, Set<Movement>> CONFLICTS = new HashMap<>();

    static {
        // MATRIZ COMPLETA DE CONFLICTOS basada en la tabla de prioridades del README
        // Formato: registerConflict(movimiento1, movimiento2) - indica que no pueden ser simultáneos
        
        // === CONFLICTOS PARA MOVIMIENTOS STRAIGHT ===
        // STRAIGHT desde NORTE: conflicto con carriles de la DERECHA (ESTE)
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN)
        );
        
        // STRAIGHT desde SUR: conflicto con carriles de la DERECHA (OESTE)
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN)
        );
        
        // STRAIGHT desde ESTE: conflicto con carriles de la DERECHA (SUR)
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN)
        );
        
        // STRAIGHT desde OESTE: conflicto con carriles de la DERECHA (NORTE)
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN)
        );
        
        // === CONFLICTOS PARA MOVIMIENTOS LEFT ===
        // LEFT desde cualquier dirección: conflicto con carro OPUESTO en cualquier movimiento
        
        // LEFT desde NORTE: conflicto con SUR (opuesto)
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.SUR, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN)
        );
        
        // LEFT desde SUR: conflicto con NORTE (opuesto)
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT),
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN)
        );
        
        // LEFT desde ESTE: conflicto con OESTE (opuesto)
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN)
        );
        
        // LEFT desde OESTE: conflicto con ESTE (opuesto)
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT),
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN)
        );
        
        // === CONFLICTOS PARA MOVIMIENTOS U-TURN ===
        // U-TURN: conflicto con carro a la IZQUIERDA en cualquier movimiento
        
        // U-TURN desde NORTE: conflicto con OESTE (izquierda)
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.OESTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.OESTE, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.OESTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN)
        );
        
        // U-TURN desde SUR: conflicto con ESTE (izquierda)
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.ESTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.ESTE, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.ESTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN)
        );
        
        // U-TURN desde ESTE: conflicto con NORTE (izquierda)
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.NORTE, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.NORTE, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN)
        );
        
        // U-TURN desde OESTE: conflicto con SUR (izquierda)
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.SUR, Movement.Turn.RIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.SUR, Movement.Turn.LEFT)
        );
        registerConflict(
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN)
        );
        
        // === CONFLICTOS ADICIONALES PARA SEGURIDAD ===
        // Múltiples U-TURNS simultáneos siempre generan conflicto
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.SUR, Movement.Turn.U_TURN)
        );
        registerConflict(
                new Movement(Movement.Entry.ESTE, Movement.Turn.U_TURN),
                new Movement(Movement.Entry.OESTE, Movement.Turn.U_TURN)
        );
    }

    private static void registerConflict(Movement a, Movement b) {
        CONFLICTS.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        CONFLICTS.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    /**
     * True si v puede avanzar sin chocar con ninguno de 'others'.
     */
    public boolean isSafeToProceed(Vehicle v, List<Vehicle> others) {
        Movement mv = v.getMovement();
        Set<Movement> blockedBy = CONFLICTS.getOrDefault(mv, Collections.emptySet());
        for (Vehicle o : others) {
            if (blockedBy.contains(o.getMovement())) {
                return false;
            }
        }
        return true;
    }
}
