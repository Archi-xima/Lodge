package com.lodge.model;

/** Settlement state of a bill. */
public enum BillStatus {
    UNPAID("Unpaid"),
    PART_PAID("Part paid"),
    PAID("Paid in full"),
    VOID("Void");

    private final String label;

    BillStatus(String label) {
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
