package com.lodge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());

            primaryStage.setTitle("Sign In - Lodge Billing Lab (Mulungushi University)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
