package app.ui;

import app.model.TrafficLight;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Visual representation of a traffic light in JavaFX
 * Displays a vertical rectangle with two circles (red and green only)
 */
public class TrafficLightView extends Group {

    private final TrafficLight trafficLight;
    private final Rectangle background;
    private final Circle redLight;
    private final Circle greenLight;

    // Configurable dimensions
    private final double width;
    private final double height;
    private final double lightRadius;

    // Visual effects
    private final DropShadow glowEffect;

    // Colors
    private static final Color BACKGROUND_COLOR = Color.DARKGRAY;
    private static final Color ACTIVE_RED = Color.RED;
    private static final Color ACTIVE_GREEN = Color.LIME;
    private static final Color INACTIVE_COLOR = Color.color(0.2, 0.2, 0.2); // Dark gray

    public TrafficLightView(TrafficLight trafficLight) {
        this(trafficLight, 20, 40, 8); // Reduced height since no yellow light
    }

    public TrafficLightView(TrafficLight trafficLight, double width, double height, double lightRadius) {
        this.trafficLight = trafficLight;
        this.width = width;
        this.height = height;
        this.lightRadius = lightRadius;

        // Create glow effect
        this.glowEffect = new DropShadow();
        this.glowEffect.setRadius(8);
        this.glowEffect.setSpread(0.5);

        // Create visual components
        this.background = createBackground();
        this.redLight = createLight(0); // Top
        this.greenLight = createLight(1); // Bottom

        // Add all components to group
        this.getChildren().addAll(background, redLight, greenLight);

        // Set initial position
        this.setLayoutX(trafficLight.getPosX());
        this.setLayoutY(trafficLight.getPosY());

        // Update visual state
        updateLightState();
    }

    private Rectangle createBackground() {
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(BACKGROUND_COLOR);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(2);
        bg.setArcWidth(6);
        bg.setArcHeight(6);
        return bg;
    }

    private Circle createLight(int position) {
        double centerX = width / 2;
        double spacing = height / 3;
        double startY = spacing;
        double y = startY + (position * spacing);

        Circle light = new Circle(centerX, y, lightRadius);
        light.setFill(INACTIVE_COLOR);
        light.setStroke(Color.DARKSLATEGRAY);
        light.setStrokeWidth(1);

        return light;
    }

    /**
     * Updates the visual state based on the traffic light's current state
     */
    public void updateLightState() {
        // Reset all lights to inactive
        setLightInactive(redLight);
        setLightInactive(greenLight);

        // Activate the current light with glow effect
        if (trafficLight.isGreen()) {
            setLightActive(greenLight, ACTIVE_GREEN, Color.DARKGREEN);
        } else {
            setLightActive(redLight, ACTIVE_RED, Color.DARKRED);
        }
    }

    private void setLightActive(Circle light, Color fillColor, Color glowColor) {
        light.setFill(fillColor);
        glowEffect.setColor(glowColor);
        light.setEffect(glowEffect);
    }

    private void setLightInactive(Circle light) {
        light.setFill(INACTIVE_COLOR);
        light.setEffect(null);
    }

    /**
     * Animates the transition between light states
     * @param duration Duration of the fade animation
     */
    public void animateTransition(Duration duration) {
        // Fade out current lights
        FadeTransition fadeOut = new FadeTransition(duration.divide(2), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.3);

        // Fade in new state
        FadeTransition fadeIn = new FadeTransition(duration.divide(2), this);
        fadeIn.setFromValue(0.3);
        fadeIn.setToValue(1.0);

        // Chain animations
        fadeOut.setOnFinished(e -> {
            updateLightState();
            fadeIn.play();
        });

        fadeOut.play();
    }

    /**
     * Animates the transition with default duration (500ms)
     */
    public void animateTransition() {
        animateTransition(Duration.millis(500));
    }

    /**
     * Updates the position of the traffic light
     */
    public void updatePosition() {
        this.setLayoutX(trafficLight.getPosX());
        this.setLayoutY(trafficLight.getPosY());
    }

    /**
     * Gets the associated traffic light model
     */
    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    /**
     * Gets the width of the traffic light view
     */
    public double getTrafficLightWidth() {
        return width;
    }

    /**
     * Gets the height of the traffic light view
     */
    public double getTrafficLightHeight() {
        return height;
    }
}
