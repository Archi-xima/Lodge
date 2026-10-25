package com.lodge.controller;

import com.lodge.model.*;
import com.lodge.service.AuthService;
import com.lodge.service.BillingService;
import com.lodge.service.GuestService;
import com.lodge.service.RoomService;
import com.lodge.util.Money;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Invoicing: walk-in bills, extras, part payments and receipts.
 */
public class BillingController implements ViewController {

    // walk-in form
    @FXML private TextField walkInGuestField;
    @FXML private TextField walkInPhoneField;
    @FXML private ComboBox<Room> walkInRoomCombo;
    @FXML private TextField walkInNightsField;
    @FXML private TextField walkInExtrasField;
    @FXML private TextField walkInNotesField;
    @FXML private Button createInvoiceBtn;

    // bills table
    @FXML private TextField billSearchField;
    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, String> colBillId;
    @FXML private TableColumn<Bill, String> colBillGuest;
    @FXML private TableColumn<Bill, String> colBillRoom;
    @FXML private TableColumn<Bill, String> colBillTotal;
    @FXML private TableColumn<Bill, String> colBillPaid;
    @FXML private TableColumn<Bill, String> colBillBalance;
    @FXML private TableColumn<Bill, String> colBillStatus;

    // detail pane
    @FXML private Label detailBillId;
    @FXML private Label detailGuest;
    @FXML private Label detailRoom;
    @FXML private Label detailStay;
    @FXML private Label detailStatus;
    @FXML private Label detailCreated;

    @FXML private TableView<BillLine> linesTable;
    @FXML private TableColumn<BillLine, String> colLineDesc;
    @FXML private TableColumn<BillLine, String> colLineCat;
    @FXML private TableColumn<BillLine, Number> colLineQty;
    @FXML private TableColumn<BillLine, String> colLineUnit;
    @FXML private TableColumn<BillLine, String> colLineTotal;

    @FXML private ComboBox<ServiceItem> extraServiceCombo;
    @FXML private TextField extraQtyField;
    @FXML private Button addExtraBtn;
    @FXML private Button removeLineBtn;

    @FXML private Label roomChargeLabel;
    @FXML private Label discountLabel;
    @FXML private Label extrasLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label paidLabel;
    @FXML private Label balanceLabel;

    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, String> colPaymentId;
    @FXML private TableColumn<Payment, String> colPaymentDate;
    @FXML private TableColumn<Payment, String> colPaymentMethod;
    @FXML private TableColumn<Payment, String> colPaymentAmount;
    @FXML private TableColumn<Payment, String> colPaymentRef;

    @FXML private TextField payAmountField;
    @FXML private ComboBox<PaymentMethod> payMethodCombo;
    @FXML private TextField payReferenceField;
    @FXML private Button recordPaymentBtn;

    @FXML private Button receiptBtn;
    @FXML private Button voidBillBtn;
    @FXML private Label statusLabel;

    private Bill selectedBill;

    @FXML
    public void initialize() {
        // ---- bills table
        colBillId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBillId()));
        colBillGuest.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getGuestName()));
        colBillRoom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRoomNumber() + " (" + d.getValue().getRoomTypeName() + ")"));
        colBillTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTotal()));
        colBillPaid.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedPaid()));
        colBillBalance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedBalance()));
        colBillStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatusLabel()));

        billsTable.setRowFactory(tv -> {
            TableRow<Bill> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openReceipt(row.getItem());
                }
            });
            return row;
        });
        billsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> showBill(n));
        billSearchField.textProperty().addListener((obs, o, n) -> refreshBillsTable());

        // ---- lines table
        colLineDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colLineCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryLabel()));
        colLineQty.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getQuantity()));
        colLineUnit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedUnitPrice()));
        colLineTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedLineTotal()));

        // ---- payments table
        colPaymentId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentId()));
        colPaymentDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedDate()));
        colPaymentMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMethodLabel()));
        colPaymentAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedAmount()));
        colPaymentRef.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReference()));

        // ---- combos
        payMethodCombo.getItems().addAll(PaymentMethod.values());
        payMethodCombo.setValue(PaymentMethod.CASH);
        extraQtyField.setText("1");

        receiptBtn.setDisable(true);
        voidBillBtn.setDisable(true);
        recordPaymentBtn.setDisable(true);
        addExtraBtn.setDisable(true);
        removeLineBtn.setDisable(true);
    }

    @Override
    public void onShown() {
        walkInRoomCombo.getItems().setAll(RoomService.getInstance().getRooms());
        if (walkInRoomCombo.getValue() == null && !walkInRoomCombo.getItems().isEmpty()) {
            walkInRoomCombo.setValue(walkInRoomCombo.getItems().get(0));
        }
        extraServiceCombo.getItems().setAll(BillingService.getInstance().getServiceCatalog());
        if (extraServiceCombo.getValue() == null && !extraServiceCombo.getItems().isEmpty()) {
            extraServiceCombo.setValue(extraServiceCombo.getItems().get(0));
        }
        refreshBillsTable();
    }

    private void refreshBillsTable() {
        String needle = billSearchField.getText() == null ? "" : billSearchField.getText().trim().toLowerCase();
        var filtered = BillingService.getInstance().getBills().filtered(bill ->
                needle.isEmpty()
                        || bill.getBillId().toLowerCase().contains(needle)
                        || (bill.getGuestName() != null && bill.getGuestName().toLowerCase().contains(needle))
                        || (bill.getRoomNumber() != null && bill.getRoomNumber().toLowerCase().contains(needle)));
        billsTable.getItems().setAll(filtered);
        if (selectedBill != null) {
            for (Bill bill : billsTable.getItems()) {
                if (bill.getBillId().equals(selectedBill.getBillId())) {
                    billsTable.getSelectionModel().select(bill);
                    return;
                }
            }
        }
    }

    private void showBill(Bill bill) {
        selectedBill = bill;
        boolean hasBill = bill != null;
        receiptBtn.setDisable(!hasBill);
        voidBillBtn.setDisable(!hasBill || bill.getStatus() == BillStatus.VOID);
        recordPaymentBtn.setDisable(!hasBill || bill.getStatus() == BillStatus.VOID);
        addExtraBtn.setDisable(!hasBill || bill.getStatus() == BillStatus.VOID);

        if (!hasBill) {
            detailBillId.setText("—");
            detailGuest.setText("Select an invoice");
            detailRoom.setText("—");
            detailStay.setText("—");
            detailStatus.setText("—");
            detailCreated.setText("—");
            linesTable.getItems().clear();
            paymentsTable.getItems().clear();
            roomChargeLabel.setText(Money.format(0));
            discountLabel.setText(Money.format(0));
            extrasLabel.setText(Money.format(0));
            subtotalLabel.setText(Money.format(0));
            taxLabel.setText(Money.format(0));
            totalLabel.setText(Money.format(0));
            paidLabel.setText(Money.format(0));
            balanceLabel.setText(Money.format(0));
            return;
        }

        detailBillId.setText(bill.getBillId());
        detailGuest.setText(bill.getGuestName() + (bill.getPhone() == null || bill.getPhone().isEmpty() ? "" : "  •  " + bill.getPhone()));
        detailRoom.setText(bill.getRoomNumber() + " — " + bill.getRoomTypeName());
        detailStay.setText(bill.getNights() + " night(s) @ " + Money.format(bill.getNightlyRate()));
        detailStatus.setText(bill.getStatusLabel());
        detailStatus.getStyleClass().setAll("status-pill", statusStyle(bill.getStatus()));
        detailCreated.setText(bill.getFormattedDate() + "  •  by " + bill.getCreatedBy());

        linesTable.getItems().setAll(bill.getLines());
        paymentsTable.getItems().setAll(bill.getPayments());

        roomChargeLabel.setText(bill.getFormattedRoomCharge());
        discountLabel.setText(bill.getLongStayDiscount() > 0
                ? bill.getFormattedLongStayDiscount() + "  (5% over " + (Bill.LONG_STAY_MIN_NIGHTS - 1) + " nights)"
                : Money.format(0));
        extrasLabel.setText(bill.getFormattedExtras());
        subtotalLabel.setText(bill.getFormattedSubtotal());
        taxLabel.setText(bill.getFormattedTax() + "  (16% VAT)");
        totalLabel.setText(bill.getFormattedTotal());
        paidLabel.setText(bill.getFormattedPaid());
        balanceLabel.setText(bill.getFormattedBalance());

        removeLineBtn.setDisable(linesTable.getSelectionModel().getSelectedItem() == null);
        linesTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> removeLineBtn.setDisable(b == null));

        payAmountField.setText(String.valueOf(Math.max(0, bill.getBalance())));
    }

    private String statusStyle(BillStatus status) {
        switch (status) {
            case PAID: return "status-green";
            case PART_PAID: return "status-amber";
            case VOID: return "status-grey";
            case UNPAID:
            default: return "status-red";
        }
    }

    // ------------------------------------------------------------------ actions

    @FXML
    private void handleCreateInvoice() {
        String name = text(walkInGuestField);
        if (name.isEmpty()) {
            warn("Please enter the guest's name.");
            return;
        }
        Room room = walkInRoomCombo.getValue();
        if (room == null) {
            warn("Please choose the room the guest occupied.");
            return;
        }
        int nights;
        double extras;
        try {
            nights = Integer.parseInt(text(walkInNightsField).isEmpty() ? "1" : text(walkInNightsField));
        } catch (NumberFormatException e) {
            warn("Nights must be a whole number.");
            return;
        }
        if (nights < 1) {
            warn("Nights must be at least 1.");
            return;
        }
        try {
            extras = text(walkInExtrasField).isEmpty() ? 0 : Money.parse(text(walkInExtrasField));
        } catch (NumberFormatException e) {
            warn("Extras must be a valid amount, e.g. 200.");
            return;
        }

        GuestService.getInstance().findOrCreate(name, text(walkInPhoneField));
        Bill bill = Bill.walkIn(BillingService.getInstance().nextBillId(), name, text(walkInPhoneField),
                room, nights, extras, text(walkInNotesField), AuthService.getInstance().currentUserName());
        BillingService.getInstance().addBill(bill);

        walkInGuestField.clear();
        walkInPhoneField.clear();
        walkInExtrasField.clear();
        walkInNotesField.clear();

        refreshBillsTable();
        billsTable.getSelectionModel().select(bill);
        billsTable.scrollTo(bill);
        setStatus("Invoice " + bill.getBillId() + " created — total " + bill.getFormattedTotal()
                + ", balance " + bill.getFormattedBalance() + ".", true);
    }

    @FXML
    private void handleAddExtra() {
        if (selectedBill == null) {
            return;
        }
        ServiceItem item = extraServiceCombo.getValue();
        if (item == null) {
            warn("Please choose an extra service.");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(text(extraQtyField).isEmpty() ? "1" : text(extraQtyField));
        } catch (NumberFormatException e) {
            warn("Quantity must be a whole number.");
            return;
        }
        if (quantity < 1) {
            warn("Quantity must be at least 1.");
            return;
        }
        BillingService.getInstance().addExtra(selectedBill, item, quantity);
        showBill(selectedBill);
        refreshBillsTable();
        billsTable.getSelectionModel().select(selectedBill);
        setStatus(quantity + " x " + item.getName() + " added to " + selectedBill.getBillId() + ".", true);
    }

    @FXML
    private void handleRemoveLine() {
        BillLine line = linesTable.getSelectionModel().getSelectedItem();
        if (selectedBill == null || line == null) {
            return;
        }
        BillingService.getInstance().removeLine(selectedBill, line);
        showBill(selectedBill);
        refreshBillsTable();
        billsTable.getSelectionModel().select(selectedBill);
        setStatus("Removed \"" + line.getDescription() + "\" from " + selectedBill.getBillId() + ".", true);
    }

    @FXML
    private void handleRecordPayment() {
        if (selectedBill == null) {
            return;
        }
        if (selectedBill.getStatus() == BillStatus.VOID) {
            warn("Void invoices cannot take payments.");
            return;
        }
        double amount;
        try {
            amount = Money.parse(text(payAmountField));
        } catch (NumberFormatException e) {
            warn("Enter a valid payment amount, e.g. 1500.");
            return;
        }
        if (amount <= 0) {
            warn("Payment must be greater than zero.");
            return;
        }
        if (amount - selectedBill.getBalance() > 0.009) {
            warn("That is more than the balance of " + selectedBill.getFormattedBalance() + ".");
            return;
        }

        Payment payment = BillingService.getInstance().recordPayment(selectedBill, amount,
                payMethodCombo.getValue(), text(payReferenceField));
        payReferenceField.clear();
        refreshBillsTable();
        billsTable.getSelectionModel().select(selectedBill);
        showBill(selectedBill);
        setStatus("Received " + payment.getFormattedAmount() + " for " + selectedBill.getBillId()
                + " — balance now " + selectedBill.getFormattedBalance() + ".", true);
    }

    @FXML
    private void handleViewReceipt() {
        if (selectedBill != null) {
            openReceipt(selectedBill);
        }
    }

    @FXML
    private void handleVoidBill() {
        if (selectedBill == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Void invoice");
        confirm.setHeaderText("Void bill " + selectedBill.getBillId() + "?");
        confirm.setContentText("A voided bill stays in the records but is excluded from reports and can no longer take payments.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            BillingService.getInstance().voidBill(selectedBill);
            refreshBillsTable();
            billsTable.getSelectionModel().select(selectedBill);
            showBill(selectedBill);
            setStatus("Bill " + selectedBill.getBillId() + " voided.", false);
        }
    }

    @FXML
    private void handleNewInvoiceFocus() {
        walkInGuestField.requestFocus();
    }

    private void openReceipt(Bill bill) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lodge/view/receipt_dialog.fxml"));
            Parent root = loader.load();
            ReceiptDialogController controller = loader.getController();
            controller.setBill(bill);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Receipt — " + bill.getBillId());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(billsTable.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/lodge/css/style.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            warn("Could not open the receipt: " + e.getMessage());
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void setStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll(success ? "success-text" : "error-text");
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Billing");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
