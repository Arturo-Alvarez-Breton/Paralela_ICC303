package app.model;


import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficLight {
    private String id;
    private AtomicBoolean green;

    public TrafficLight(String id) {
        this.id = id;
        this.green = new AtomicBoolean(false); // Default to red light
    }

    public void changeLight() {
        green.set(!green.get()); // Toggle the light state
    }

    public boolean isGreen() {
        return green.get();
    }
}
