package app.model;

/**
 * Representa una posición 2D con coordenadas exactas del sistema JavaFX original
 */
public class Position {
    private double x;
    private double y;
    
    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Position(Position other) {
        this.x = other.x;
        this.y = other.y;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Calcula la distancia euclidiana a otra posición de manera segura
     */
    public double distanceTo(Position other) {
        if (other == null) {
            return Double.MAX_VALUE; // Distancia infinita si la otra posición es null
        }
        
        try {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        } catch (Exception e) {
            return Double.MAX_VALUE; // En caso de cualquier error, devolver distancia máxima
        }
    }
    
    /**
     * Mueve la posición hacia otra posición por una distancia específica de manera segura
     */
    public void moveTowards(Position target, double distance) {
        if (target == null || distance <= 0) {
            return; // No hacer nada si el target es null o la distancia no es válida
        }
        
        try {
            double currentDistance = distanceTo(target);
            if (currentDistance <= distance || currentDistance == Double.MAX_VALUE) {
                // Si la distancia es menor o igual, llegar al destino
                this.x = target.x;
                this.y = target.y;
            } else {
                // Calcular dirección normalizada
                double ratio = distance / currentDistance;
                double dx = (target.x - this.x) * ratio;
                double dy = (target.y - this.y) * ratio;
                
                this.x += dx;
                this.y += dy;
            }
        } catch (Exception e) {
            // Si hay cualquier error, no mover la posición
            System.err.println("Error moviendo posición: " + e.getMessage());
        }
    }
    
    @Override
    public String toString() {
        return String.format("Position(%.1f, %.1f)", x, y);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position)) return false;
        Position other = (Position) obj;
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }
    
    @Override
    public int hashCode() {
        return Double.hashCode(x) * 31 + Double.hashCode(y);
    }
}
