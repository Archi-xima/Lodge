package com.lodge.service;

import com.lodge.model.Expense;
import com.lodge.model.PaymentMethod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Immutable accounting data; summary mode contains no transaction or customer rows. */
public record AccountingSnapshot(
        boolean detailed, LocalDate from, LocalDate to, Summary summary,
        Map<PaymentMethod, Double> receiptsByMethod, List<MonthlyTotal> trend,
        List<PaymentRow> payments, List<InvoiceRow> invoices, List<InvoiceRow> outstanding,
        List<Expense> expenses, List<ActivityRow> activity) {

    public record Summary(double invoiced, double netInvoiced, double invoiceTax,
                          double discounts, double collected, double outstanding,
                          double overdue, double expensesNet, double expenseTax,
                          double expensesPaid, double suppliersOutstanding,
                          double creditBalances, int voidedInvoicePayments) { }

    public record PaymentRow(String paymentId, String billId, String guest,
                             LocalDateTime receivedAt, PaymentMethod method, double amount,
                             String reference, String receivedBy, boolean voidedInvoice) { }

    public record InvoiceRow(String billId, String guest, LocalDate issuedOn, LocalDate dueOn,
                             double net, double tax, double total, double paid, double balance,
                             String status, long daysOverdue, String ageing) { }

    public record MonthlyTotal(YearMonth month, double invoiced, double collected) { }
    public record ActivityRow(LocalDateTime timestamp, String user, String action, String detail) { }
}
