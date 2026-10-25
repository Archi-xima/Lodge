package com.lodge.model;

/** Grouping for the extra services the lodge sells. */
public enum ServiceCategory {
    DINING("Dining"),
    LAUNDRY("Laundry"),
    SPA("Spa & Wellness"),
    TRANSPORT("Transport"),
    MINIBAR("Minibar"),
    ACTIVITIES("Activities");

    private final String label;

    ServiceCategory(String label) {
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
