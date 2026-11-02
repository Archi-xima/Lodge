
package com.lodge.model;

/**
 * Holds availability and occupancy information for a room type.
 */
public class RoomAvailability {

    private RoomType roomType;
    private int totalRooms;
    private int availableRooms;
    private int occupiedRooms;
    private int reservedRooms;
    private int maintenanceRooms;
    private double occupancyPercentage;

    public RoomAvailability(RoomType roomType, int totalRooms,
                            int availableRooms, int occupiedRooms) {
        this(roomType, totalRooms, availableRooms, occupiedRooms, 0, 0);
    }

    public RoomAvailability(RoomType roomType, int totalRooms,
                            int availableRooms, int occupiedRooms,
                            int reservedRooms, int maintenanceRooms) {

        this.roomType = roomType;
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
        this.occupiedRooms = occupiedRooms;
        this.reservedRooms = reservedRooms;
        this.maintenanceRooms = maintenanceRooms;

        if (totalRooms > 0) {
            this.occupancyPercentage =
                    ((double) occupiedRooms / totalRooms) * 100;
        } else {
            this.occupancyPercentage = 0;
        }
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public int getOccupiedRooms() {
        return occupiedRooms;
    }

    public int getReservedRooms() {
        return reservedRooms;
    }

    public int getMaintenanceRooms() {
        return maintenanceRooms;
    }

    public double getOccupancyPercentage() {
        return occupancyPercentage;
    }

    public String getAvailabilityText() {
        return availableRooms + " of " + totalRooms + " rooms available";
    }

    public String getAvailabilityState() {
        if (totalRooms == 0) {
            return "No Rooms";
        }

        if (availableRooms == 0) {
            if (maintenanceRooms == totalRooms) {
                return "Under Maintenance";
            }
            return "Fully Booked";
        }

        if (((double) availableRooms / totalRooms) < 0.30) {
            return "Limited";
        }

        return "Available";
    }
}
