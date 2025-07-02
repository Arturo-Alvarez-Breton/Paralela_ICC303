package app.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainView {
    public void start(Stage primaryStage) {
        IntersectionView intersectionView = new IntersectionView();

        Scene scene = new Scene(intersectionView);
        primaryStage.setTitle("Simulador de Interseccion");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
