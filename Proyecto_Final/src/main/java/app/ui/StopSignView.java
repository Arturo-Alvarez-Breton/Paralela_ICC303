package app.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.effect.DropShadow;

/**
 * Componente visual para señales de PARE (STOP)
 * Representa una señal octagonal roja con texto blanco "PARE"
 */
public class StopSignView extends Group {
    
    private static final Color STOP_SIGN_RED = Color.web("#CC0000");
    private static final Color STOP_SIGN_BORDER = Color.WHITE;
    private static final Color STOP_TEXT_COLOR = Color.WHITE;
    private static final double DEFAULT_SIZE = 25.0;
    
    private final Polygon octagon;
    private final Text stopText;
    private final double size;
    
    /**
     * Constructor con tamaño por defecto
     * @param x Posición X del centro de la señal
     * @param y Posición Y del centro de la señal
     */
    public StopSignView(double x, double y) {
        this(x, y, DEFAULT_SIZE);
    }
    
    /**
     * Constructor con tamaño personalizable
     * @param x Posición X del centro de la señal
     * @param y Posición Y del centro de la señal
     * @param size Tamaño de la señal (radio aproximado)
     */
    public StopSignView(double x, double y, double size) {
        this.size = size;
        
        // Crear octágono
        this.octagon = createOctagon(x, y, size);
        
        // Crear texto "PARE"
        this.stopText = createStopText(x, y, size);
        
        // Agregar efectos visuales
        addVisualEffects();
        
        // Agregar componentes al grupo
        this.getChildren().addAll(octagon, stopText);
    }
    
    /**
     * Crea el octágono de la señal de PARE
     */
    private Polygon createOctagon(double centerX, double centerY, double radius) {
        Polygon octagon = new Polygon();
        
        // Calcular los 8 puntos del octágono
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8 + Math.PI / 8; // Rotar 22.5° para orientación correcta
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            octagon.getPoints().addAll(x, y);
        }
        
        // Estilo del octágono
        octagon.setFill(STOP_SIGN_RED);
        octagon.setStroke(STOP_SIGN_BORDER);
        octagon.setStrokeWidth(2.0);
        
        return octagon;
    }
    
    /**
     * Crea el texto "PARE" centrado en la señal
     */
    private Text createStopText(double centerX, double centerY, double size) {
        Text text = new Text("PARE");
        
        // Configurar fuente según el tamaño
        double fontSize = size * 0.4; // 40% del tamaño de la señal
        text.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        text.setFill(STOP_TEXT_COLOR);
        text.setTextAlignment(TextAlignment.CENTER);
        
        // Centrar el texto
        text.setX(centerX - text.getBoundsInLocal().getWidth() / 2);
        text.setY(centerY + text.getBoundsInLocal().getHeight() / 4);
        
        return text;
    }
    
    /**
     * Agrega efectos visuales como sombra
     */
    private void addVisualEffects() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(3.0);
        shadow.setOffsetX(2.0);
        shadow.setOffsetY(2.0);
        shadow.setColor(Color.color(0, 0, 0, 0.5));
        
        octagon.setEffect(shadow);
    }
    
    /**
     * Actualiza la posición de la señal
     * @param x Nueva posición X del centro
     * @param y Nueva posición Y del centro
     */
    public void setPosition(double x, double y) {
        // Limpiar puntos actuales del octágono
        octagon.getPoints().clear();
        
        // Recalcular puntos del octágono
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8 + Math.PI / 8;
            double newX = x + size * Math.cos(angle);
            double newY = y + size * Math.sin(angle);
            octagon.getPoints().addAll(newX, newY);
        }
        
        // Reposicionar texto
        stopText.setX(x - stopText.getBoundsInLocal().getWidth() / 2);
        stopText.setY(y + stopText.getBoundsInLocal().getHeight() / 4);
    }
    
    /**
     * Obtiene el tamaño actual de la señal
     * @return Tamaño (radio) de la señal
     */
    public double getSize() {
        return size;
    }
    
    /**
     * Crea una señal de PARE con orientación específica para una dirección
     * @param x Posición X
     * @param y Posición Y
     * @param size Tamaño de la señal
     * @param direction Dirección de la calle ("north", "south", "east", "west")
     * @return Nueva instancia de StopSignView
     */
    public static StopSignView createForDirection(double x, double y, double size, String direction) {
        StopSignView stopSign = new StopSignView(x, y, size);
        
        // Opcional: rotar la señal según la dirección si es necesario
        switch (direction.toLowerCase()) {
            case "north":
                // Sin rotación - orientación por defecto
                break;
            case "south":
                stopSign.setRotate(180);
                break;
            case "east":
                stopSign.setRotate(90);
                break;
            case "west":
                stopSign.setRotate(-90);
                break;
        }
        
        return stopSign;
    }
}