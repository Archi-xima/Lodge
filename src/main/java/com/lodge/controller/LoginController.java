package com.lodge.controller;

import com.lodge.service.AuthService;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML private VBox signInForm;
    @FXML private VBox signUpForm;

    // Sign In fields
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label loginErrorLabel;
    @FXML private Button signInButton;

    // Sign Up fields
    @FXML private TextField regFullNameField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private ComboBox<String> regRoleComboBox;
    @FXML private Label regMessageLabel;

    @FXML
    public void initialize() {
        if (regRoleComboBox != null) {
            regRoleComboBox.getItems().addAll("Staff", "Administrator", "Student");
            regRoleComboBox.setValue("Staff");
        }

        // Allow pressing Enter in password field to login
        if (passwordField != null) {
            passwordField.setOnAction(this::handleSignIn);
        }
    }

    @FXML
    private void handleSignIn(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        boolean success = AuthService.getInstance().login(username, password);
        if (success) {
            loginErrorLabel.setText("");
            navigateToMain();
        } else {
            showError("Invalid credentials. Try admin / admin123 or register.");
        }
    }

    @FXML
    private void handleDemoAdmin() {
        usernameField.setText("admin");
        passwordField.setText("admin123");
        handleSignIn(null);
    }

    @FXML
    private void handleDemoReception() {
        usernameField.setText("reception");
        passwordField.setText("lodge123");
        handleSignIn(null);
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        String fullName = regFullNameField.getText();
        String username = regUsernameField.getText();
        String password = regPasswordField.getText();
        String role = regRoleComboBox.getValue();

        if (fullName == null || fullName.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            regMessageLabel.setText("All fields are required!");
            regMessageLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        boolean created = AuthService.getInstance().register(username, password, fullName, role);
        if (created) {
            regMessageLabel.setText("Account created! Switching to Sign In...");
            regMessageLabel.setStyle("-fx-text-fill: #16a34a;");
            
            // Switch back to sign in after brief pause
            usernameField.setText(username);
            passwordField.setText(password);
            showSignInView();
        } else {
            regMessageLabel.setText("Username already exists. Choose another.");
            regMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void showSignUpView() {
        signInForm.setVisible(false);
        signInForm.setManaged(false);
        signUpForm.setVisible(true);
        signUpForm.setManaged(true);
        regMessageLabel.setText("");
    }

    @FXML
    private void showSignInView() {
        signUpForm.setVisible(false);
        signUpForm.setManaged(false);
        signInForm.setVisible(true);
        signInForm.setManaged(true);
        loginErrorLabel.setText("");
    }

    private void showError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
    }

    private void navigateToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signInButton.getScene().getWindow();
            Scene scene = new Scene(root, 1050, 720);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());

            // Gentle fade transition
            root.setOpacity(0);
            stage.setScene(scene);
            stage.setTitle("Lodge Billing Lab - ICT261 Lecture 3");
            stage.centerOnScreen();

            FadeTransition ft = new FadeTransition(Duration.millis(350), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error opening main application: " + e.getMessage());
        }
    }
}
