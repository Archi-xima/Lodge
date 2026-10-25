package com.lodge.model;

/** How a guest settles their bill. */
public enum PaymentMethod {
    CASH("Cash"),
    MOBILE_MONEY("Mobile Money"),
    CARD("Card"),
    BANK_TRANSFER("Bank Transfer");

    private final String label;

    PaymentMethod(String label) {
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
