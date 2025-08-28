package app.controller;

import app.model.Intersection;
import app.model.TrafficLight;
import app.ui.TrafficLightView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for managing traffic lights using tick-based timing
 * Supports alternating West->East and East->West traffic flow
 */
public class TrafficController implements TickController.TickListener {
    private final List<Intersection> intersections;
    private final List<TrafficLight> trafficLights;
    private final Map<String, TrafficLightView> trafficLightViews;

    // Control modes
    public enum TimingMode {
        SYNCHRONIZED,  // All lights change together
        OFFSET         // Lights change with configurable delays (green wave)
    }

    // Traffic flow states
    public enum TrafficPhase {
        WEST_FLOW,    // West->East traffic has green, East->West has red
        EAST_FLOW     // East->West traffic has green, West->East has red
    }

    private TimingMode currentMode;
    private boolean isRunning;
    private TrafficPhase currentPhase;
    private TickController tickController;

    // Timing configuration (in ticks)
    private static final int DEFAULT_GREEN_TICKS = 100; // 5 seconds at 20 ticks/second
    private static final int DEFAULT_RED_TICKS = 100;   // 5 seconds at 20 ticks/second
    
    private int greenTicks = DEFAULT_GREEN_TICKS;
    private int redTicks = DEFAULT_RED_TICKS;

    // Tick counting for phase switching
    private long phaseStartTick = 0;
    private long currentTick = 0;

    // Offset configuration for green wave (in ticks)
    private final Map<String, Integer> lightOffsets;
    private final Map<String, Integer> lightStartTicks;

    public TrafficController(List<Intersection> intersections, List<TrafficLight> trafficLights) {
        this.intersections = intersections != null ? intersections : new ArrayList<>();
        this.trafficLights = trafficLights != null ? trafficLights : new ArrayList<>();
        this.trafficLightViews = new ConcurrentHashMap<>();
        this.lightOffsets = new HashMap<>();
        this.lightStartTicks = new HashMap<>();
        this.currentMode = TimingMode.SYNCHRONIZED;
        this.isRunning = false;

        initializeDefaultOffsets();
        initializeTrafficLights();
    }

    /**
     * Initialize default offset values for green wave effect
     */
    private void initializeDefaultOffsets() {
        // For Scenario 2: create offset pattern for East-West green wave
        // Intersection 1: Start of highway (West->East traffic only)
        lightOffsets.put("traffic_light_intersection_1_we", 0);    // Start immediately

        // Intersections 2 and 3: Middle intersections with both directions
        lightOffsets.put("traffic_light_intersection_2_we", 0);    // Start immediately
        lightOffsets.put("traffic_light_intersection_2_ew", 0);    // Opposite direction
        lightOffsets.put("traffic_light_intersection_3_we", 80);   // 4 second delay (80 ticks)
        lightOffsets.put("traffic_light_intersection_3_ew", 80);   // Same delay for opposite

        // Intersection 4: End of highway (East->West traffic only)
        lightOffsets.put("traffic_light_intersection_4_ew", 160);  // 8 second delay (160 ticks)
    }

    /**
     * Initialize traffic lights with tick-based timing
     */
    private void initializeTrafficLights() {
        // Traffic lights are now controlled centrally by the controller
        // No individual timing setup needed
        System.out.println("Initialized " + trafficLights.size() + " traffic lights for centralized control");
    }

    /**
     * Set the tick controller and register as listener
     */
    public void setTickController(TickController tickController) {
        this.tickController = tickController;
        if (tickController != null) {
            tickController.addTickListener(this);
        }
    }

    /**
     * Handle tick events for traffic light timing
     */
    @Override
    public void onTick(long tickNumber) {
        if (!isRunning) {
            return;
        }

        currentTick = tickNumber;
        
        // Calculate ticks since phase started
        long ticksSincePhaseStart = currentTick - phaseStartTick;
        
        // Debug: Log every 50 ticks
        if (tickNumber % 50 == 0) {
            System.out.println("TrafficController tick " + tickNumber + 
                             ", phase: " + currentPhase + 
                             ", ticks since start: " + ticksSincePhaseStart + 
                             ", change in: " + (greenTicks - ticksSincePhaseStart));
        }
        
        // Check if it's time to switch phases
        if (ticksSincePhaseStart >= greenTicks) {
            switchPhase();
            phaseStartTick = currentTick;
        }

        // Update all traffic lights based on current phase
        updateTrafficLightsByPhase();

        // Update visual views
        updateAllViews();
    }

    /**
     * Update traffic lights based on current phase
     */
    private void updateTrafficLightsByPhase() {
        for (TrafficLight light : trafficLights) {
            String lightId = light.getId();
            
            if (lightId.contains("_we")) {
                // West->East traffic lights
                light.setGreen(currentPhase == TrafficPhase.WEST_FLOW);
            } else if (lightId.contains("_ew")) {
                // East->West traffic lights  
                light.setGreen(currentPhase == TrafficPhase.EAST_FLOW);
            } else {
                // Default behavior for lights without specific direction
                light.setGreen(currentPhase == TrafficPhase.WEST_FLOW);
            }
        }
    }

    /**
     * Switch between traffic phases
     */
    public void switchPhase() {
        TrafficPhase oldPhase = currentPhase;
        currentPhase = (currentPhase == TrafficPhase.WEST_FLOW) ? 
                      TrafficPhase.EAST_FLOW : TrafficPhase.WEST_FLOW;
        
        System.out.println("*** PHASE CHANGE *** From " + oldPhase + " to " + currentPhase + " at tick " + currentTick);
    }

    /**
     * Starts the traffic light control system
     */
    public void startControl() {
        if (isRunning) {
            return; // Already running
        }

        isRunning = true;
        currentPhase = TrafficPhase.WEST_FLOW; // Start with West flow
        phaseStartTick = 0; // Reset phase timer
        
        System.out.println("Starting Traffic Controller with mode: " + currentMode);
        System.out.println("Traffic lights will change every " + greenTicks + " ticks");

        // Initialize all lights to proper state
        updateTrafficLightsByPhase();
        updateAllViews();
    }

    /**
     * Stops the traffic light control system
     */
    public void stopControl() {
        isRunning = false;
        System.out.println("Traffic Controller stopped");
    }

    /**
     * Pauses the traffic light control system
     */
    public void pauseControl() {
        isRunning = false;
        System.out.println("Traffic Controller paused");
    }

    /**
     * Resumes the traffic light control system
     */
    public void resumeControl() {
        isRunning = true;
        System.out.println("Traffic Controller resumed");
    }

    /**
     * Initialize traffic lights to proper phase
     */
    private void initializeTrafficPhase() {
        updateTrafficLightsByPhase();
        updateAllViews();
    }

    /**
     * Updates all traffic light views
     */
    private void updateAllViews() {
        for (TrafficLightView view : trafficLightViews.values()) {
            view.updateLightState();
        }
    }

    /**
     * Updates a specific traffic light view
     */
    private void updateLightView(TrafficLight light) {
        TrafficLightView view = trafficLightViews.get(light.getId());
        if (view != null) {
            view.updateLightState();
        }
    }

    /**
     * Registers a traffic light view for visual updates
     */
    public void registerTrafficLightView(TrafficLightView view) {
        trafficLightViews.put(view.getTrafficLight().getId(), view);
    }

    /**
     * Unregisters a traffic light view
     */
    public void unregisterTrafficLightView(String lightId) {
        trafficLightViews.remove(lightId);
    }

    /**
     * Sets the timing mode and restarts control if running
     */
    public void setTimingMode(TimingMode mode) {
        if (mode != this.currentMode) {
            boolean wasRunning = isRunning;
            if (wasRunning) {
                stopControl();
            }
            this.currentMode = mode;
            if (wasRunning) {
                startControl();
            }
        }
    }

    /**
     * Sets a custom offset for a specific traffic light
     */
    public void setLightOffset(String lightId, int offsetTicks) {
        lightOffsets.put(lightId, offsetTicks);
        System.out.println("Set offset for " + lightId + ": " + offsetTicks + " ticks");
    }

    /**
     * Gets the current timing mode
     */
    public TimingMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Checks if the controller is currently running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Legacy method for managing intersections (kept for compatibility)
     */
    private void manageIntersections() {
        // Integration with intersection logic can be added here if needed
    }

    /**
     * Gets all registered traffic lights
     */
    public List<TrafficLight> getTrafficLights() {
        return new ArrayList<>(trafficLights);
    }

    /**
     * Gets a traffic light by ID
     */
    public TrafficLight getTrafficLight(String id) {
        return trafficLights.stream()
                .filter(light -> light.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets current status of all traffic lights
     */
    public Map<String, Boolean> getAllLightStates() {
        Map<String, Boolean> states = new HashMap<>();
        for (TrafficLight light : trafficLights) {
            states.put(light.getId(), light.isGreen());
        }
        return states;
    }

    /**
     * Checks if a traffic light allows traffic to pass
     * @param lightId ID of the traffic light to check
     * @return true if traffic can pass (GREEN), false if RED
     */
    public boolean canTrafficPass(String lightId) {
        TrafficLight light = getTrafficLight(lightId);
        if (light == null) {
            return false; // Safe default - no light means no pass
        }

        return light.isGreen();
    }

    /**
     * Gets the current traffic phase
     * @return Current phase (WEST_FLOW or EAST_FLOW)
     */
    public TrafficPhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Sets the duration of green lights in ticks
     * @param ticks Number of ticks for green phase
     */
    public void setGreenTicks(int ticks) {
        this.greenTicks = Math.max(1, ticks);
        System.out.println("Green phase duration set to " + this.greenTicks + " ticks");
    }

    /**
     * Sets the duration of red lights in ticks
     * @param ticks Number of ticks for red phase
     */
    public void setRedTicks(int ticks) {
        this.redTicks = Math.max(1, ticks);
        System.out.println("Red phase duration set to " + this.redTicks + " ticks");
    }

    /**
     * Sets the duration for both green and red phases
     * @param ticks Number of ticks for each phase
     */
    public void setPhaseDuration(int ticks) {
        setGreenTicks(ticks);
        setRedTicks(ticks);
    }

    /**
     * Gets the current green phase duration in ticks
     */
    public int getGreenTicks() {
        return greenTicks;
    }

    /**
     * Gets the current red phase duration in ticks
     */
    public int getRedTicks() {
        return redTicks;
    }

    /**
     * Gets the total cycle duration in ticks
     */
    public int getTotalCycleTicks() {
        return greenTicks + redTicks;
    }

    /**
     * Gets remaining ticks until next phase change
     */
    public long getTicksUntilPhaseChange() {
        long ticksSincePhaseStart = currentTick - phaseStartTick;
        return Math.max(0, greenTicks - ticksSincePhaseStart);
    }

    /**
     * Gets detailed status including phase information
     * @return Status string with phase and timing information
     */
    public String getDetailedStatus() {
        if (!isRunning) {
            return "Estado: Inactivo";
        }

        String phaseStr = currentPhase == TrafficPhase.WEST_FLOW ? "West→East" : "East→West";
        long ticksRemaining = getTicksUntilPhaseChange();
        return String.format("Estado: Activo | Modo: %s | Fase: %s | Cambio en: %d ticks", 
                           currentMode, phaseStr, ticksRemaining);
    }
}
