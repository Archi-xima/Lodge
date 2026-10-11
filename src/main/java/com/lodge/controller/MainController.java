package com.lodge.controller;

import com.lodge.model.Bill;
import com.lodge.model.RoomType;
import com.lodge.model.User;
import com.lodge.service.AuthService;
import com.lodge.service.BillingService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;

public class MainController {

    // User info & Header
    @FXML private Label currentUserLabel;
    @FXML private Label userRoleBadge;

    // Action Toolbar
    @FXML private Button newGuestBtn;
    @FXML private Button viewReceiptBtn;
    @FXML private Button deleteBillBtn;
    @FXML private Button signOutBtn;

    // Form inputs
    @FXML private TextField guestNameField;
    @FXML private ComboBox<RoomType> roomTypeComboBox;
    @FXML private TextField nightsField;
    @FXML private TextField extrasField;
    @FXML private TextField paidField;
    @FXML private TextField notesField;
    @FXML private Button calculateBtn;
    @FXML private Button saveBillBtn;

    // Feedback status label
    @FXML private Label statusMessageLabel;

    // TableView and Columns
    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, String> colBillId;
    @FXML private TableColumn<Bill, String> colGuestName;
    @FXML private TableColumn<Bill, String> colRoomType;
    @FXML private TableColumn<Bill, Number> colNights;
    @FXML private TableColumn<Bill, String> colTotal;
    @FXML private TableColumn<Bill, String> colPaid;
    @FXML private TableColumn<Bill, String> colBalance;

    private final BillingService billingService = BillingService.getInstance();
    private final AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        setupUserHeader();
        setupRoomTypeComboBox();
        setupTable();
        setupRowSelection();
        loadInitialState();
    }

    private void setupUserHeader() {
        User user = authService.getCurrentUser();
        if (user != null) {
            if (currentUserLabel != null) currentUserLabel.setText(user.getFullName());
            if (userRoleBadge != null) userRoleBadge.setText(user.getRole());
        }
    }

    private void setupRoomTypeComboBox() {
        roomTypeComboBox.getItems().setAll(RoomType.values());
        roomTypeComboBox.setValue(RoomType.DELUXE);
    }

    private void setupTable() {
        colBillId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBillId()));
        colGuestName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGuestName()));
        colRoomType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoomTypeName()));
        colNights.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getNights()));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedTotal()));
        colPaid.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedPaid()));
        colBalance.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedBalance()));

        billsTable.setItems(billingService.getSavedBills());

        // Double-click row to view receipt
        billsTable.setRowFactory(tv -> {
            TableRow<Bill> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openReceiptDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupRowSelection() {
        billsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            if (viewReceiptBtn != null) viewReceiptBtn.setDisable(!hasSelection);
            if (deleteBillBtn != null) deleteBillBtn.setDisable(!hasSelection);
        });
    }

    private void loadInitialState() {
        // Set defaults matching screenshot sample
        guestNameField.setText("John Phiri");
        roomTypeComboBox.setValue(RoomType.DELUXE);
        nightsField.setText("5");
        extrasField.setText("200");
        paidField.setText("1000");
        notesField.setText("");

        statusMessageLabel.setText("Saved B003: Balance: K 1812.50");
        statusMessageLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");

        // Select the last bill by default
        ObservableList<Bill> list = billingService.getSavedBills();
        if (!list.isEmpty()) {
            billsTable.getSelectionModel().select(list.get(list.size() - 1));
        }
    }

    @FXML
    private void handleCalculate(ActionEvent event) {
        try {
            ValidationResult vr = validateInputs();
            if (!vr.isValid) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", vr.errorMessage);
                return;
            }

            Bill preview = new Bill("PREVIEW", vr.guestName, vr.roomType, vr.nights, vr.extras, vr.paid, vr.notes, "Staff");
            String msg = String.format("Calculated Total: %s | Paid: %s | Balance Due: %s",
                    preview.getFormattedTotal(), preview.getFormattedPaid(), preview.getFormattedBalance());

            statusMessageLabel.setText(msg);
            statusMessageLabel.setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold;");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Calculation Summary");
            alert.setHeaderText("Bill Estimate for " + preview.getGuestName());
            alert.setContentText(
                    "Room: " + preview.getRoomTypeName() + " (" + preview.getFormattedRate() + " / night)\n" +
                    "Nights: " + preview.getNights() + "\n" +
                    "Room Charge: " + preview.getFormattedRoomCharge() + "\n" +
                    "Discount: " + preview.getFormattedDiscount() + (preview.getDiscount() > 0 ? " (5% applied)" : "") + "\n" +
                    "Extras: " + preview.getFormattedExtras() + "\n" +
                    "-----------------------------------\n" +
                    "Total: " + preview.getFormattedTotal() + "\n" +
                    "Amount Paid: " + preview.getFormattedPaid() + "\n" +
                    "Balance: " + preview.getFormattedBalance()
            );
            alert.showAndWait();

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Calculation Error", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveBill(ActionEvent event) {
        ValidationResult vr = validateInputs();
        if (!vr.isValid) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", vr.errorMessage);
            return;
        }

        String creator = authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : "Staff";
        String billId = billingService.generateNextBillId();
        Bill newBill = new Bill(billId, vr.guestName, vr.roomType, vr.nights, vr.extras, vr.paid, vr.notes, creator);

        billingService.addBill(newBill);
        billsTable.getSelectionModel().select(newBill);
        billsTable.scrollTo(newBill);

        // Update status message in green like screenshot
        statusMessageLabel.setText(String.format("Saved %s: Balance: %s", newBill.getBillId(), newBill.getFormattedBalance()));
        statusMessageLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
    }

    @FXML
    private void handleNewGuest(ActionEvent event) {
        guestNameField.clear();
        roomTypeComboBox.setValue(RoomType.STANDARD);
        nightsField.setText("1");
        extrasField.setText("0");
        paidField.setText("0");
        notesField.clear();
        guestNameField.requestFocus();

        statusMessageLabel.setText("Ready for new guest entry.");
        statusMessageLabel.setStyle("-fx-text-fill: #475569; -fx-font-weight: normal;");
    }

    @FXML
    private void handleViewReceipt(ActionEvent event) {
        Bill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No Selection", "Please select a bill from the table to view receipt.");
            return;
        }
        openReceiptDialog(selected);
    }

    @FXML
    private void handleDeleteSelectedBill(ActionEvent event) {
        Bill selected = billsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "No Selection", "Please select a bill from the table to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Bill " + selected.getBillId() + "?");
        confirm.setContentText("Are you sure you want to delete the bill for guest: " + selected.getGuestName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String id = selected.getBillId();
            billingService.removeBill(selected);
            statusMessageLabel.setText("Bill " + id + " deleted.");
            statusMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleSignOut(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sign Out");
        confirm.setHeaderText("Sign Out of Lodge Billing Lab?");
        confirm.setContentText("Are you sure you want to log out?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            authService.logout();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/login.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) guestNameField.getScene().getWindow();
                Scene scene = new Scene(root, 900, 620);
                scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());

                root.setOpacity(0);
                stage.setScene(scene);
                stage.setTitle("Sign In - Lodge Billing Lab");
                stage.centerOnScreen();

                FadeTransition ft = new FadeTransition(Duration.millis(300), root);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Lodge Billing Lab");
        alert.setHeaderText("Mulungushi University - ICT261 Lecture 3");
        alert.setContentText(
                "Course: ICT261 Lecture 3\n" +
                "Lecturer: Mr E Nyirenda\n" +
                "Institution: Mulungushi University\n" +
                "Application: Lodge Billing Lab\n\n" +
                "Features:\n" +
                "• Room Rate Calculations (Standard, Deluxe, Family)\n" +
                "• 5-night stay 5% discount rule\n" +
                "• Extras & Payment processing\n" +
                "• Full Authentication (Sign In & Sign Out)\n" +
                "• Printable Receipt Generation & Bill Management"
        );
        alert.showAndWait();
    }

    @FXML
    private void handlePricingRules(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Practice Room Prices & Rules");
        alert.setHeaderText("Billing Rules Guide");
        alert.setContentText(
                "• Standard Room: K 350.00 per night\n" +
                "• Deluxe Room: K 550.00 per night\n" +
                "• Family Room: K 750.00 per night\n\n" +
                "Special Rule:\n" +
                "• 5 nights or more: 5% off the room charge.\n" +
                "• Extras get no discount.\n" +
                "• No tax applied in this lab.\n" +
                "• Saved bills persist during your session."
        );
        alert.showAndWait();
    }

    private void openReceiptDialog(Bill bill) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/receipt_dialog.fxml"));
            Parent root = loader.load();

            ReceiptDialogController controller = loader.getController();
            controller.setBill(bill);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Guest Receipt - " + bill.getBillId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(billsTable.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Receipt Error", "Could not load receipt: " + e.getMessage());
        }
    }

    private ValidationResult validateInputs() {
        String name = guestNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, "Please enter guest name.", null, null, 0, 0, 0, null);
        }

        RoomType room = roomTypeComboBox.getValue();
        if (room == null) {
            return new ValidationResult(false, "Please select a room type.", null, null, 0, 0, 0, null);
        }

        int nights;
        try {
            nights = Integer.parseInt(nightsField.getText().trim());
            if (nights <= 0) {
                return new ValidationResult(false, "Nights must be at least 1.", null, null, 0, 0, 0, null);
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Nights must be a valid whole number.", null, null, 0, 0, 0, null);
        }

        double extras = 0.0;
        String extrasText = extrasField.getText();
        if (extrasText != null && !extrasText.trim().isEmpty()) {
            try {
                extras = Double.parseDouble(extrasText.trim());
                if (extras < 0) {
                    return new ValidationResult(false, "Extras cannot be negative.", null, null, 0, 0, 0, null);
                }
            } catch (NumberFormatException e) {
                return new ValidationResult(false, "Extras in K must be a valid number.", null, null, 0, 0, 0, null);
            }
        }

        double paid = 0.0;
        String paidText = paidField.getText();
        if (paidText != null && !paidText.trim().isEmpty()) {
            try {
                paid = Double.parseDouble(paidText.trim());
                if (paid < 0) {
                    return new ValidationResult(false, "Amount paid cannot be negative.", null, null, 0, 0, 0, null);
                }
            } catch (NumberFormatException e) {
                return new ValidationResult(false, "Amount paid in K must be a valid number.", null, null, 0, 0, 0, null);
            }
        }

        String notes = notesField.getText() != null ? notesField.getText().trim() : "";
        return new ValidationResult(true, "", name.trim(), room, nights, extras, paid, notes);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class ValidationResult {
        final boolean isValid;
        final String errorMessage;
        final String guestName;
        final RoomType roomType;
        final int nights;
        final double extras;
        final double paid;
        final String notes;

        ValidationResult(boolean isValid, String errorMessage, String guestName, RoomType roomType, int nights, double extras, double paid, String notes) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.guestName = guestName;
            this.roomType = roomType;
            this.nights = nights;
            this.extras = extras;
            this.paid = paid;
            this.notes = notes;
        }
    }
}
