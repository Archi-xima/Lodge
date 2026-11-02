
package com.lodge.model;
import com.google.gson.annotations.SerializedName;

/**
 * The four room categories offered by the lodge.
 */
public enum RoomType {

    STANDARD(
            "Standard Room",
            350.00,
            2,
            1,
            "Basic furnishings, shared facilities",
            "com/lodge/images/room-standard.jpg"
    ),

    DELUXE(
            "Deluxe Room",
            550.00,
            3,
            2,
            "Premium furnishings, en-suite bathroom",
            "com/lodge/images/room-deluxe.jpg"
    ),

    FAMILY(
            "Family Room",
            750.00,
            5,
            4,
            "Multiple beds, kitchenette",
            "com/lodge/images/room-family.jpg"
    ),

    @SerializedName(value = "COTTAGE", alternate = {"EXECUTIVE"})

    COTTAGE(
            "Thatched Deluxe Cottage",
            1200.00,
            4,
            2,
            "Private cottage, luxury finishes, outdoor deck",
            "com/lodge/images/cottage.jpg"
    );

    private final String displayName;
    private final double ratePerNight;
    private final int capacity;
    private final int minimumCapacity;
    private final String description;
    private final String imageResource;

    RoomType(String displayName, double ratePerNight,
             int capacity, int minimumCapacity,
             String description, String imageResource) {
        this.displayName = displayName;
        this.ratePerNight = ratePerNight;
        this.capacity = capacity;
        this.minimumCapacity = minimumCapacity;
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

    public int getMinimumCapacity() {
        return minimumCapacity;
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
        if (name != null) {
            for (RoomType type : values()) {
                if (type.displayName.equalsIgnoreCase(name)
                        || type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
        }

        return STANDARD;
    }
}
