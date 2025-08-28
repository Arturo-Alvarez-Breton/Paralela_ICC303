package app.model;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficLight {
    private String id;
    private AtomicBoolean isGreen;

    // Visual position for rendering
    private double posX;
    private double posY;

    public TrafficLight(String id) {
        this.id = id;
        this.isGreen = new AtomicBoolean(false); // Start with red
        this.posX = 0;
        this.posY = 0;
    }

    public String getId() {
        return id;
    }

    public boolean isGreen() {
        return isGreen.get();
    }

    public void setGreen(boolean green) {
        isGreen.set(green);
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public void setPosition(double x, double y) {
        this.posX = x;
        this.posY = y;
    }

    /**
     * Manually toggle the light state
     */
    public void toggle() {
        isGreen.set(!isGreen.get());
    }

    @Override
    public String toString() {
        return String.format("TrafficLight[id=%s, isGreen=%s]", id, isGreen.get());
    }
}
