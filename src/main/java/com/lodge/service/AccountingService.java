package com.lodge.service;

import com.lodge.model.*;
import com.lodge.util.Csv;
import com.lodge.util.Money;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Access-controlled finance workspace. Every read, export and mutation rechecks the session. */
public final class AccountingService {
    private static AccountingService instance;
    private final AuthService auth;
    private final DataStore store;
    private final Supplier<List<Bill>> bills;
    private final Clock clock;
    private List<Expense> expenses;

    private AccountingService() {
        this(AuthService.getInstance(), DataStore.getInstance(),
                () -> new ArrayList<>(BillingService.getInstance().getBills()), Clock.systemDefaultZone());
    }

    AccountingService(AuthService auth, DataStore store, Supplier<List<Bill>> bills, Clock clock) {
        this.auth = auth;
        this.store = store;
        this.bills = bills;
        this.clock = clock;
    }

    public static synchronized AccountingService getInstance() {
        if (instance == null) instance = new AccountingService();
        return instance;
    }

    public void requireAccess() {
        if (!auth.hasPermission(Role.Permission.VIEW_ACCOUNTING)) {
            throw new SecurityException("Accounting is available to accountants and administrators only.");
        }
    }

    public boolean isAccountant() {
        requireAccess();
        return auth.getCurrentUser().getRole() == Role.ACCOUNTANT;
    }

    public synchronized AccountingSnapshot snapshot(LocalDate from, LocalDate to, boolean detailRequested) {
        requireAccess();
        validateRange(from, to);
        boolean detailed = isAccountant() || detailRequested;
        return AccountingReports.build(bills.get(), expenses(), new ArrayList<>(auth.getAuditTrail()),
                from, to, LocalDate.now(clock), detailed);
    }

    public void requestDetailedView() {
        requireAccess();
        if (!isAccountant()) auth.log("ACCOUNTING DETAIL", "Administrator opened the detailed accounting view");
    }

    private List<Expense> expenses() {
        if (expenses == null) expenses = new ArrayList<>(store.loadExpenses());
        return expenses;
    }

    public synchronized Expense recordExpense(String supplier, String description, ExpenseCategory category,
            String invoiceReference, LocalDate issuedOn, LocalDate dueOn, double total, double tax,
            LocalDate paidOn, PaymentMethod method, String paymentReference) {
        requireAccess();
        supplier = required(supplier, "Enter the supplier name.");
        description = required(description, "Enter an expense description.");
        if (category == null || issuedOn == null || dueOn == null) {
            throw new IllegalArgumentException("Choose a category, invoice date and due date.");
        }
        if (dueOn.isBefore(issuedOn)) throw new IllegalArgumentException("The due date cannot be before the invoice date.");
        if (!Double.isFinite(total) || !Double.isFinite(tax)) {
            throw new IllegalArgumentException("Enter finite amounts for the total and VAT.");
        }
        total = Money.round(total);
        tax = Money.round(tax);
        if (total <= 0 || tax < 0 || tax > total) {
            throw new IllegalArgumentException("Enter a positive total and VAT between zero and the total.");
        }
        if (paidOn != null) validatePayment(paidOn, method, paymentReference);
        List<String> ids = expenses().stream().map(Expense::getExpenseId).toList();
        Expense expense = new Expense(store.nextId("E", ids), supplier, description, category,
                clean(invoiceReference), issuedOn, dueOn, total, tax, paidOn, paidOn == null ? null : method,
                paidOn == null ? "" : clean(paymentReference), auth.currentUserName(), LocalDateTime.now(clock),
                paidOn == null ? "" : auth.currentUserName());
        List<Expense> updated = new ArrayList<>(expenses());
        updated.add(expense);
        store.saveExpensesStrict(updated);
        expenses = updated;
        auth.log("ACCOUNTING EXPENSE", expense.getExpenseId() + " — " + supplier + " — " + Money.format(total));
        return expense;
    }

    public synchronized void payExpense(String expenseId, LocalDate paidOn, PaymentMethod method, String reference) {
        requireAccess();
        validatePayment(paidOn, method, reference);
        List<Expense> updated = new ArrayList<>(expenses());
        for (int i = 0; i < updated.size(); i++) {
            Expense expense = updated.get(i);
            if (!expense.getExpenseId().equals(expenseId)) continue;
            if (expense.isPaid()) throw new IllegalArgumentException("This supplier bill is already paid.");
            updated.set(i, expense.withPayment(paidOn, method, clean(reference), auth.currentUserName()));
            store.saveExpensesStrict(updated);
            expenses = updated;
            auth.log("ACCOUNTING PAYMENT", expenseId + " — " + Money.format(expense.getTotal()) + " via " + method);
            return;
        }
        throw new IllegalArgumentException("Select a supplier bill to pay.");
    }

    public synchronized void setInvoiceDueDate(String billId, LocalDate dueOn) {
        requireAccess();
        if (dueOn == null) throw new IllegalArgumentException("Choose a due date.");
        List<Bill> current = bills.get();
        Bill bill = current.stream().filter(b -> Objects.equals(b.getBillId(), billId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an invoice."));
        if (bill.getStatus() == BillStatus.VOID) throw new IllegalArgumentException("A void invoice cannot be changed.");
        LocalDate previous = bill.getDueDate();
        bill.setDueDate(dueOn);
        try { store.saveBillsStrict(current); }
        catch (RuntimeException error) { bill.setDueDate(previous); throw error; }
        auth.log("INVOICE DUE DATE", billId + " — due date " + previous + " → " + dueOn);
    }

    public enum Export { SUMMARY, PAYMENTS, INVOICES, OUTSTANDING, EXPENSES }

    public String exportCsv(Export type, LocalDate from, LocalDate to, boolean detailRequested) {
        AccountingSnapshot data = snapshot(from, to, detailRequested);
        if (!data.detailed() && type != Export.SUMMARY) {
            throw new SecurityException("Open the detailed accounting view before exporting transactions.");
        }
        StringBuilder csv = new StringBuilder("\ufeff");
        switch (type) {
            case SUMMARY -> {
                csv.append(Csv.row("Measure", "Amount (K)", "From", "To"));
                var s = data.summary();
                csv.append(Csv.row("Invoices excluding voids", s.invoiced(), from, to));
                csv.append(Csv.row("Net invoiced", s.netInvoiced(), from, to));
                csv.append(Csv.row("VAT on invoices", s.invoiceTax(), from, to));
                csv.append(Csv.row("Collections by payment date", s.collected(), from, to));
                csv.append(Csv.row("Supplier expenses, net, by invoice date", s.expensesNet(), from, to));
                csv.append(Csv.row("VAT recorded on supplier expenses", s.expenseTax(), from, to));
                csv.append(Csv.row("Supplier payments by paid date", s.expensesPaid(), from, to));
                csv.append(Csv.row("Outstanding customer balances now", s.outstanding(), "", ""));
                csv.append(Csv.row("Overdue customer balances now", s.overdue(), "", ""));
                csv.append(Csv.row("Supplier bills unpaid now", s.suppliersOutstanding(), "", ""));
                for (PaymentMethod method : PaymentMethod.values()) {
                    csv.append(Csv.row("Collections: " + method, data.receiptsByMethod().get(method), from, to));
                }
            }
            case PAYMENTS -> {
                csv.append(Csv.row("Payment", "Invoice", "Guest", "Received at", "Method", "Amount (K)", "Reference", "Received by", "Voided invoice"));
                for (var p : data.payments()) csv.append(Csv.row(p.paymentId(), p.billId(), p.guest(), p.receivedAt(),
                        p.method(), p.amount(), p.reference(), p.receivedBy(), p.voidedInvoice()));
            }
            case INVOICES, OUTSTANDING -> {
                csv.append(Csv.row("Invoice", "Guest", "Issued", "Due", "Net (K)", "VAT (K)", "Total (K)", "Paid to date (K)",
                        "Balance now (K)", "Status", "Days overdue now", "Ageing now"));
                for (var i : type == Export.INVOICES ? data.invoices() : data.outstanding()) {
                    csv.append(Csv.row(i.billId(), i.guest(), i.issuedOn(), i.dueOn(), i.net(), i.tax(), i.total(),
                            i.paid(), i.balance(), i.status(), i.daysOverdue(), i.ageing()));
                }
            }
            case EXPENSES -> {
                csv.append(Csv.row("Expense", "Supplier", "Description", "Category", "Supplier invoice", "Issued", "Due",
                        "Net (K)", "VAT (K)", "Total (K)", "Status", "Paid on", "Method", "Payment reference", "Recorded by", "Payment recorded by"));
                for (Expense e : data.expenses()) csv.append(Csv.row(e.getExpenseId(), e.getSupplier(), e.getDescription(),
                        e.getCategory(), e.getInvoiceReference(), e.getIssuedOn(), e.getDueOn(), e.getNet(), e.getTax(),
                        e.getTotal(), e.getStatus(), e.getPaidOn(), e.getPaymentMethod(), e.getPaymentReference(), e.getCreatedBy(), e.getPaidBy()));
            }
        }
        return csv.toString();
    }

    private void validatePayment(LocalDate date, PaymentMethod method, String reference) {
        if (date == null || method == null) throw new IllegalArgumentException("Choose the payment date and method.");
        if (date.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException("A payment cannot be dated in the future.");
        if (method != PaymentMethod.CASH && clean(reference).isEmpty()) {
            throw new IllegalArgumentException("Enter the bank, mobile money or card payment reference.");
        }
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Choose a valid date range; the end must be on or after the start.");
        }
    }

    private static String clean(String text) { return text == null ? "" : text.trim(); }
    private static String required(String text, String error) {
        if (clean(text).isEmpty()) throw new IllegalArgumentException(error);
        return clean(text);
    }
}
