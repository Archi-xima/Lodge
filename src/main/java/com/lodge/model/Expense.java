package com.lodge.model;

import com.lodge.util.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable supplier bill. Gson restores records through this validating constructor. */
public record Expense(String expenseId, String supplier, String description, ExpenseCategory category,
                      String invoiceReference, LocalDate issuedOn, LocalDate dueOn,
                      double total, double tax, LocalDate paidOn, PaymentMethod paymentMethod,
                      String paymentReference, String createdBy, LocalDateTime createdAt, String paidBy) {
    public Expense {
        Objects.requireNonNull(expenseId, "Expense ID is required.");
        Objects.requireNonNull(category, "Expense category is required.");
        Objects.requireNonNull(issuedOn, "Expense invoice date is required.");
        Objects.requireNonNull(dueOn, "Expense due date is required.");
        Objects.requireNonNull(createdAt, "Expense creation date is required.");
        if (supplier == null || supplier.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Supplier and description are required.");
        }
        if (!Double.isFinite(total) || !Double.isFinite(tax)) {
            throw new IllegalArgumentException("Expense amounts must be finite.");
        }
        total = Money.round(total);
        tax = Money.round(tax);
        if (total <= 0 || tax < 0 || tax > total || dueOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("Expense dates or amounts are invalid.");
        }
        invoiceReference = Objects.toString(invoiceReference, "");
        paymentReference = Objects.toString(paymentReference, "");
        createdBy = Objects.toString(createdBy, "");
        paidBy = paidOn == null ? "" : Objects.toString(paidBy, createdBy);
        if (paidOn != null && (paymentMethod == null
                || (paymentMethod != PaymentMethod.CASH && paymentReference.isBlank()))) {
            throw new IllegalArgumentException("A paid supplier bill requires its payment details.");
        }
    }

    public Expense withPayment(LocalDate date, PaymentMethod method, String reference, String recordedBy) {
        return new Expense(expenseId, supplier, description, category, invoiceReference,
                issuedOn, dueOn, total, tax, date, method, reference, createdBy, createdAt, recordedBy);
    }

    public String getExpenseId() { return expenseId; }
    public String getSupplier() { return supplier; }
    public String getDescription() { return description; }
    public ExpenseCategory getCategory() { return category; }
    public String getInvoiceReference() { return invoiceReference; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public LocalDate getDueOn() { return dueOn; }
    public double getTotal() { return total; }
    public double getTax() { return tax; }
    public double getNet() { return Money.round(total - tax); }
    public boolean isPaid() { return paidOn != null; }
    public LocalDate getPaidOn() { return paidOn; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getPaidBy() { return paidBy; }
    public String getStatus() { return isPaid() ? "Paid" : "Unpaid"; }
}
