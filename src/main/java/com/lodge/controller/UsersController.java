package com.lodge.controller;

import com.lodge.model.Role;
import com.lodge.model.User;
import com.lodge.service.AuthService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UsersController implements ViewController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableView<User> customersTable;
    @FXML private TableColumn<User, String> colCustomerUsername;
    @FXML private TableColumn<User, String> colCustomerName;
    @FXML private TableColumn<User, String> colCustomerStatus;

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private Button createAccountButton;
    @FXML private Button deleteCustomerButton;
    @FXML private Label messageLabel;

    @Override
    public void onShown() {
        refreshUsers();
        updateAccess();
    }

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoleLabel()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Inactive"));
        colCustomerUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colCustomerName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colCustomerStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isActive() ? "Active" : "Inactive"));

        roleCombo.getItems().setAll(Role.RECEPTIONIST, Role.MANAGER, Role.ACCOUNTANT, Role.ADMIN);
        roleCombo.setValue(Role.RECEPTIONIST);
        createAccountButton.setOnAction(e -> handleCreateAccount());
        deleteCustomerButton.setOnAction(e -> handleDeleteCustomer());
        customersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, selection) -> deleteCustomerButton.setDisable(
                        selection == null || !AuthService.getInstance().hasPermission(Role.Permission.MANAGE_USERS)));
        refreshUsers();
        updateAccess();
    }

    private void refreshUsers() {
        ObservableList<User> staff = FXCollections.observableArrayList(
                AuthService.getInstance().getUsers().stream()
                        .filter(user -> user.getRole() != Role.CUSTOMER).toList());
        ObservableList<User> customers = FXCollections.observableArrayList(
                AuthService.getInstance().getUsers().stream()
                        .filter(user -> user.getRole() == Role.CUSTOMER).toList());
        usersTable.setItems(staff);
        customersTable.setItems(customers);
        deleteCustomerButton.setDisable(customersTable.getSelectionModel().getSelectedItem() == null);
    }

    private void updateAccess() {
        boolean canManage = AuthService.getInstance().hasPermission(Role.Permission.MANAGE_USERS);
        createAccountButton.setDisable(!canManage);
        usernameField.setDisable(!canManage);
        fullNameField.setDisable(!canManage);
        passwordField.setDisable(!canManage);
        roleCombo.setDisable(!canManage);
        deleteCustomerButton.setDisable(!canManage
                || customersTable.getSelectionModel().getSelectedItem() == null);
        if (!canManage) {
            messageLabel.setText("Only the administrator can create staff accounts.");
            messageLabel.getStyleClass().setAll("muted-text");
        } else {
            messageLabel.setText("Create a new staff account for the lodge.");
            messageLabel.getStyleClass().setAll("muted-text");
        }
    }

    @FXML
    private void handleCreateAccount() {
        if (!AuthService.getInstance().hasPermission(Role.Permission.MANAGE_USERS)) {
            showError("Only the administrator can create staff accounts.");
            return;
        }

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        Role role = roleCombo.getValue() == null ? Role.RECEPTIONIST : roleCombo.getValue();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || password.length() < 8) {
            showError("Enter a username, full name, and a password of at least 8 characters.");
            return;
        }

        boolean created = AuthService.getInstance().register(username, password, fullName, role);
        if (created) {
            usernameField.clear();
            fullNameField.clear();
            passwordField.clear();
            roleCombo.setValue(Role.RECEPTIONIST);
            showSuccess("Account created successfully.");
            refreshUsers();
        } else {
            showError("That username already exists or the account could not be saved.");
        }
    }

    @FXML
    private void handleDeleteCustomer() {
        if (!AuthService.getInstance().hasPermission(Role.Permission.MANAGE_USERS)) {
            showError("Only the administrator can delete customer accounts.");
            return;
        }
        User customer = customersTable.getSelectionModel().getSelectedItem();
        if (customer == null || customer.getRole() != Role.CUSTOMER) {
            showError("Select a customer account to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete customer account");
        confirmation.setHeaderText("Delete the account for " + customer.getFullName() + "?");
        confirmation.setContentText("This prevents further sign-in. Historical bookings and the guest profile are retained.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            if (AuthService.getInstance().removeUser(customer)) {
                refreshUsers();
                showSuccess("Customer account deleted. Historical bookings were retained.");
            } else {
                showError("The customer account could not be deleted.");
            }
        } catch (RuntimeException e) {
            showError("The customer account could not be saved as deleted: " + e.getMessage());
        }
    }

    private void showError(String text) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().setAll("error-text");
    }

    private void showSuccess(String text) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().setAll("success-text");
    }
}
