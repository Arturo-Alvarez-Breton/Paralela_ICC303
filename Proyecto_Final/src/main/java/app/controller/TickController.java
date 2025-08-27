package app.controller;

import javafx.animation.AnimationTimer;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador principal del sistema de ticks para la simulación
 * Maneja el tiempo de simulación y coordina todos los sistemas
 */
public class TickController {
    
    public enum SimulationState {
        STOPPED,
        RUNNING,
        PAUSED
    }
    
    private SimulationState currentState;
    private AnimationTimer timer;
    private long tickCount;
    private long lastTickTime;
    private final List<TickListener> listeners;
    
    // Configuración de ticks
    private long tickIntervalNs = 50_000_000L; // 50ms = 20 ticks por segundo (por defecto)
    
    // Velocidades predefinidas
    public static final long SPEED_SLOW = 100_000_000L;    // 100ms = 10 ticks/seg
    public static final long SPEED_NORMAL = 50_000_000L;   // 50ms = 20 ticks/seg
    public static final long SPEED_FAST = 25_000_000L;     // 25ms = 40 ticks/seg
    public static final long SPEED_VERY_FAST = 16_666_666L; // ~16ms = 60 ticks/seg
    
    public TickController() {
        this.currentState = SimulationState.STOPPED;
        this.tickCount = 0;
        this.lastTickTime = 0;
        this.listeners = new ArrayList<>();
        
        initializeTimer();
    }
    
    /**
     * Inicializa el timer de animación
     */
    private void initializeTimer() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentState == SimulationState.RUNNING) {
                    // Verificar si es tiempo de hacer un tick
                    if (now - lastTickTime >= tickIntervalNs) {
                        processTick();
                        lastTickTime = now;
                    }
                }
            }
        };
    }
    
    /**
     * Procesa un tick de simulación
     */
    private void processTick() {
        tickCount++;
        
        // Notificar a todos los listeners
        for (TickListener listener : listeners) {
            try {
                listener.onTick(tickCount);
            } catch (Exception e) {
                System.err.println("Error en listener durante tick " + tickCount + ": " + e.getMessage());
            }
        }
        
        // Log cada 100 ticks (5 segundos)
        if (tickCount % 100 == 0) {
            System.out.println("Tick: " + tickCount + " | Listeners: " + listeners.size());
        }
    }
    
    /**
     * Inicia la simulación
     */
    public void start() {
        if (currentState != SimulationState.RUNNING) {
            currentState = SimulationState.RUNNING;
            lastTickTime = System.nanoTime();
            timer.start();
            System.out.println("Simulación iniciada - Tick Controller activo");
        }
    }
    
    /**
     * Pausa la simulación
     */
    public void pause() {
        if (currentState == SimulationState.RUNNING) {
            currentState = SimulationState.PAUSED;
            System.out.println("Simulación pausada en tick: " + tickCount);
        }
    }
    
    /**
     * Reanuda la simulación
     */
    public void resume() {
        if (currentState == SimulationState.PAUSED) {
            currentState = SimulationState.RUNNING;
            lastTickTime = System.nanoTime();
            System.out.println("Simulación reanudada desde tick: " + tickCount);
        }
    }
    
    /**
     * Detiene completamente la simulación
     */
    public void stop() {
        currentState = SimulationState.STOPPED;
        timer.stop();
        tickCount = 0;
        System.out.println("Simulación detenida - Tick Controller inactivo");
    }
    
    /**
     * Reinicia la simulación
     */
    public void reset() {
        stop();
        tickCount = 0;
        System.out.println("Simulación reiniciada");
    }
    
    /**
     * Registra un listener para recibir notificaciones de tick
     */
    public void addTickListener(TickListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("Listener agregado al Tick Controller");
        }
    }
    
    /**
     * Remueve un listener
     */
    public void removeTickListener(TickListener listener) {
        listeners.remove(listener);
        System.out.println("Listener removido del Tick Controller");
    }
    
    /**
     * Limpia todos los listeners
     */
    public void clearListeners() {
        listeners.clear();
        System.out.println("Todos los listeners removidos del Tick Controller");
    }
    
    // Getters
    public SimulationState getCurrentState() { return currentState; }
    public long getTickCount() { return tickCount; }
    public boolean isRunning() { return currentState == SimulationState.RUNNING; }
    public boolean isPaused() { return currentState == SimulationState.PAUSED; }
    public boolean isStopped() { return currentState == SimulationState.STOPPED; }
    public int getListenerCount() { return listeners.size(); }
    
    /**
     * Establece la velocidad de los ticks
     */
    public void setTickSpeed(long intervalNs) {
        this.tickIntervalNs = intervalNs;
        System.out.println("Velocidad de ticks cambiada a: " + (1_000_000_000L / intervalNs) + " ticks/seg");
    }
    
    /**
     * Establece el intervalo de ticks en milisegundos
     */
    public void setTickInterval(int intervalMs) {
        this.tickIntervalNs = intervalMs * 1_000_000L; // Convertir ms a nanosegundos
        System.out.println("Intervalo de ticks cambiado a: " + intervalMs + "ms (" + (1000 / intervalMs) + " ticks/seg)");
    }
    
    /**
     * Establece velocidad usando constantes predefinidas
     */
    public void setTickSpeed(String speed) {
        switch (speed.toLowerCase()) {
            case "slow" -> setTickSpeed(SPEED_SLOW);
            case "normal" -> setTickSpeed(SPEED_NORMAL);
            case "fast" -> setTickSpeed(SPEED_FAST);
            case "very_fast" -> setTickSpeed(SPEED_VERY_FAST);
            default -> System.out.println("Velocidad no reconocida: " + speed);
        }
    }
    
    /**
     * Obtiene la velocidad actual en ticks por segundo
     */
    public int getCurrentTicksPerSecond() {
        return (int) (1_000_000_000L / tickIntervalNs);
    }
    
    /**
     * Obtiene información de estado detallada
     */
    public String getStatusInfo() {
        return String.format("Estado: %s | Tick: %d | Velocidad: %d tps | Listeners: %d", 
                           currentState, tickCount, getCurrentTicksPerSecond(), listeners.size());
    }
    
    /**
     * Interface para objetos que quieren recibir notificaciones de tick
     */
    public interface TickListener {
        void onTick(long tickNumber);
    }
}