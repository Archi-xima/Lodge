package com.lodge.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** A reservation for a room by a guest over a date range. */
public class Booking {

    private String bookingId;
    private String guestId;
    private String guestName;
    private String phone;
    private String roomNumber;
    private String roomTypeName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BookingStatus status;
    private double nightlyRate;
    private String notes;
    private String billId;
    private String createdBy;
    private LocalDateTime createdAt;

    public Booking() {
        this.createdAt = LocalDateTime.now();
        this.status = BookingStatus.CONFIRMED;
    }

    public Booking(String bookingId, Guest guest, Room room, LocalDate checkIn, LocalDate checkOut, String notes, String createdBy) {
        this();
        this.bookingId = bookingId;
        this.guestId = guest.getGuestId();
        this.guestName = guest.getFullName();
        this.phone = guest.getPhone();
        this.roomNumber = room.getRoomNumber();
        this.roomTypeName = room.getTypeName();
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nightlyRate = room.getEffectiveRate();
        this.notes = notes;
        this.createdBy = createdBy;
    }

    /** Number of nights between check-in and check-out (minimum 1). */
    public int getNights() {
        if (checkIn == null || checkOut == null) {
            return 1;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return nights < 1 ? 1 : (int) nights;
    }

    public boolean isArrivingOn(LocalDate date) {
        return status == BookingStatus.CONFIRMED && checkIn != null && checkIn.equals(date);
    }

    public boolean isDepartingOn(LocalDate date) {
        return status == BookingStatus.CHECKED_IN && checkOut != null && checkOut.equals(date);
    }

    public boolean isInHouse() {
        return status == BookingStatus.CHECKED_IN;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return status == null ? "" : status.getLabel();
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
