package com.lodge.controller;

import com.lodge.model.*;
import com.lodge.service.AccountingService;
import com.lodge.service.AccountingSnapshot;
import com.lodge.util.JsonAdapters;
import com.lodge.util.Money;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import static com.lodge.service.AccountingSnapshot.*;

/** Accountant workspace and the administrator's explicitly requested detailed view. */
public class AccountingController implements ViewController {
    @FXML private VBox accountingRoot;
    @FXML private Label viewDescription, accountingMessage, periodLabel, trendLabel;
    @FXML private Button detailToggle;
    @FXML private DatePicker fromPicker, toPicker;
    @FXML private Label invoicedValue, collectedValue, outstandingValue, expensesPaidValue;
    @FXML private Label cashValue, mobileValue, bankValue, cardValue;
    @FXML private VBox summaryPane;
    @FXML private TabPane detailTabs;
    @FXML private BarChart<String, Number> summaryTrend, detailTrend;
    @FXML private Label netValue, taxValue, discountsValue, expenseTaxValue, supplierValue, overdueValue, checksLabel;
    @FXML private TextField paymentSearch, outstandingSearch;
    @FXML private ComboBox<String> paymentFilter;
    @FXML private CheckBox overdueOnly;
    @FXML private TableView<PaymentRow> paymentsTable;
    @FXML private TableView<InvoiceRow> outstandingTable;
    @FXML private DatePicker invoiceDuePicker;
    @FXML private Button updateDueButton;
    @FXML private TableView<Expense> expensesTable;
    @FXML private TextField supplierField, expenseDescriptionField, supplierInvoiceField, expenseTotalField, expenseTaxField;
    @FXML private ComboBox<ExpenseCategory> expenseCategory;
    @FXML private DatePicker expenseIssuedPicker, expenseDuePicker, expensePaidPicker;
    @FXML private CheckBox expensePaidCheck;
    @FXML private VBox expensePaymentFields;
    @FXML private ComboBox<PaymentMethod> expenseMethod;
    @FXML private TextField expensePaymentRef;
    @FXML private DatePicker settleDatePicker;
    @FXML private ComboBox<PaymentMethod> settleMethod;
    @FXML private TextField settleReference;
    @FXML private Button settleExpenseButton;
    @FXML private TableView<ReportRow> reportTable;
    @FXML private TableView<ActivityRow> activityTable;
    @FXML private ComboBox<PaymentMethod> checkMethod;
    @FXML private TextField checkAmount;
    @FXML private Label checkResult;

    private final AccountingService service = AccountingService.getInstance();
    private AccountingSnapshot data;
    private boolean detailRequested;

    public record ReportRow(String measure, double amount) { }

    @FXML public void initialize() {
        service.requireAccess();
        fromPicker.setValue(LocalDate.now().withDayOfMonth(1));
        toPicker.setValue(LocalDate.now());
        detailToggle.setVisible(!service.isAccountant());
        detailToggle.setManaged(!service.isAccountant());
        setupTables();
        paymentFilter.getItems().add("All payment methods");
        for (PaymentMethod method : PaymentMethod.values()) paymentFilter.getItems().add(method.getLabel());
        paymentFilter.getSelectionModel().selectFirst();
        paymentFilter.valueProperty().addListener((o, a, b) -> filterPayments());
        paymentSearch.textProperty().addListener((o, a, b) -> filterPayments());
        outstandingSearch.textProperty().addListener((o, a, b) -> filterOutstanding());
        overdueOnly.selectedProperty().addListener((o, a, b) -> filterOutstanding());
        outstandingTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            updateDueButton.setDisable(b == null);
            invoiceDuePicker.setValue(b == null ? null : b.dueOn());
        });
        expensesTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) ->
                settleExpenseButton.setDisable(b == null || b.isPaid()));
        expenseCategory.getItems().setAll(ExpenseCategory.values());
        expenseCategory.setValue(ExpenseCategory.OTHER);
        expenseIssuedPicker.setValue(LocalDate.now());
        expenseDuePicker.setValue(LocalDate.now());
        expensePaidPicker.setValue(LocalDate.now());
        settleDatePicker.setValue(LocalDate.now());
        expenseMethod.getItems().setAll(PaymentMethod.values());
        settleMethod.getItems().setAll(PaymentMethod.values());
        expenseMethod.setValue(PaymentMethod.CASH);
        settleMethod.setValue(PaymentMethod.CASH);
        checkMethod.getItems().setAll(PaymentMethod.values());
        checkMethod.setValue(PaymentMethod.CASH);
        expensePaymentFields.visibleProperty().bind(expensePaidCheck.selectedProperty());
        expensePaymentFields.managedProperty().bind(expensePaidCheck.selectedProperty());
        updateDueButton.setDisable(true);
        settleExpenseButton.setDisable(true);
        summaryPane.setVisible(false);
        summaryPane.setManaged(false);
        detailTabs.setVisible(false);
        detailTabs.setManaged(false);
    }

    @Override public void onShown() { refresh(); }

    private void setupTables() {
        column(paymentsTable, "Received", PaymentRow::receivedAt, 155, false);
        column(paymentsTable, "Payment", PaymentRow::paymentId, 95, false);
        column(paymentsTable, "Invoice", PaymentRow::billId, 85, false);
        column(paymentsTable, "Guest", PaymentRow::guest, 160, false);
        column(paymentsTable, "Method", PaymentRow::method, 125, false);
        column(paymentsTable, "Amount", PaymentRow::amount, 115, true);
        column(paymentsTable, "Reference", PaymentRow::reference, 155, false);
        column(paymentsTable, "Received by", PaymentRow::receivedBy, 140, false);
        column(paymentsTable, "Invoice check", p -> p.voidedInvoice() ? "Voided invoice" : "", 130, false);
        column(outstandingTable, "Invoice", InvoiceRow::billId, 90, false);
        column(outstandingTable, "Guest", InvoiceRow::guest, 170, false);
        column(outstandingTable, "Issued", InvoiceRow::issuedOn, 110, false);
        column(outstandingTable, "Due", InvoiceRow::dueOn, 110, false);
        column(outstandingTable, "Total", InvoiceRow::total, 110, true);
        column(outstandingTable, "Paid", InvoiceRow::paid, 110, true);
        column(outstandingTable, "Balance", InvoiceRow::balance, 120, true);
        column(outstandingTable, "Ageing", InvoiceRow::ageing, 115, false);
        column(expensesTable, "Expense", Expense::getExpenseId, 85, false);
        column(expensesTable, "Supplier", Expense::getSupplier, 150, false);
        column(expensesTable, "Description", Expense::getDescription, 180, false);
        column(expensesTable, "Category", Expense::getCategory, 140, false);
        column(expensesTable, "Supplier invoice", Expense::getInvoiceReference, 135, false);
        column(expensesTable, "Issued", Expense::getIssuedOn, 110, false);
        column(expensesTable, "Due", Expense::getDueOn, 110, false);
        column(expensesTable, "Total", Expense::getTotal, 110, true);
        column(expensesTable, "VAT", Expense::getTax, 95, true);
        column(expensesTable, "Status", Expense::getStatus, 85, false);
        column(expensesTable, "Paid on", Expense::getPaidOn, 110, false);
        column(expensesTable, "Method", Expense::getPaymentMethod, 120, false);
        column(expensesTable, "Payment reference", Expense::getPaymentReference, 150, false);
        column(expensesTable, "Payment recorded by", Expense::getPaidBy, 155, false);
        column(reportTable, "Measure", ReportRow::measure, 440, false);
        column(reportTable, "Amount (K)", ReportRow::amount, 175, true);
        column(activityTable, "When", ActivityRow::timestamp, 155, false);
        column(activityTable, "User", ActivityRow::user, 130, false);
        column(activityTable, "Action", ActivityRow::action, 175, false);
        column(activityTable, "Details", ActivityRow::detail, 490, false);
        paymentsTable.setPlaceholder(new Label("No payments match these dates and filters."));
        outstandingTable.setPlaceholder(new Label("No outstanding invoices match this filter."));
        expensesTable.setPlaceholder(new Label("No supplier expenses recorded for these dates."));
        activityTable.setPlaceholder(new Label("No financial activity for these dates."));
    }

    private <T> void column(TableView<T> table, String title, Function<T, ?> value, double width, boolean money) {
        TableColumn<T, Object> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setMinWidth(Math.min(width, 85));
        column.setCellValueFactory(c -> new SimpleObjectProperty<>(value.apply(c.getValue())));
        if (money) column.getStyleClass().add("accounting-number");
        column.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : money && item instanceof Number number
                        ? Money.format(number.doubleValue()) : item instanceof LocalDateTime date
                        ? date.format(JsonAdapters.DATE_TIME) : item.toString());
            }
        });
        table.getColumns().add(column);
    }

    @FXML private void refresh() {
        try {
            data = service.snapshot(fromPicker.getValue(), toPicker.getValue(), detailRequested);
            checkResult.setText("");
            var s = data.summary();
            invoicedValue.setText(Money.format(s.invoiced()));
            collectedValue.setText(Money.format(s.collected()));
            outstandingValue.setText(Money.format(s.outstanding()));
            expensesPaidValue.setText(Money.format(s.expensesPaid()));
            cashValue.setText(Money.format(data.receiptsByMethod().get(PaymentMethod.CASH)));
            mobileValue.setText(Money.format(data.receiptsByMethod().get(PaymentMethod.MOBILE_MONEY)));
            bankValue.setText(Money.format(data.receiptsByMethod().get(PaymentMethod.BANK_TRANSFER)));
            cardValue.setText(Money.format(data.receiptsByMethod().get(PaymentMethod.CARD)));
            periodLabel.setText("Selected period: " + data.from() + " to " + data.to()
                    + "  •  Outstanding balances are current.");
            boolean detailed = data.detailed();
            summaryPane.setVisible(!detailed);
            summaryPane.setManaged(!detailed);
            detailTabs.setVisible(detailed);
            detailTabs.setManaged(detailed);
            detailToggle.setText(detailed ? "Back to summary" : "Open detailed view");
            viewDescription.setText(service.isAccountant() ? "Your detailed financial workspace"
                    : detailed ? "Detailed financial workspace · Administrator" : "Financial summary · Administrator");
            trendLabel.setText("Six calendar months ending " + data.to().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            populateTrend(detailed ? detailTrend : summaryTrend);
            if (detailed) {
                netValue.setText(Money.format(s.netInvoiced()));
                taxValue.setText(Money.format(s.invoiceTax()));
                discountsValue.setText(Money.format(s.discounts()));
                expenseTaxValue.setText(Money.format(s.expenseTax()));
                supplierValue.setText(Money.format(s.suppliersOutstanding()));
                overdueValue.setText(Money.format(s.overdue()));
                checksLabel.setText("Customer credit balances: " + Money.format(s.creditBalances())
                        + "  •  Payments on voided invoices in this period: " + s.voidedInvoicePayments()
                        + ". Review these before closing the period.");
                filterPayments();
                filterOutstanding();
                expensesTable.getItems().setAll(data.expenses());
                activityTable.getItems().setAll(data.activity());
                reportTable.getItems().setAll(
                        new ReportRow("Invoices issued, excluding voids", s.invoiced()),
                        new ReportRow("Net invoiced, after discounts", s.netInvoiced()),
                        new ReportRow("VAT on invoices issued", s.invoiceTax()),
                        new ReportRow("Long-stay discounts", s.discounts()),
                        new ReportRow("Payments collected, by receipt date", s.collected()),
                        new ReportRow("Supplier expenses, net, by invoice date", s.expensesNet()),
                        new ReportRow("VAT entered on supplier expenses", s.expenseTax()),
                        new ReportRow("Supplier payments, by paid date", s.expensesPaid()),
                        new ReportRow("Receipts less supplier payments", Money.round(s.collected() - s.expensesPaid())),
                        new ReportRow("Customer balances outstanding now", s.outstanding()),
                        new ReportRow("Customer balances overdue now", s.overdue()),
                        new ReportRow("Supplier bills unpaid now", s.suppliersOutstanding()));
            } else {
                for (TableView<?> table : List.of(paymentsTable, outstandingTable, expensesTable, reportTable, activityTable)) {
                    table.getItems().clear();
                }
                detailTrend.getData().clear();
            }
            message("Figures updated.", true);
        } catch (SecurityException error) {
            data = null;
            accountingRoot.getChildren().setAll(new Label(error.getMessage()));
        } catch (RuntimeException error) { message(error.getMessage(), false); }
    }

    private void populateTrend(BarChart<String, Number> chart) {
        chart.getData().clear();
        XYChart.Series<String, Number> invoices = new XYChart.Series<>();
        XYChart.Series<String, Number> receipts = new XYChart.Series<>();
        invoices.setName("Invoiced"); receipts.setName("Collected");
        for (var month : data.trend()) {
            String label = month.month().format(DateTimeFormatter.ofPattern("MMM yy"));
            invoices.getData().add(new XYChart.Data<>(label, month.invoiced()));
            receipts.getData().add(new XYChart.Data<>(label, month.collected()));
        }
        chart.getData().addAll(List.of(invoices, receipts));
    }

    private void filterPayments() {
        if (data == null || !data.detailed()) return;
        String needle = clean(paymentSearch.getText()).toLowerCase(java.util.Locale.ROOT);
        String method = paymentFilter.getValue();
        paymentsTable.getItems().setAll(data.payments().stream().filter(p ->
                (method == null || method.equals("All payment methods") || (p.method() != null && p.method().getLabel().equals(method)))
                        && (clean(p.billId()) + " " + clean(p.guest()) + " " + clean(p.reference()))
                        .toLowerCase(java.util.Locale.ROOT).contains(needle)).toList());
    }

    private void filterOutstanding() {
        if (data == null || !data.detailed()) return;
        String needle = clean(outstandingSearch.getText()).toLowerCase(java.util.Locale.ROOT);
        outstandingTable.getItems().setAll(data.outstanding().stream()
                .filter(i -> (!overdueOnly.isSelected() || i.daysOverdue() > 0)
                        && (clean(i.billId()) + " " + clean(i.guest())).toLowerCase(java.util.Locale.ROOT).contains(needle)).toList());
    }

    @FXML private void toggleDetail() {
        if (service.isAccountant()) return;
        if (!detailRequested) service.requestDetailedView();
        detailRequested = !detailRequested;
        refresh();
    }

    @FXML private void thisMonth() {
        fromPicker.setValue(LocalDate.now().withDayOfMonth(1));
        toPicker.setValue(LocalDate.now());
        refresh();
    }

    @FXML private void updateDueDate() {
        var row = outstandingTable.getSelectionModel().getSelectedItem();
        if (row == null) return;
        try {
            service.setInvoiceDueDate(row.billId(), invoiceDuePicker.getValue());
            refresh(); message("Due date updated for " + row.billId() + ".", true);
        } catch (RuntimeException error) { message(error.getMessage(), false); }
    }

    @FXML private void recordExpense() {
        try {
            if (expensePaidCheck.isSelected() && expensePaidPicker.getValue() == null) {
                throw new IllegalArgumentException("Choose the date the supplier was paid.");
            }
            Expense expense = service.recordExpense(supplierField.getText(), expenseDescriptionField.getText(),
                    expenseCategory.getValue(), supplierInvoiceField.getText(), expenseIssuedPicker.getValue(),
                    expenseDuePicker.getValue(), Money.parse(expenseTotalField.getText()),
                    clean(expenseTaxField.getText()).isEmpty() ? 0 : Money.parse(expenseTaxField.getText()),
                    expensePaidCheck.isSelected() ? expensePaidPicker.getValue() : null,
                    expenseMethod.getValue(), expensePaymentRef.getText());
            supplierField.clear(); expenseDescriptionField.clear(); supplierInvoiceField.clear();
            expenseTotalField.clear(); expenseTaxField.setText("0"); expensePaymentRef.clear();
            expensePaidCheck.setSelected(false);
            refresh();
            message("Supplier expense " + expense.getExpenseId() + " saved. Use its invoice dates to find it in the register.", true);
        } catch (RuntimeException error) { message(error.getMessage(), false); }
    }

    @FXML private void settleExpense() {
        Expense expense = expensesTable.getSelectionModel().getSelectedItem();
        if (expense == null) return;
        try {
            service.payExpense(expense.getExpenseId(), settleDatePicker.getValue(), settleMethod.getValue(), settleReference.getText());
            settleReference.clear(); refresh(); message("Supplier payment saved for " + expense.getExpenseId() + ".", true);
        } catch (RuntimeException error) { message(error.getMessage(), false); }
    }

    @FXML private void exportSummary() { export(AccountingService.Export.SUMMARY); }
    @FXML private void exportPayments() { export(AccountingService.Export.PAYMENTS); }
    @FXML private void exportInvoices() { export(AccountingService.Export.INVOICES); }
    @FXML private void exportOutstanding() { export(AccountingService.Export.OUTSTANDING); }
    @FXML private void exportExpenses() { export(AccountingService.Export.EXPENSES); }

    @FXML private void compareReceipts() {
        try {
            requireAppliedDates();
            AccountingSnapshot fresh = service.snapshot(data.from(), data.to(), detailRequested);
            double actual = Money.parse(checkAmount.getText());
            if (actual < 0 || checkMethod.getValue() == null) {
                throw new IllegalArgumentException("Choose a payment method and enter a non-negative receipt total.");
            }
            double expected = fresh.receiptsByMethod().get(checkMethod.getValue());
            checkResult.setText("Recorded: " + Money.format(expected) + "  •  Counted / statement: "
                    + Money.format(actual) + "  •  Difference: " + Money.format(Money.round(actual - expected)));
        } catch (RuntimeException error) { message(error.getMessage(), false); }
    }

    private void export(AccountingService.Export type) {
        try {
            requireAppliedDates();
            String csv = service.exportCsv(type, data.from(), data.to(), detailRequested);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export " + type.toString().toLowerCase(java.util.Locale.ROOT));
            chooser.setInitialFileName("accounting-" + type.toString().toLowerCase(java.util.Locale.ROOT) + "-" + data.to() + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV spreadsheet", "*.csv"));
            java.io.File file = chooser.showSaveDialog(accountingRoot.getScene().getWindow());
            if (file == null) return;
            service.requireAccess();
            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
            message("Export saved to " + file.getName() + ".", true);
        } catch (Exception error) { message("Export could not be saved: " + error.getMessage(), false); }
    }

    private static String clean(String text) { return text == null ? "" : text.trim(); }
    private void requireAppliedDates() {
        service.requireAccess();
        if (data == null || !java.util.Objects.equals(fromPicker.getValue(), data.from())
                || !java.util.Objects.equals(toPicker.getValue(), data.to())) {
            throw new IllegalArgumentException("Apply the selected dates before exporting or comparing figures.");
        }
    }
    private void message(String text, boolean success) {
        accountingMessage.setText(text == null ? "The operation could not be completed." : text);
        accountingMessage.getStyleClass().setAll(success ? "success-text" : "error-text");
    }
}
