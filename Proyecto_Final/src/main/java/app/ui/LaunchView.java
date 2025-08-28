package app.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LaunchView {

    public static final int WIDTH = 1600;  // Aumentado de 1280 a 1600 (320 píxeles más)
    public static final int HEIGHT = 900;  // Aumentado de 720 a 900 (180 píxeles más)

    public Scene createLaunchScene(Stage primaryStage) {
        // Título
        Label title = new Label("Simulación de Tráfico");
        // se aplican los estilos del titulo
        title.getStyleClass().add("title");

        // Botón Iniciar Escenario 1
        Button btnStart1 = new Button("Iniciar Escenario 1");
        btnStart1.getStyleClass().add("start-btn");
        btnStart1.setOnAction(e -> {
            IntersectionView intersection = new IntersectionView(1);
            Scene intersectionScene = new Scene(intersection, WIDTH, HEIGHT);
            intersectionScene.getStylesheets().add(
                    getClass().getResource("/css/launchView.css").toExternalForm()
            );
            primaryStage.setScene(intersectionScene);
        });

        // Botón Iniciar Escenario 2 (Autopista)
        Button btnStart2 = new Button("Iniciar Escenario 2 (Autopista)");
        btnStart2.getStyleClass().add("start-btn");
        btnStart2.setOnAction(e -> {
            IntersectionView highway = new IntersectionView(2);
            Scene highwayScene = new Scene(highway, WIDTH, HEIGHT);
            highwayScene.getStylesheets().add(
                    getClass().getResource("/css/launchView.css").toExternalForm()
            );
            primaryStage.setScene(highwayScene);
        });

        // Botón Salir de la aplicación
        Button btnExit = new Button("Salir");
        btnExit.getStyleClass().add("exit-btn");
        btnExit.setOnAction(e -> {
            Platform.exit(); // Cierra la aplicación JavaFX
        });

        // Contenedor vertical
        VBox root = new VBox(30, title, btnStart1, btnStart2, btnExit);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(
                getClass().getResource("/css//launchView.css").toExternalForm()
        );
        return scene;
    }
}