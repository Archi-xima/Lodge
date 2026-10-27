package com.lodge.model;

/** Categories for the accountant's supplier expense register. */
public enum ExpenseCategory {
    FOOD_AND_SUPPLIES("Food & supplies"), UTILITIES("Utilities"), PAYROLL("Payroll"),
    MAINTENANCE("Maintenance"), LAUNDRY("Laundry"), TRANSPORT("Transport"),
    FEES_AND_COMMISSIONS("Fees & commissions"), OTHER("Other");

    private final String label;
    ExpenseCategory(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
