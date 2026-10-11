package com.lodge.service;

import com.lodge.model.Bill;
import com.lodge.model.RoomType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BillingService {
    private static BillingService instance;
    private final ObservableList<Bill> savedBills = FXCollections.observableArrayList();
    private int billCounter = 3; // next will be B004

    private BillingService() {
        seedInitialBills();
    }

    public static synchronized BillingService getInstance() {
        if (instance == null) {
            instance = new BillingService();
        }
        return instance;
    }

    private void seedInitialBills() {
        // Pre-populate exact sample bills as shown in the screenshot
        Bill b1 = new Bill("B002", "Mary Banda", RoomType.STANDARD, 2, 100.0, 500.0, "Standard room booking", "Reception");
        Bill b2 = new Bill("B003", "John Phiri", RoomType.DELUXE, 5, 200.0, 1000.0, "Late check-in requested", "Reception");
        savedBills.addAll(b1, b2);
    }

    public ObservableList<Bill> getSavedBills() {
        return savedBills;
    }

    public synchronized String generateNextBillId() {
        billCounter++;
        return String.format("B%03d", billCounter);
    }

    public void addBill(Bill bill) {
        savedBills.add(bill);
    }

    public boolean removeBill(Bill bill) {
        return savedBills.remove(bill);
    }

    public void clearAll() {
        savedBills.clear();
        billCounter = 0;
    }
}
