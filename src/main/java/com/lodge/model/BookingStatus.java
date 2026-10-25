package com.lodge.model;

/** Lifecycle of a reservation. */
public enum BookingStatus {
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked in"),
    CHECKED_OUT("Checked out"),
    CANCELLED("Cancelled");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
