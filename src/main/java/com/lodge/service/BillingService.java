package com.lodge.service;

import com.lodge.model.*;
import com.lodge.util.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoices, payments and the extras price list.
 */
public class BillingService {

    private static BillingService instance;

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final ObservableList<ServiceItem> serviceCatalog = FXCollections.observableArrayList();

    private BillingService() {
        bills.setAll(DataStore.getInstance().loadBills());
        serviceCatalog.setAll(DataStore.getInstance().loadServices());
        if (serviceCatalog.isEmpty()) {
            seedCatalog();
        }
        if (bills.isEmpty()) {
            seedBills();
        }
        DataStore.getInstance().getPersistence().registerAutoSave("bills", this::save);
        DataStore.getInstance().getPersistence().registerAutoSave("services", this::saveCatalog);
    }

    public static synchronized BillingService getInstance() {
        if (instance == null) {
            instance = new BillingService();
        }
        return instance;
    }

    // ------------------------------------------------------------------ seed data

    private void seedCatalog() {
        serviceCatalog.addAll(
                new ServiceItem("S001", "Full breakfast", ServiceCategory.DINING, 60.00, "plate"),
                new ServiceItem("S002", "Three-course dinner", ServiceCategory.DINING, 120.00, "plate"),
                new ServiceItem("S003", "Braai platter", ServiceCategory.DINING, 150.00, "platter"),
                new ServiceItem("S004", "Laundry service", ServiceCategory.LAUNDRY, 40.00, "bag"),
                new ServiceItem("S005", "Airport transfer", ServiceCategory.TRANSPORT, 250.00, "trip"),
                new ServiceItem("S006", "Village & river tour", ServiceCategory.ACTIVITIES, 180.00, "person"),
                new ServiceItem("S007", "Spa massage", ServiceCategory.SPA, 300.00, "session"),
                new ServiceItem("S008", "Minibar restock", ServiceCategory.MINIBAR, 90.00, "stock"),
                new ServiceItem("S009", "Extra bed", ServiceCategory.DINING, 80.00, "night"),
                new ServiceItem("S010", "Late check-out (18:00)", ServiceCategory.DINING, 100.00, "stay"),
                new ServiceItem("S011", "Poolside drinks", ServiceCategory.MINIBAR, 45.00, "round"),
                new ServiceItem("S012", "Guided game walk", ServiceCategory.ACTIVITIES, 220.00, "person")
        );
        saveCatalog();
    }

    private void seedBills() {
        RoomService rooms = RoomService.getInstance();

        Bill b1 = Bill.walkIn("B001", "Mary Banda", "+260 966 222 111",
                rooms.findByNumber("103"), 2, 100.00, "Walk-in guest", "Reception");
        b1.addPayment(new Payment("P001", "B001", 966.00, PaymentMethod.CASH, "Till 01", "Reception"));

        Bill b2 = Bill.walkIn("B002", "John Phiri", "+260 977 123 456",
                rooms.findByNumber("104"), 5, 200.00, "Late check-in requested", "Reception");
        b2.addPayment(new Payment("P002", "B002", 1000.00, PaymentMethod.MOBILE_MONEY, "Ref 88213", "Reception"));

        bills.addAll(b1, b2);
        save();
    }

    // ------------------------------------------------------------------ bills

    public ObservableList<Bill> getBills() {
        return bills;
    }

    public Bill findById(String billId) {
        for (Bill bill : bills) {
            if (billId != null && billId.equals(bill.getBillId())) {
                return bill;
            }
        }
        return null;
    }

    public String nextBillId() {
        List<String> ids = new ArrayList<>();
        for (Bill bill : bills) {
            ids.add(bill.getBillId());
        }
        return DataStore.getInstance().nextId("B", ids);
    }

    public void addBill(Bill bill) {
        bills.add(bill);
        save();
        AuthService.getInstance().log("ADD BILL", bill.getBillId() + " — " + bill.getGuestName()
                + " — " + Money.format(bill.getTotal()));
    }

    public boolean removeBill(Bill bill) {
        boolean removed = bills.remove(bill);
        if (removed) {
            save();
            AuthService.getInstance().log("DELETE BILL", bill.getBillId());
        }
        return removed;
    }

    public void voidBill(Bill bill) {
        bill.voidBill();
        save();
        AuthService.getInstance().log("VOID BILL", bill.getBillId() + " — " + bill.getGuestName());
    }

    /** Records a payment against a bill and refreshes its status. */
    public Payment recordPayment(Bill bill, double amount, PaymentMethod method, String reference) {
        Payment payment = new Payment(nextPaymentId(), bill.getBillId(), amount, method, reference, AuthService.getInstance().currentUserName());
        bill.addPayment(payment);
        save();
        AuthService.getInstance().log("PAYMENT", payment.getFormattedAmount() + " received for "
                + bill.getBillId() + " via " + payment.getMethodLabel());
        return payment;
    }

    private String nextPaymentId() {
        List<String> ids = new ArrayList<>();
        for (Bill bill : bills) {
            for (Payment payment : bill.getPayments()) {
                ids.add(payment.getPaymentId());
            }
        }
        return DataStore.getInstance().nextId("P", ids);
    }

    /** Adds an extra charge to a bill, e.g. dinner for two. */
    public void addExtra(Bill bill, ServiceItem item, int quantity) {
        bill.addLine(BillLine.fromService(item, quantity));
        save();
        AuthService.getInstance().log("ADD EXTRA", quantity + " x " + item.getName() + " on " + bill.getBillId());
    }

    public boolean removeLine(Bill bill, BillLine line) {
        boolean removed = bill.removeLine(line);
        if (removed) {
            save();
        }
        return removed;
    }

    public void save() {
        DataStore.getInstance().saveBills(new ArrayList<>(bills));
    }

    // ------------------------------------------------------------------ extras catalogue

    public ObservableList<ServiceItem> getServiceCatalog() {
        return serviceCatalog;
    }

    public void saveCatalog() {
        DataStore.getInstance().saveServices(new ArrayList<>(serviceCatalog));
    }

    public String nextServiceId() {
        List<String> ids = new ArrayList<>();
        for (ServiceItem item : serviceCatalog) {
            ids.add(item.getServiceId());
        }
        return DataStore.getInstance().nextId("S", ids);
    }

    // ------------------------------------------------------------------ reporting helpers

    public List<Bill> billsInMonth(YearMonth month) {
        List<Bill> result = new ArrayList<>();
        for (Bill bill : bills) {
            if (bill.getCreatedAt() != null && month.atDay(1).equals(bill.getCreatedAt().toLocalDate().withDayOfMonth(1))
                    && YearMonth.from(bill.getCreatedAt()).equals(month)) {
                result.add(bill);
            }
        }
        return result;
    }

    public List<Bill> billsCreatedOn(LocalDate date) {
        List<Bill> result = new ArrayList<>();
        for (Bill bill : bills) {
            if (bill.getCreatedAt() != null && bill.getCreatedAt().toLocalDate().equals(date)) {
                result.add(bill);
            }
        }
        return result;
    }

    public List<Bill> outstandingBills() {
        List<Bill> result = new ArrayList<>();
        for (Bill bill : bills) {
            if (bill.getStatus() == BillStatus.UNPAID || bill.getStatus() == BillStatus.PART_PAID) {
                result.add(bill);
            }
        }
        return result;
    }

    public double totalOutstanding() {
        double sum = 0;
        for (Bill bill : outstandingBills()) {
            sum += bill.getBalance();
        }
        return Money.round(sum);
    }

    public double revenueInMonth(YearMonth month) {
        double sum = 0;
        for (Bill bill : bills) {
            for (Payment payment : bill.getPayments()) {
                if (payment.getTimestamp() != null && YearMonth.from(payment.getTimestamp()).equals(month)) {
                    sum += payment.getAmount();
                }
            }
        }
        return Money.round(sum);
    }

    public double billedInMonth(YearMonth month) {
        double sum = 0;
        for (Bill bill : bills) {
            if (bill.getCreatedAt() != null && YearMonth.from(bill.getCreatedAt()).equals(month)
                    && bill.getStatus() != BillStatus.VOID) {
                sum += bill.getTotal();
            }
        }
        return Money.round(sum);
    }
}
