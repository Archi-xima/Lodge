package com.lodge.controller;

import com.lodge.model.Role;
import com.lodge.service.AuthService;
import com.lodge.service.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class AdminPanelController {

    @FXML private Label statusLabel;
    @FXML private Label dataDirectoryLabel;
    @FXML private Label currentUserLabel;

    @FXML
    public void initialize() {
        var auth = AuthService.getInstance();
        var user = auth.getCurrentUser();

        boolean allowed = user != null
                && auth.hasPermission(Role.Permission.MANAGE_USERS);

        if (!allowed) {
            statusLabel.setText(
                    "Access denied: administrator privileges required.");
            return;
        }

        currentUserLabel.setText(user.getFullName());
        dataDirectoryLabel.setText(
                DataStore.getInstance().getDataDir().toString());
        statusLabel.setText(
                "Administrator settings — configuration is read-only.");
    }


    @FXML
    private void handleShowSecurityInfo() {
        if (!AuthService.getInstance()
                .hasPermission(Role.Permission.MANAGE_USERS)) {
            statusLabel.setText("Administrator access required.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Security information");
        alert.setHeaderText("Lodge account security");
        alert.setContentText(
                "Staff creation is administrator-only.\n"
                        + "Passwords are stored as hashes.\n"
                        + "Inactive accounts cannot sign in.\n"
                        + "Sessions expire after 15 minutes of inactivity.\n"
                        + "Configuration changes are not enabled in this screen.");
        alert.showAndWait();
    }
}
