package com.lodge.controller;

import com.lodge.model.Bill;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ReceiptDialogController {

    @FXML private Label receiptTitleLabel;
    @FXML private Label billIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label guestNameLabel;
    @FXML private Label roomTypeLabel;
    @FXML private Label rateLabel;
    @FXML private Label nightsLabel;
    @FXML private Label roomChargeLabel;
    @FXML private Label discountLabel;
    @FXML private Label extrasLabel;
    @FXML private Label totalLabel;
    @FXML private Label paidLabel;
    @FXML private Label balanceLabel;
    @FXML private Label statusBadgeLabel;
    @FXML private Label notesLabel;
    @FXML private Label servedByLabel;
    @FXML private Button closeButton;

    public void setBill(Bill bill) {
        if (bill == null) return;

        billIdLabel.setText(bill.getBillId());
        dateLabel.setText(bill.getFormattedDate());
        guestNameLabel.setText(bill.getGuestName());
        roomTypeLabel.setText(bill.getRoomTypeName());
        rateLabel.setText(bill.getFormattedRate() + " / night");
        nightsLabel.setText(String.valueOf(bill.getNights()));
        roomChargeLabel.setText(bill.getFormattedRoomCharge());
        
        if (bill.getDiscount() > 0) {
            discountLabel.setText("- " + bill.getFormattedDiscount() + " (5% discount)");
            discountLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
        } else {
            discountLabel.setText("K 0.00");
            discountLabel.setStyle("-fx-text-fill: #64748b;");
        }

        extrasLabel.setText(bill.getFormattedExtras());
        totalLabel.setText(bill.getFormattedTotal());
        paidLabel.setText(bill.getFormattedPaid());
        balanceLabel.setText(bill.getFormattedBalance());

        if (bill.getBalance() <= 0) {
            statusBadgeLabel.setText("PAID IN FULL");
            statusBadgeLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 12px; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else {
            statusBadgeLabel.setText("BALANCE OUTSTANDING");
            statusBadgeLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 12px; -fx-padding: 4 12; -fx-font-weight: bold;");
        }

        notesLabel.setText(bill.getNotes().isEmpty() ? "No notes recorded" : bill.getNotes());
        servedByLabel.setText("Prepared by: " + bill.getCreatedBy());
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
