package com.lodge.service;

import com.lodge.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountingServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-25T08:00:00Z"), ZoneId.of("Africa/Lusaka"));
    @TempDir Path directory;
    private DataStore store;
    private AuthService auth;
    private AccountingService service;
    private final List<Bill> bills = new ArrayList<>();

    @BeforeEach void setUp() {
        store = new DataStore(directory);
        auth = new AuthService(store);
        assertTrue(auth.createInitialAdministrator("owner", "Owner", "Owner-Password"));
        assertTrue(auth.login("owner", "Owner-Password"));
        for (Role role : List.of(Role.ACCOUNTANT, Role.MANAGER, Role.RECEPTIONIST)) {
            assertTrue(auth.register(role.name(), "Staff-Password", role.getLabel(), role));
        }
        assertTrue(auth.registerCustomer("guest", "Guest-Password", "Guest", "guest@example.com", ""));
        service = new AccountingService(auth, store, () -> new ArrayList<>(bills), CLOCK);
    }

    private void login(Role role) {
        auth.logout();
        assertTrue(auth.login(role == Role.ADMIN ? "owner" : role == Role.CUSTOMER ? "guest" : role.name(),
                role == Role.ADMIN ? "Owner-Password" : role == Role.CUSTOMER ? "Guest-Password" : "Staff-Password"));
    }

    private AccountingSnapshot snapshot(boolean detail) {
        return service.snapshot(TODAY.withDayOfMonth(1), TODAY, detail);
    }

    private Bill bill(String id, LocalDate issued) {
        Bill bill = Bill.walkIn(id, "Private Guest", "", new Room("102", RoomType.STANDARD, 1),
                1, 0, "", "Reception");
        bill.setCreatedAt(issued.atStartOfDay());
        bills.add(bill);
        return bill;
    }

    private void payment(Bill bill, double amount, LocalDate received, PaymentMethod method) {
        Payment payment = new Payment("P" + bill.getPayments().size(), bill.getBillId(), amount, method, "REF-123", "Cashier");
        payment.setTimestamp(received.atTime(12, 30));
        bill.addPayment(payment);
    }

    private Expense expense() {
        return service.recordExpense("Supplier", "Cleaning supplies", ExpenseCategory.FOOD_AND_SUPPLIES,
                "SUP-001", TODAY, TODAY.plusDays(7), 580, 80, null, null, "");
    }

    @Test void onlyAccountantsAndAdministratorsCanReadOrChangeAccounting() {
        for (Role role : List.of(Role.MANAGER, Role.RECEPTIONIST, Role.CUSTOMER)) {
            login(role);
            assertThrows(SecurityException.class, () -> snapshot(true), role.name());
            assertThrows(SecurityException.class, () -> service.exportCsv(AccountingService.Export.SUMMARY, TODAY, TODAY, true));
            assertThrows(SecurityException.class, this::expense);
            assertThrows(SecurityException.class, () -> service.payExpense("E001", TODAY, PaymentMethod.CASH, ""));
            assertThrows(SecurityException.class, () -> service.setInvoiceDueDate("B001", TODAY));
            assertFalse(auth.hasPermission(Role.Permission.VIEW_REPORTS));
        }
        auth.logout();
        assertThrows(SecurityException.class, () -> snapshot(false));
    }

    @Test void administratorDefaultsToSummaryWithoutSensitiveRows() {
        Bill bill = bill("B001", TODAY);
        payment(bill, 100, TODAY, PaymentMethod.CASH);
        expense();
        AccountingSnapshot summary = snapshot(false);
        assertFalse(summary.detailed());
        assertEquals(100, summary.summary().collected());
        assertTrue(summary.payments().isEmpty());
        assertTrue(summary.invoices().isEmpty());
        assertTrue(summary.outstanding().isEmpty());
        assertTrue(summary.expenses().isEmpty());
        assertTrue(summary.activity().isEmpty());
        assertFalse(service.exportCsv(AccountingService.Export.SUMMARY, TODAY, TODAY, false).contains("Private Guest"));
        assertThrows(SecurityException.class, () -> service.exportCsv(AccountingService.Export.PAYMENTS, TODAY, TODAY, false));
        service.requestDetailedView();
        assertTrue(snapshot(true).detailed());
        assertEquals(1, snapshot(true).payments().size());
        assertTrue(auth.getAuditTrail().stream().anyMatch(a -> a.getAction().equals("ACCOUNTING DETAIL")));
    }

    @Test void accountantAlwaysGetsDetailedDataEvenWithoutRequestFlag() {
        bill("B001", TODAY);
        login(Role.ACCOUNTANT);
        assertTrue(snapshot(false).detailed());
        assertEquals(1, snapshot(false).outstanding().size());
        assertTrue(service.exportCsv(AccountingService.Export.INVOICES, TODAY, TODAY, false).contains("Private Guest"));
    }

    @Test void receiptsUsePaymentDatesAndIncludeOldInvoices() {
        Bill old = bill("B001", TODAY.minusMonths(1));
        payment(old, 100, TODAY, PaymentMethod.BANK_TRANSFER);
        AccountingSnapshot data = snapshot(true);
        assertEquals(0, data.summary().invoiced());
        assertEquals(100, data.summary().collected());
        assertEquals(100, data.receiptsByMethod().get(PaymentMethod.BANK_TRANSFER));
        assertEquals(306, data.summary().outstanding());
        assertEquals(1, data.payments().size());
        assertTrue(data.invoices().isEmpty());
        assertEquals(1, data.outstanding().size());
        assertEquals(100, data.trend().get(5).collected());
        assertEquals(406, data.trend().get(4).invoiced());
    }

    @Test void dateRangeIncludesBothEndsAndExcludesOtherPaymentDates() {
        Bill bill = bill("B001", TODAY);
        LocalDate from = TODAY.minusDays(1);
        payment(bill, 10, from.minusDays(1), PaymentMethod.CASH);
        payment(bill, 20, from, PaymentMethod.MOBILE_MONEY);
        payment(bill, 30, TODAY, PaymentMethod.BANK_TRANSFER);
        payment(bill, 40, TODAY.plusDays(1), PaymentMethod.CARD);
        AccountingSnapshot data = service.snapshot(from, TODAY, true);
        assertEquals(50, data.summary().collected());
        assertEquals(2, data.payments().size());
        assertEquals(0, data.receiptsByMethod().get(PaymentMethod.CASH));
        assertEquals(20, data.receiptsByMethod().get(PaymentMethod.MOBILE_MONEY));
        assertEquals(30, data.receiptsByMethod().get(PaymentMethod.BANK_TRANSFER));
    }

    @Test void voidingDoesNotEraseMoneyAlreadyCollected() {
        Bill bill = bill("B001", TODAY);
        payment(bill, 75, TODAY, PaymentMethod.CASH);
        bill.voidBill();
        AccountingSnapshot data = snapshot(true);
        assertEquals(0, data.summary().invoiced());
        assertEquals(0, data.summary().outstanding());
        assertEquals(75, data.summary().collected());
        assertEquals(1, data.summary().voidedInvoicePayments());
        assertTrue(data.payments().get(0).voidedInvoice());
    }

    @Test void expenseBillsAndSupplierPaymentsUseTheirOwnDatesAndSurviveRestart() {
        service.recordExpense("Supplier", "Utilities", ExpenseCategory.UTILITIES, "SUP-002",
                TODAY.minusMonths(1), TODAY, 580, 80, TODAY, PaymentMethod.MOBILE_MONEY, "MM-123");
        AccountingService restarted = new AccountingService(auth, store, () -> bills, CLOCK);
        var current = restarted.snapshot(TODAY.withDayOfMonth(1), TODAY, true);
        assertEquals(580, current.summary().expensesPaid());
        assertEquals(0, current.summary().expensesNet());
        assertTrue(current.expenses().isEmpty());
        var previous = restarted.snapshot(TODAY.minusMonths(1).withDayOfMonth(1), TODAY.minusMonths(1), true);
        assertEquals(500, previous.summary().expensesNet());
        assertEquals(80, previous.summary().expenseTax());
        assertEquals(0, previous.summary().expensesPaid());
        assertEquals(PaymentMethod.MOBILE_MONEY, previous.expenses().get(0).getPaymentMethod());
        assertEquals("MM-123", previous.expenses().get(0).getPaymentReference());
    }

    @Test void supplierBillsCanBePaidOnceAndNonCashPaymentsRequireReference() {
        Expense expense = expense();
        assertEquals(580, snapshot(true).summary().suppliersOutstanding());
        login(Role.ACCOUNTANT);
        assertThrows(IllegalArgumentException.class, () -> service.payExpense(expense.getExpenseId(), TODAY, PaymentMethod.BANK_TRANSFER, ""));
        service.payExpense(expense.getExpenseId(), TODAY, PaymentMethod.BANK_TRANSFER, "BANK-987");
        assertEquals(0, snapshot(true).summary().suppliersOutstanding());
        assertEquals(580, snapshot(true).summary().expensesPaid());
        assertThrows(IllegalArgumentException.class, () -> service.payExpense(expense.getExpenseId(), TODAY, PaymentMethod.CASH, ""));
        assertEquals("BANK-987", store.loadExpenses().get(0).getPaymentReference());
        assertEquals("Accountant", store.loadExpenses().get(0).getPaidBy());
        assertEquals("Owner", store.loadExpenses().get(0).getCreatedBy());
    }

    @Test void invalidExpensesCannotEnterTheRegister() {
        for (double amount : new double[] {0, -1, 0.004, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> service.recordExpense("Supplier", "Bill", ExpenseCategory.OTHER,
                    "", TODAY, TODAY, amount, 0, null, null, ""));
        }
        assertThrows(IllegalArgumentException.class, () -> service.recordExpense("Supplier", "Bill", ExpenseCategory.OTHER,
                "", TODAY, TODAY, 100, 101, null, null, ""));
        assertThrows(IllegalArgumentException.class, () -> service.recordExpense("Supplier", "Bill", ExpenseCategory.OTHER,
                "", TODAY, TODAY.minusDays(1), 100, 0, null, null, ""));
        assertTrue(snapshot(true).expenses().isEmpty());
    }

    @Test void failedExpenseWriteDoesNotPublishAnUnsavedRecord() throws Exception {
        snapshot(true);
        Path blocked = directory.resolve(DataStore.FILE_EXPENSES);
        Files.createDirectory(blocked);
        Files.writeString(blocked.resolve("keep.txt"), "Existing data");
        assertThrows(RuntimeException.class, this::expense);
        assertTrue(snapshot(true).expenses().isEmpty());
        assertEquals("Existing data", Files.readString(blocked.resolve("keep.txt")));
    }

    @Test void corruptedExpenseFileIsPreservedAndReported() throws Exception {
        Files.writeString(directory.resolve(DataStore.FILE_EXPENSES), "{broken");
        assertThrows(IllegalStateException.class, () -> snapshot(true));
        assertEquals("{broken", Files.readString(directory.resolve(DataStore.FILE_EXPENSES)));
    }

    @Test void dueDatesPersistAndControlAgeing() {
        Bill bill = bill("B001", TODAY.minusDays(40));
        assertEquals("31–60 days", snapshot(true).outstanding().get(0).ageing());
        service.setInvoiceDueDate(bill.getBillId(), TODAY.plusDays(7));
        assertEquals("Current", snapshot(true).outstanding().get(0).ageing());
        assertEquals(0, snapshot(true).summary().overdue());
        assertEquals(TODAY.plusDays(7), store.loadBills().get(0).getDueDate());
    }

    @Test void incompleteExpenseRecordsAreRejectedWithoutOverwritingTheirFile() throws Exception {
        Files.writeString(directory.resolve(DataStore.FILE_EXPENSES), "[{}]");
        assertThrows(IllegalStateException.class, () -> snapshot(true));
        assertEquals("[{}]", Files.readString(directory.resolve(DataStore.FILE_EXPENSES)));
    }

    @Test void dueDateChangeRollsBackWhenPersistenceFails() throws Exception {
        Bill bill = bill("B001", TODAY.minusDays(4));
        Path blocked = directory.resolve(DataStore.FILE_BILLS);
        Files.createDirectory(blocked);
        Files.writeString(blocked.resolve("keep.txt"), "Keep");
        LocalDate original = bill.getDueDate();
        assertThrows(RuntimeException.class, () -> service.setInvoiceDueDate("B001", TODAY));
        assertEquals(original, bill.getDueDate());
    }

    @Test void ageingBoundariesAndDueTodayAreConsistent() {
        int[] days = {0, 1, 30, 31, 60, 61, 90, 91};
        String[] bands = {"Current", "1–30 days", "1–30 days", "31–60 days", "31–60 days", "61–90 days", "61–90 days", "90+ days"};
        for (int i = 0; i < days.length; i++) {
            Bill bill = bill("B" + i, TODAY.minusDays(days[i]));
            bill.setDueDate(TODAY.minusDays(days[i]));
        }
        var rows = snapshot(true).outstanding();
        for (int i = 0; i < days.length; i++) {
            String id = "B" + i;
            assertEquals(bands[i], rows.stream().filter(r -> r.billId().equals(id)).findFirst().orElseThrow().ageing());
        }
    }

    @Test void sessionIsRecheckedAfterLogoutAndOnEveryExport() {
        assertTrue(snapshot(true).detailed());
        auth.logout();
        assertThrows(SecurityException.class, () -> snapshot(true));
        assertThrows(SecurityException.class, () -> service.exportCsv(AccountingService.Export.PAYMENTS, TODAY, TODAY, true));
    }

    @Test void rejectsReversedDateRangeAndExportsRequiredPaymentMethods() {
        assertThrows(IllegalArgumentException.class, () -> service.snapshot(TODAY, TODAY.minusDays(1), true));
        String csv = service.exportCsv(AccountingService.Export.SUMMARY, TODAY, TODAY, false);
        assertTrue(csv.contains("Cash"));
        assertTrue(csv.contains("Mobile Money"));
        assertTrue(csv.contains("Bank Transfer"));
    }
}
