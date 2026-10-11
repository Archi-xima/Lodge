package com.lodge.model;

public enum RoomType {
    STANDARD("Standard", 350.00),
    DELUXE("Deluxe", 550.00),
    FAMILY("Family", 750.00);

    private final String displayName;
    private final double ratePerNight;

    RoomType(String displayName, double ratePerNight) {
        this.displayName = displayName;
        this.ratePerNight = ratePerNight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getRatePerNight() {
        return ratePerNight;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static RoomType fromDisplayName(String name) {
        for (RoomType rt : values()) {
            if (rt.displayName.equalsIgnoreCase(name)) {
                return rt;
            }
        }
        return STANDARD;
    }
}
