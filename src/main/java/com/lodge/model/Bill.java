package com.lodge.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Bill {
    private String billId;
    private String guestName;
    private RoomType roomType;
    private int nights;
    private double extras;
    private double amountPaid;
    private String notes;
    private double roomCharge;
    private double discount;
    private double total;
    private double balance;
    private LocalDateTime createdAt;
    private String createdBy;

    public Bill(String billId, String guestName, RoomType roomType, int nights, double extras, double amountPaid, String notes, String createdBy) {
        this.billId = billId;
        this.guestName = guestName;
        this.roomType = roomType;
        this.nights = nights;
        this.extras = extras;
        this.amountPaid = amountPaid;
        this.notes = notes != null ? notes : "";
        this.createdBy = createdBy != null ? createdBy : "Staff";
        this.createdAt = LocalDateTime.now();
        calculate();
    }

    public void calculate() {
        this.roomCharge = (roomType != null ? roomType.getRatePerNight() : 0.0) * nights;
        // Business Rule from screenshot:
        // 5 nights or more: 5% off the room charge.
        // Extras get no discount. No tax in this lab.
        if (nights >= 5) {
            this.discount = this.roomCharge * 0.05;
        } else {
            this.discount = 0.0;
        }
        this.total = (this.roomCharge - this.discount) + this.extras;
        this.balance = this.total - this.amountPaid;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
        calculate();
    }

    public String getRoomTypeName() {
        return roomType != null ? roomType.getDisplayName() : "";
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
        calculate();
    }

    public double getExtras() {
        return extras;
    }

    public void setExtras(double extras) {
        this.extras = extras;
        calculate();
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
        calculate();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getRoomCharge() {
        return roomCharge;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
    }

    public double getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Formatted strings for UI and TableView
    public String getFormattedTotal() {
        return String.format("K %.2f", total);
    }

    public String getFormattedPaid() {
        return String.format("K %.2f", amountPaid);
    }

    public String getFormattedBalance() {
        return String.format("K %.2f", balance);
    }

    public String getFormattedRate() {
        return roomType != null ? String.format("K %.2f", roomType.getRatePerNight()) : "K 0.00";
    }

    public String getFormattedRoomCharge() {
        return String.format("K %.2f", roomCharge);
    }

    public String getFormattedDiscount() {
        return String.format("K %.2f", discount);
    }

    public String getFormattedExtras() {
        return String.format("K %.2f", extras);
    }

    public String getFormattedDate() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
    }
}
