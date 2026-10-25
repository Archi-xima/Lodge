package com.lodge.model;

/**
 * Staff roles and the permissions each one carries.
 * Access control is decided centrally through {@link #can(Permission)}.
 */
public enum Role {
    ADMIN("Administrator", 4),
    MANAGER("Manager", 3),
    RECEPTIONIST("Receptionist", 2),
    ACCOUNTANT("Accountant", 2),
    CUSTOMER("Customer", 0);

    private final String label;
    private final int rank;

    Role(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    public String getLabel() {
        return label;
    }

    public int getRank() {
        return rank;
    }

    /** Every role can serve guests and raise bills. */
    public boolean can(Permission permission) {
        if (this == CUSTOMER) {
            return false;
        }
        switch (permission) {
            case MANAGE_USERS:
            case DELETE_BILL:
                return this == ADMIN;
            case MANAGE_ROOMS:
            case MANAGE_BOOKINGS:
            case VIEW_REPORTS:
                return rank >= RECEPTIONIST.rank;
            case RECORD_PAYMENT:
            case BILLING:
            case VIEW_GUESTS:
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return label;
    }

    /** Capabilities checked by the controllers before showing or allowing an action. */
    public enum Permission {
        BILLING, RECORD_PAYMENT, VIEW_GUESTS, MANAGE_BOOKINGS, MANAGE_ROOMS, VIEW_REPORTS, DELETE_BILL, MANAGE_USERS
    }
}
