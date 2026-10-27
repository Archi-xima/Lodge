package com.lodge.service;

import com.lodge.model.*;
import com.lodge.util.Money;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.lodge.service.AccountingSnapshot.*;

/** Pure calculations shared by the guarded accounting service and its tests. */
final class AccountingReports {
    private AccountingReports() { }

    static AccountingSnapshot build(List<Bill> bills, List<Expense> expenses, List<AuditEntry> audit,
                                    LocalDate from, LocalDate to, LocalDate today, boolean detailed) {
        double invoiced = 0, net = 0, tax = 0, discounts = 0, collected = 0;
        double outstanding = 0, overdue = 0, credits = 0, expensesNet = 0, expenseTax = 0;
        double expensesPaid = 0, suppliersOutstanding = 0;
        int voidedPayments = 0;
        List<PaymentRow> paymentRows = new ArrayList<>();
        List<InvoiceRow> invoiceRows = new ArrayList<>();
        List<InvoiceRow> outstandingRows = new ArrayList<>();
        EnumMap<PaymentMethod, Double> methods = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) methods.put(method, 0.0);
        YearMonth lastMonth = YearMonth.from(to);
        Map<YearMonth, double[]> months = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) months.put(lastMonth.minusMonths(i), new double[2]);

        for (Bill bill : bills) {
            boolean voided = bill.getStatus() == BillStatus.VOID;
            LocalDate issued = bill.getCreatedAt() == null ? null : bill.getCreatedAt().toLocalDate();
            InvoiceRow row = invoice(bill, today);
            if (between(issued, from, to)) {
                if (detailed) invoiceRows.add(row);
                if (!voided) {
                    invoiced += bill.getTotal();
                    net += bill.getSubtotal();
                    tax += bill.getTaxAmount();
                    discounts += bill.getLongStayDiscount();
                }
            }
            if (!voided && issued != null && months.containsKey(YearMonth.from(issued))) {
                months.get(YearMonth.from(issued))[0] += bill.getTotal();
            }
            if (!voided && bill.getBalance() > 0.009) {
                outstanding += bill.getBalance();
                if (row.daysOverdue() > 0) overdue += bill.getBalance();
                if (detailed) outstandingRows.add(row);
            } else if (!voided && bill.getBalance() < -0.009) {
                credits -= bill.getBalance();
            }
            for (Payment payment : bill.getPayments()) {
                if (payment.getTimestamp() == null) continue;
                LocalDate received = payment.getTimestamp().toLocalDate();
                if (months.containsKey(YearMonth.from(received))) {
                    months.get(YearMonth.from(received))[1] += payment.getAmount();
                }
                if (!between(received, from, to)) continue;
                // Voiding an invoice does not reverse money already received.
                collected += payment.getAmount();
                if (payment.getMethod() != null) methods.merge(payment.getMethod(), payment.getAmount(), Double::sum);
                if (voided) voidedPayments++;
                if (detailed) paymentRows.add(new PaymentRow(payment.getPaymentId(), bill.getBillId(),
                        bill.getGuestName(), payment.getTimestamp(), payment.getMethod(), payment.getAmount(),
                        payment.getReference(), payment.getReceivedBy(), voided));
            }
        }
        List<Expense> expenseRows = new ArrayList<>();
        for (Expense expense : expenses) {
            if (between(expense.getIssuedOn(), from, to)) {
                expensesNet += expense.getNet();
                expenseTax += expense.getTax();
                if (detailed) expenseRows.add(expense);
            }
            if (between(expense.getPaidOn(), from, to)) expensesPaid += expense.getTotal();
            if (!expense.isPaid()) suppliersOutstanding += expense.getTotal();
        }
        paymentRows.sort(Comparator.comparing(PaymentRow::receivedAt).reversed());
        outstandingRows.sort(Comparator.comparingLong(InvoiceRow::daysOverdue).reversed()
                .thenComparing(InvoiceRow::billId));
        invoiceRows.sort(Comparator.comparing(InvoiceRow::issuedOn).reversed());
        expenseRows.sort(Comparator.comparing(Expense::getIssuedOn).reversed());
        methods.replaceAll((method, value) -> Money.round(value));
        List<MonthlyTotal> trend = months.entrySet().stream().map(entry -> new MonthlyTotal(
                entry.getKey(), Money.round(entry.getValue()[0]), Money.round(entry.getValue()[1]))).toList();
        Set<String> financeActions = Set.of("PAYMENT", "ADD BILL", "VOID BILL", "DELETE BILL", "ADD EXTRA",
                "ACCOUNTING EXPENSE", "ACCOUNTING PAYMENT", "INVOICE DUE DATE", "ACCOUNTING DETAIL");
        List<ActivityRow> activities = detailed ? audit.stream()
                .filter(a -> a.getTimestamp() != null && between(a.getTimestamp().toLocalDate(), from, to)
                        && financeActions.contains(a.getAction()))
                .sorted(Comparator.comparing(AuditEntry::getTimestamp).reversed()).limit(100)
                .map(a -> new ActivityRow(a.getTimestamp(), a.getUsername(), a.getAction(), a.getDetail())).toList()
                : List.of();
        Summary summary = new Summary(Money.round(invoiced), Money.round(net), Money.round(tax),
                Money.round(discounts), Money.round(collected), Money.round(outstanding), Money.round(overdue),
                Money.round(expensesNet), Money.round(expenseTax), Money.round(expensesPaid),
                Money.round(suppliersOutstanding), Money.round(credits), voidedPayments);
        return new AccountingSnapshot(detailed, from, to, summary, Map.copyOf(methods), trend,
                List.copyOf(paymentRows), List.copyOf(invoiceRows), List.copyOf(outstandingRows),
                List.copyOf(expenseRows), activities);
    }

    static boolean between(LocalDate date, LocalDate from, LocalDate to) {
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }

    private static InvoiceRow invoice(Bill bill, LocalDate today) {
        LocalDate due = bill.getDueDate();
        long days = due == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(due, today));
        String ageing = days == 0 ? "Current" : days <= 30 ? "1–30 days"
                : days <= 60 ? "31–60 days" : days <= 90 ? "61–90 days" : "90+ days";
        return new InvoiceRow(bill.getBillId(), bill.getGuestName(),
                bill.getCreatedAt() == null ? null : bill.getCreatedAt().toLocalDate(), due,
                bill.getSubtotal(), bill.getTaxAmount(), bill.getTotal(), bill.getAmountPaid(),
                bill.getBalance(), bill.getStatusLabel(), days, ageing);
    }
}
