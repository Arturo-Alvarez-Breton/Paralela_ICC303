package app.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LaunchView {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    public Scene createLaunchScene(Stage primaryStage) {
        // Título
        Label title = new Label("Simulación de Tráfico");
        // se aplican los estilos del titulo
        title.getStyleClass().add("title");

        // Botón Iniciar y asignacion de estilos al mismo
        Button btnStart = new Button("Iniciar Escenario 1");
        btnStart.getStyleClass().add("start-btn");
        btnStart.setOnAction(e -> {
            // Cuando pulso, cargo la escena de la intersección
            IntersectionView intersection = new IntersectionView();
            Scene intersectionScene = new Scene(intersection, WIDTH, HEIGHT);
            intersectionScene.getStylesheets().add(
                    getClass().getResource("/css/launchView.css").toExternalForm()
            );
            primaryStage.setScene(intersectionScene);
        });

        // Contenedor vertical
        VBox root = new VBox(40, title, btnStart);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(
                getClass().getResource("/css//launchView.css").toExternalForm()
        );
        return scene;
    }
}