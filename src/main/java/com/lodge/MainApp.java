package com.lodge;

import com.lodge.util.Constants;
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.LOGIN_VIEW));
            Parent root = loader.load();

            Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);
            
            // Load all Member 2 stylesheets
java.net.URL cssUrl = getClass().getResource("/com/lodge/css/style.css");
if (cssUrl == null) {
    throw new IllegalStateException("Cannot find style.css on the classpath!");
}
scene.getStylesheets().add(cssUrl.toExternalForm());
            scene.getStylesheets().add(getClass().getResource(Constants.RESOURCE_CSS).toExternalForm());

            primaryStage.setTitle(Constants.SIGN_IN_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(Constants.MIN_WINDOW_WIDTH);
            primaryStage.setMinHeight(Constants.MIN_WINDOW_HEIGHT);
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(Constants.RESOURCE_IMAGE_DIR + "/hero-lodge.jpg")));
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
