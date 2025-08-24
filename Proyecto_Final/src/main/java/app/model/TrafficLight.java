package app.model;

import app.model.enums.LightColor;
import java.util.concurrent.atomic.AtomicReference;

public class TrafficLight {
    private String id;
    private AtomicReference<LightColor> currentColor;
    private int greenTime;  // seconds
    private int yellowTime; // seconds
    private int redTime;    // seconds

    // Visual position for rendering
    private double posX;
    private double posY;

    public TrafficLight(String id) {
        this.id = id;
        this.currentColor = new AtomicReference<>(LightColor.RED);
        // Default timing based on requirements
        this.greenTime = 5;
        this.yellowTime = 2;
        this.redTime = 7;
        this.posX = 0;
        this.posY = 0;
    }

    public TrafficLight(String id, int greenTime, int yellowTime, int redTime) {
        this.id = id;
        this.currentColor = new AtomicReference<>(LightColor.RED);
        this.greenTime = greenTime;
        this.yellowTime = yellowTime;
        this.redTime = redTime;
        this.posX = 0;
        this.posY = 0;
    }

    public String getId() {
        return id;
    }

    public LightColor getCurrentColor() {
        return currentColor.get();
    }

    public void setCurrentColor(LightColor color) {
        currentColor.set(color);
    }

    public int getGreenTime() {
        return greenTime;
    }

    public int getYellowTime() {
        return yellowTime;
    }

    public int getRedTime() {
        return redTime;
    }

    public void setGreenTime(int greenTime) {
        this.greenTime = greenTime;
    }

    public void setYellowTime(int yellowTime) {
        this.yellowTime = yellowTime;
    }

    public void setRedTime(int redTime) {
        this.redTime = redTime;
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
     * Get the total cycle time for this traffic light
     * @return Total seconds for one complete cycle
     */
    public int getTotalCycleTime() {
        return greenTime + yellowTime + redTime;
    }

    /**
     * Transition to the next color in the cycle
     */
    public void nextColor() {
        LightColor current = currentColor.get();
        switch (current) {
            case GREEN:
                currentColor.set(LightColor.YELLOW);
                break;
            case YELLOW:
                currentColor.set(LightColor.RED);
                break;
            case RED:
                currentColor.set(LightColor.GREEN);
                break;
        }
    }

    @Override
    public String toString() {
        return String.format("TrafficLight[id=%s, color=%s, cycle=%d+%d+%d]",
                           id, currentColor.get(), greenTime, yellowTime, redTime);
    }
}
