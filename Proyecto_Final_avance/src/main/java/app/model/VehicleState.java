package app.model;

/**
 * Estados posibles de un vehículo en el sistema basado en ticks
 */
public enum VehicleState {
    APPROACHING,    // Acercándose a la intersección
    WAITING,        // Esperando autorización en la línea de parada
    CROSSING,       // Cruzando la intersección
    EXITING,        // Saliendo de la intersección
    COMPLETED       // Ha completado su recorrido
}
