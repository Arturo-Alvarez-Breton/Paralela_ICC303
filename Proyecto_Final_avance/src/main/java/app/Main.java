package app;

import app.ui.LaunchView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Crear y mostrar la pantalla de bienvenida
        LaunchView launch = new LaunchView();
        primaryStage.setTitle("Simulador de Tráfico");
        primaryStage.setScene(launch.createLaunchScene(primaryStage));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

