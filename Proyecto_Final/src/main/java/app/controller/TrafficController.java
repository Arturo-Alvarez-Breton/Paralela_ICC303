package app.controller;

import app.model.Intersection;
import app.model.TrafficLight;
import app.model.Vehicle;
import app.model.enums.LightColor;
import app.ui.TrafficLightView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for managing traffic lights and their coordination
 * Supports alternating West->East and East->West traffic flow with visual yellow warnings
 */
public class TrafficController {
    private final List<Intersection> intersections;
    private final List<TrafficLight> trafficLights;
    private final Map<String, TrafficLightView> trafficLightViews;
    private final Map<TrafficLight, Timeline> lightTimelines;

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

    // Timing configuration
    private static final int TOTAL_GREEN_TIME = 5;  // Total green time in seconds
    private static final int YELLOW_WARNING_TIME = 2; // Last 2 seconds show yellow visually
    private static final int PURE_GREEN_TIME = TOTAL_GREEN_TIME - YELLOW_WARNING_TIME; // 3 seconds pure green

    // Offset configuration for green wave (in seconds)
    private final Map<String, Integer> lightOffsets;

    public TrafficController(List<Intersection> intersections, List<TrafficLight> trafficLights) {
        this.intersections = intersections != null ? intersections : new ArrayList<>();
        this.trafficLights = trafficLights != null ? trafficLights : new ArrayList<>();
        this.trafficLightViews = new ConcurrentHashMap<>();
        this.lightTimelines = new ConcurrentHashMap<>();
        this.lightOffsets = new HashMap<>();
        this.currentMode = TimingMode.SYNCHRONIZED;
        this.isRunning = false;

        initializeDefaultOffsets();
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
        lightOffsets.put("traffic_light_intersection_3_we", 4);    // 4 second delay
        lightOffsets.put("traffic_light_intersection_3_ew", 4);    // Same delay for opposite

        // Intersection 4: End of highway (East->West traffic only)
        lightOffsets.put("traffic_light_intersection_4_ew", 8);    // 8 second delay for final intersection
    }

    /**
     * Starts the traffic light control system
     */
    public void startControl() {
        if (isRunning) {
            return; // Already running
        }

        isRunning = true;
        System.out.println("Starting Traffic Controller with mode: " + currentMode);

        switch (currentMode) {
            case SYNCHRONIZED:
                startSynchronizedControl();
                break;
            case OFFSET:
                startOffsetControl();
                break;
        }
    }

    /**
     * Starts synchronized control - alternating West/East phases with visual yellow warnings
     */
    private void startSynchronizedControl() {
        if (trafficLights.isEmpty()) {
            System.out.println("No traffic lights to control");
            return;
        }

        // Initialize phase - Start with West flow
        currentPhase = TrafficPhase.WEST_FLOW;

        // Create master timeline for alternating phases
        Timeline masterTimeline = new Timeline();

        // Phase 1: West Green (pure green for 3 seconds)
        masterTimeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(PURE_GREEN_TIME), e -> {
                // Start yellow warning for West (last 2 seconds of green)
                setTrafficPhase(TrafficPhase.WEST_FLOW, true); // true = show yellow warning
                updateAllViews();
                System.out.println("West yellow warning phase (backend still green)");
            })
        );

        // Phase 2: Switch to East flow
        masterTimeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(TOTAL_GREEN_TIME), e -> {
                currentPhase = TrafficPhase.EAST_FLOW;
                setTrafficPhase(TrafficPhase.EAST_FLOW, false); // false = pure green
                updateAllViews();
                System.out.println("Switched to East flow - pure green");
            })
        );

        // Phase 3: East Yellow warning (pure green for East for 3 seconds total)
        masterTimeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(TOTAL_GREEN_TIME + PURE_GREEN_TIME), e -> {
                // Start yellow warning for East (last 2 seconds of green)
                setTrafficPhase(TrafficPhase.EAST_FLOW, true); // true = show yellow warning
                updateAllViews();
                System.out.println("East yellow warning phase (backend still green)");
            })
        );

        // Phase 4: Back to West flow (complete cycle)
        masterTimeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(TOTAL_GREEN_TIME * 2), e -> {
                currentPhase = TrafficPhase.WEST_FLOW;
                setTrafficPhase(TrafficPhase.WEST_FLOW, false); // false = pure green
                updateAllViews();
                System.out.println("Back to West flow - pure green (cycle restart)");
            })
        );

        masterTimeline.setCycleCount(Timeline.INDEFINITE);
        masterTimeline.play();

        // Store timeline for cleanup
        lightTimelines.put(trafficLights.get(0), masterTimeline);

        // Set initial state - West green, East red
        setTrafficPhase(TrafficPhase.WEST_FLOW, false);
        updateAllViews();

        System.out.println("Alternating West/East control started for " + trafficLights.size() + " traffic lights");
        System.out.println("Cycle: West(3s green + 2s yellow) -> East(3s green + 2s yellow) -> repeat");
    }

    /**
     * Sets traffic lights according to phase and warning state
     * @param phase Current traffic phase (WEST_FLOW or EAST_FLOW)
     * @param yellowWarning Whether to show yellow warning (visual only, backend remains green)
     */
    private void setTrafficPhase(TrafficPhase phase, boolean yellowWarning) {
        for (TrafficLight light : trafficLights) {
            String lightId = light.getId();

            if (lightId.contains("_we")) {
                // West->East traffic lights
                if (phase == TrafficPhase.WEST_FLOW) {
                    // West has priority - green (or yellow warning)
                    light.setCurrentColor(yellowWarning ? LightColor.YELLOW : LightColor.GREEN);
                } else {
                    // East has priority - West must stop
                    light.setCurrentColor(LightColor.RED);
                }
            } else if (lightId.contains("_ew")) {
                // East->West traffic lights
                if (phase == TrafficPhase.EAST_FLOW) {
                    // East has priority - green (or yellow warning)
                    light.setCurrentColor(yellowWarning ? LightColor.YELLOW : LightColor.GREEN);
                } else {
                    // West has priority - East must stop
                    light.setCurrentColor(LightColor.RED);
                }
            }
        }
    }

    /**
     * Starts offset control with alternating phases and coordinated timing
     */
    private void startOffsetControl() {
        // In offset mode, create staggered timing for green wave effect
        for (TrafficLight light : trafficLights) {
            startIndividualLightControlWithPhases(light);
        }

        System.out.println("Offset control with alternating phases started for " + trafficLights.size() + " traffic lights");
    }

    /**
     * Starts individual light control with alternating phases and offset timing
     */
    private void startIndividualLightControlWithPhases(TrafficLight light) {
        int offset = lightOffsets.getOrDefault(light.getId(), 0);
        String lightId = light.getId();

        Timeline timeline = new Timeline();

        // Determine if this is a West->East or East->West light
        boolean isWestLight = lightId.contains("_we");

        // Create cycling timeline for this specific light with offset
        int cycleTime = TOTAL_GREEN_TIME * 2; // Total cycle time (West + East phases

        for (int cycle = 0; cycle < 10; cycle++) { // Create multiple cycles
            int cycleStart = cycle * cycleTime;

            if (isWestLight) {
                // West->East light pattern with offset
                // Green phase (3 seconds pure + 2 seconds yellow warning)
                timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(cycleStart + offset), e -> {
                        light.setCurrentColor(LightColor.GREEN);
                        updateLightView(light);
                    })
                );

                // Yellow warning (visual, backend still green)
                timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(cycleStart + offset + PURE_GREEN_TIME), e -> {
                        light.setCurrentColor(LightColor.YELLOW);
                        updateLightView(light);
                    })
                );

                // Red phase (during East green time)
                timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(cycleStart + offset + TOTAL_GREEN_TIME), e -> {
                        light.setCurrentColor(LightColor.RED);
                        updateLightView(light);
                    })
                );

            } else {
                // East->West light pattern with offset (starts when West ends)
                // Red phase (during West green time)
                timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(cycleStart + offset), e -> {
                        light.setCurrentColor(LightColor.RED);
                        updateLightView(light);
                    })
                );

                // Green phase starts after West ends
                timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(cycleStart + offset + TOTAL_GREEN_TIME), e -> {
                        light.setCurrentColor(LightColor.GREEN);
                        updateLightView(light);
                    })
                );

                // Yellow warning for East
                timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(cycleStart + offset + TOTAL_GREEN_TIME + PURE_GREEN_TIME), e -> {
                        light.setCurrentColor(LightColor.YELLOW);
                        updateLightView(light);
                    })
                );
            }
        }

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        lightTimelines.put(light, timeline);

        // Set initial state
        if (isWestLight) {
            light.setCurrentColor(offset == 0 ? LightColor.GREEN : LightColor.RED);
        } else {
            light.setCurrentColor(LightColor.RED); // East starts red
        }
        updateLightView(light);
    }

    /**
     * Starts control for an individual traffic light with offset
     */
    private void startIndividualLightControl(TrafficLight light) {
        int offset = lightOffsets.getOrDefault(light.getId(), 0);
        int greenTime = light.getGreenTime();
        int yellowTime = light.getYellowTime();
        int redTime = light.getRedTime();

        Timeline timeline = new Timeline();

        // Initial delay based on offset
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(offset), e -> {
                light.setCurrentColor(LightColor.GREEN);
                updateLightView(light);
            })
        );

        // Green to Yellow
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(offset + greenTime), e -> {
                light.setCurrentColor(LightColor.YELLOW);
                updateLightView(light);
            })
        );

        // Yellow to Red
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(offset + greenTime + yellowTime), e -> {
                light.setCurrentColor(LightColor.RED);
                updateLightView(light);
            })
        );

        // Red to Green (complete cycle)
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(offset + greenTime + yellowTime + redTime), e -> {
                light.setCurrentColor(LightColor.GREEN);
                updateLightView(light);
            })
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        lightTimelines.put(light, timeline);

        // Set initial state (start with RED until offset kicks in)
        light.setCurrentColor(LightColor.RED);
        updateLightView(light);
    }

    /**
     * Sets all traffic lights to the specified color
     */
    private void setAllLightsColor(LightColor color) {
        for (TrafficLight light : trafficLights) {
            light.setCurrentColor(color);
        }
    }

    /**
     * Updates all traffic light views
     */
    private void updateAllViews() {
        for (TrafficLightView view : trafficLightViews.values()) {
            view.animateTransition();
        }
    }

    /**
     * Updates a specific traffic light view
     */
    private void updateLightView(TrafficLight light) {
        TrafficLightView view = trafficLightViews.get(light.getId());
        if (view != null) {
            view.animateTransition();
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
    public void setLightOffset(String lightId, int offsetSeconds) {
        lightOffsets.put(lightId, offsetSeconds);

        // Restart control if running to apply new offset
        if (isRunning && currentMode == TimingMode.OFFSET) {
            stopControl();
            startControl();
        }
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
     * Stops all traffic light control
     */
    public void stopControl() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        // Stop all timelines
        for (Timeline timeline : lightTimelines.values()) {
            timeline.stop();
        }
        lightTimelines.clear();

        System.out.println("Traffic Controller stopped");
    }

    /**
     * Legacy method for managing intersections (kept for compatibility)
     */
    private void manageIntersections() {
        for (Intersection intersection : intersections) {
            Vehicle nextVehicle = intersection.getNextVehicle();
            if (nextVehicle != null) {
                // Logic for managing vehicle crossing
                // TODO: Integrate with traffic light states
            }
        }
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
    public Map<String, LightColor> getAllLightStates() {
        Map<String, LightColor> states = new HashMap<>();
        for (TrafficLight light : trafficLights) {
            states.put(light.getId(), light.getCurrentColor());
        }
        return states;
    }

    /**
     * Checks if a traffic light allows traffic to pass
     * Yellow is considered as GREEN for backend logic (vehicles can pass)
     * @param lightId ID of the traffic light to check
     * @return true if traffic can pass (GREEN or YELLOW), false if RED
     */
    public boolean canTrafficPass(String lightId) {
        TrafficLight light = getTrafficLight(lightId);
        if (light == null) {
            return false; // Safe default - no light means no pass
        }

        LightColor currentColor = light.getCurrentColor();
        // Both GREEN and YELLOW allow traffic to pass
        return currentColor == LightColor.GREEN || currentColor == LightColor.YELLOW;
    }

    /**
     * Gets the current traffic phase
     * @return Current phase (WEST_FLOW or EAST_FLOW)
     */
    public TrafficPhase getCurrentPhase() {
        return currentPhase;
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
        return String.format("Estado: Activo | Modo: %s | Fase: %s", currentMode, phaseStr);
    }
}
