package com.lodge.controller;

import com.lodge.model.Bill;
import com.lodge.model.BillLine;
import com.lodge.model.Payment;
import com.lodge.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Printable guest receipt.
 */
public class ReceiptDialogController {

    @FXML private VBox receiptPane;
    @FXML private Label billIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label guestLabel;
    @FXML private Label phoneLabel;
    @FXML private Label roomLabel;
    @FXML private Label stayLabel;
    @FXML private Label statusBadgeLabel;
    @FXML private Label notesLabel;
    @FXML private Label servedByLabel;

    @FXML private TableView<BillLine> linesTable;
    @FXML private TableColumn<BillLine, String> colRDesc;
    @FXML private TableColumn<BillLine, Number> colRQty;
    @FXML private TableColumn<BillLine, String> colRUnit;
    @FXML private TableColumn<BillLine, String> colRTotal;

    @FXML private Label roomChargeLabel;
    @FXML private Label discountLabel;
    @FXML private Label extrasLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label paidLabel;
    @FXML private Label balanceLabel;
    @FXML private Label paymentsLabel;

    @FXML private Button closeButton;

    private Bill bill;

    @FXML
    public void initialize() {
        colRDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colRQty.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getQuantity()));
        colRUnit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedUnitPrice()));
        colRTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedLineTotal()));
    }

    public void setBill(Bill bill) {
        if (bill == null) {
            return;
        }
        this.bill = bill;

        billIdLabel.setText(bill.getBillId());
        dateLabel.setText(bill.getFormattedDate());
        guestLabel.setText(bill.getGuestName());
        phoneLabel.setText(bill.getPhone() == null || bill.getPhone().isEmpty() ? "—" : bill.getPhone());
        roomLabel.setText(bill.getRoomNumber() + " — " + bill.getRoomTypeName());
        stayLabel.setText(bill.getNights() + " night(s) @ " + Money.format(bill.getNightlyRate()));

        linesTable.getItems().setAll(bill.getLines());

        roomChargeLabel.setText(bill.getFormattedRoomCharge());
        discountLabel.setText(bill.getLongStayDiscount() > 0 ? bill.getFormattedLongStayDiscount() : Money.format(0));
        discountLabel.getStyleClass().setAll(bill.getLongStayDiscount() > 0 ? "success-text" : "muted-text");
        extrasLabel.setText(bill.getFormattedExtras());
        subtotalLabel.setText(bill.getFormattedSubtotal());
        taxLabel.setText(bill.getFormattedTax() + "  (16% VAT)");
        totalLabel.setText(bill.getFormattedTotal());
        paidLabel.setText(bill.getFormattedPaid());
        balanceLabel.setText(bill.getFormattedBalance());

        StringBuilder payments = new StringBuilder();
        for (Payment payment : bill.getPayments()) {
            payments.append(payment.getFormattedDate()).append("  ")
                    .append(payment.getMethodLabel()).append("  ")
                    .append(payment.getFormattedAmount())
                    .append(payment.getReference() == null || payment.getReference().isEmpty()
                            ? "" : "  (" + payment.getReference() + ")")
                    .append("\n");
        }
        paymentsLabel.setText(payments.length() == 0 ? "No payments received yet." : payments.toString().trim());

        switch (bill.getStatus()) {
            case PAID:
                statusBadgeLabel.setText("PAID IN FULL");
                statusBadgeLabel.getStyleClass().setAll("status-pill", "status-green");
                break;
            case PART_PAID:
                statusBadgeLabel.setText("PART PAID");
                statusBadgeLabel.getStyleClass().setAll("status-pill", "status-amber");
                break;
            case VOID:
                statusBadgeLabel.setText("VOID");
                statusBadgeLabel.getStyleClass().setAll("status-pill", "status-grey");
                break;
            default:
                statusBadgeLabel.setText("BALANCE OUTSTANDING");
                statusBadgeLabel.getStyleClass().setAll("status-pill", "status-red");
                break;
        }

        notesLabel.setText(bill.getNotes() == null || bill.getNotes().isEmpty() ? "No notes recorded" : bill.getNotes());
        servedByLabel.setText("Prepared by: " + bill.getCreatedBy());
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
