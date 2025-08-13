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
        // Ejemplos de conflictos — completa con todas tus reglas
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.SUR, Movement.Turn.STRAIGHT)
        );
        registerConflict(
                new Movement(Movement.Entry.NORTE, Movement.Turn.STRAIGHT),
                new Movement(Movement.Entry.OESTE,  Movement.Turn.LEFT)
        );
        // … añade aquí el resto de pares en conflicto …
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
