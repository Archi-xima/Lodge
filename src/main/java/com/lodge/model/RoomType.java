package com.lodge.model;

/**
 * The categories of rooms the lodge sells, each with its own nightly rate,
 * capacity, marketing description and showcase photograph.
 */
public enum RoomType {
    STANDARD("Standard Room", 350.00, 2, "Cosy room with a double bed, en-suite shower and garden view.", "com/lodge/images/room-standard.jpg"),
    DELUXE("Deluxe Room", 550.00, 2, "Spacious room with queen bed, air-conditioning and pool view.", "com/lodge/images/room-deluxe.jpg"),
    FAMILY("Family Room", 750.00, 4, "Two bedrooms under one roof, ideal for families up to four guests.", "com/lodge/images/room-family.jpg"),
    EXECUTIVE("Executive Suite", 1200.00, 3, "Suite with lounge area, four-poster bed and private balcony.", "com/lodge/images/room-executive.jpg"),
    COTTAGE("VIP Thatched Cottage", 1500.00, 2, "Standalone thatched cottage with veranda and dedicated butler.", "com/lodge/images/cottage.jpg");

    private final String displayName;
    private final double ratePerNight;
    private final int capacity;
    private final String description;
    private final String imageResource;

    RoomType(String displayName, double ratePerNight, int capacity, String description, String imageResource) {
        this.displayName = displayName;
        this.ratePerNight = ratePerNight;
        this.capacity = capacity;
        this.description = description;
        this.imageResource = imageResource;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getRatePerNight() {
        return ratePerNight;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public String getImageResource() {
        return imageResource;
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
