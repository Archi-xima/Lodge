package com.lodge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Entry point of the Lodge Billing System.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1080, 720);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());

            primaryStage.setTitle("Sign In — SkyRidge Lodge Billing System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(640);
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/lodge/images/hero-lodge.jpg")));
            } catch (Exception ignored) {
                // icon is cosmetic only
            }
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
