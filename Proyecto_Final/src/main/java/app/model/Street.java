package app.model;

import app.model.enums.DirectionEnum;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una calle dentro de la simulación.
 * Contiene su posición, dimensiones y direcciones posibles.
 */
public class Street {

    private String id;
    private List<DirectionEnum> possibleDirections;
    private int threshold; // Cantidad máxima de vehículos permitidos
    private int width;
    private int height;
    private int posX;
    private int posY;

    public Street(String id, List<DirectionEnum> possibleDirections, int threshold,
                  int width, int height, int posX, int posY) {
        this.id = id;
        this.possibleDirections = new ArrayList<>(possibleDirections);
        this.threshold = threshold;
        this.width = width;
        this.height = height;
        this.posX = posX;
        this.posY = posY;
    }

    public String getId() {
        return id;
    }

    public List<DirectionEnum> getPossibleDirections() {
        return possibleDirections;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }
}
