package com.lodge.controller;

import com.lodge.service.AuthService;
import com.lodge.model.Role;
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

/**
 * Sign in / registration screen.
 */
public class LoginController {

    @FXML private VBox signInForm;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label loginErrorLabel;
    @FXML private Button signInButton;

    @FXML private VBox adminSetupForm;
    @FXML private TextField adminNameField;
    @FXML private TextField adminUsernameField;
    @FXML private PasswordField adminPasswordField;
    @FXML private Label adminSetupMessageLabel;
    @FXML private Button createAdminButton;
    @FXML private VBox customerRegistrationForm;
    @FXML private TextField customerNameField;
    @FXML private TextField customerEmailField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerUsernameField;
    @FXML private PasswordField customerPasswordField;
    @FXML private Label customerRegistrationMessageLabel;

    @FXML
    public void initialize() {
        if (passwordField != null) {
            passwordField.setOnAction(this::handleSignIn);
            if (usernameField != null) {
                usernameField.setOnAction(e -> passwordField.requestFocus());
            }
        }
        if (AuthService.getInstance().needsInitialAdministratorSetup()) {
            signInForm.setVisible(false);
            signInForm.setManaged(false);
            adminSetupForm.setVisible(true);
            adminSetupForm.setManaged(true);
        }
    }

    @FXML
    private void handleSignIn(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showError("Please enter both your username and password.");
            return;
        }

        try {
            if (AuthService.getInstance().login(username, password)) {
                loginErrorLabel.setText("");
                if (AuthService.getInstance().getCurrentUser().getRole() == Role.CUSTOMER) {
                    navigateToCustomer();
                } else {
                    navigateToMain();
                }
            } else {
                showError("Those sign-in details were not recognised. Please try again.");
            }
        } catch (RuntimeException e) {
            showError("Sign-in could not be completed because the local data could not be saved.");
        }
    }

    @FXML
    private void handleShowCustomerRegistration(ActionEvent event) {
        signInForm.setVisible(false);
        signInForm.setManaged(false);
        customerRegistrationForm.setVisible(true);
        customerRegistrationForm.setManaged(true);
        customerNameField.requestFocus();
    }

    @FXML
    private void handleReturnToSignIn(ActionEvent event) {
        customerRegistrationForm.setVisible(false);
        customerRegistrationForm.setManaged(false);
        signInForm.setVisible(true);
        signInForm.setManaged(true);
        loginErrorLabel.setText("");
    }

    @FXML
    private void handleCreateCustomer(ActionEvent event) {
        String fullName = customerNameField.getText();
        String email = customerEmailField.getText();
        String phone = customerPhoneField.getText();
        String username = customerUsernameField.getText();
        String password = customerPasswordField.getText();
        if (fullName == null || fullName.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || username == null || username.trim().isEmpty()
                || password == null || password.trim().length() < 8) {
            showCustomerRegistrationError("Enter your name, email, username, and a password of at least 8 characters.");
            return;
        }

        try {
            if (!AuthService.getInstance().registerCustomer(username, password, fullName, email, phone)) {
                showCustomerRegistrationError("That username is already in use or the details are invalid.");
                return;
            }
            if (!AuthService.getInstance().login(username, password)) {
                showCustomerRegistrationError("Your account was created, but sign-in failed. Return to sign in.");
                return;
            }
            navigateToCustomer();
        } catch (RuntimeException e) {
            showCustomerRegistrationError("Account creation could not be saved: " + e.getMessage());
        }
    }

    private void showCustomerRegistrationError(String message) {
        customerRegistrationMessageLabel.setText(message);
        customerRegistrationMessageLabel.getStyleClass().setAll("error-text");
    }

    @FXML
    private void handleCreateAdministrator(ActionEvent event) {
        String fullName = adminNameField.getText();
        String username = adminUsernameField.getText();
        String password = adminPasswordField.getText();

        if (fullName == null || fullName.trim().isEmpty()
                || username == null || username.trim().isEmpty()
                || password == null || password.length() < 8) {
            adminSetupMessageLabel.setText("Enter your name, username, and a password of at least 8 characters.");
            adminSetupMessageLabel.getStyleClass().setAll("error-text");
            return;
        }

        try {
            if (!AuthService.getInstance().createInitialAdministrator(username, fullName, password)) {
                adminSetupMessageLabel.setText("An administrator already exists or that username is unavailable.");
                adminSetupMessageLabel.getStyleClass().setAll("error-text");
                return;
            }
        } catch (RuntimeException e) {
            adminSetupMessageLabel.setText("The administrator account could not be saved: " + e.getMessage());
            adminSetupMessageLabel.getStyleClass().setAll("error-text");
            return;
        }

        usernameField.setText(username.trim());
        passwordField.clear();
        adminPasswordField.clear();
        adminSetupForm.setVisible(false);
        adminSetupForm.setManaged(false);
        signInForm.setVisible(true);
        signInForm.setManaged(true);
        loginErrorLabel.setText("Administrator account created. Sign in to continue.");
        loginErrorLabel.getStyleClass().setAll("success-text");
        passwordField.requestFocus();
    }

    private void showError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.getStyleClass().setAll("error-text");
    }

    private void navigateToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signInButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());

            root.setOpacity(0);
            stage.setScene(scene);
            stage.setTitle("SkyRidge Lodge Billing System");
            stage.centerOnScreen();

            FadeTransition ft = new FadeTransition(Duration.millis(320), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (IOException e) {
            AuthService.getInstance().logout();
            e.printStackTrace();
            showError("Error opening the main application: " + e.getMessage());
        }
    }

    private void navigateToCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/customer.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) signInButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Customer Booking — SkyRidge Lodge");
            stage.centerOnScreen();
        } catch (IOException e) {
            AuthService.getInstance().logout();
            e.printStackTrace();
            showCustomerRegistrationError("Error opening customer bookings: " + e.getMessage());
        }
    }
}
