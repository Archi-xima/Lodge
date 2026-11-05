package com.lodge.controller;

import com.lodge.model.Room;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;
import com.lodge.util.Money;

import java.util.List;
import java.util.Optional;

/**
 * Read-only snapshot of how one room tier is doing right now. The room
 * selection screen draws one card per snapshot.
 *
 * <p>This class has no JavaFX code and does not touch any service, so it can
 * be unit tested on its own. It is built from a plain list of {@link Room}s.</p>
 *
 * <p><b>Member 7 hand-off:</b> Member 7 owns the official availability rules
 * ({@code RoomAvailability} / {@code RoomAvailabilityService}). Until that is
 * merged, this class computes the same figures from room statuses so the screen
 * works today. When Member 7's service is merged, only
 * {@code RoomSelectionController.loadTier(RoomType)} needs to change.</p>
 */
public final class RoomTierViewModel {

    /** Availability label shown on the badge. Same three states as the spec. */
    public enum Level {
        AVAILABLE("Available", "tier-badge-available"),
        LIMITED("Limited", "tier-badge-limited"),
        FULLY_BOOKED("Fully Booked", "tier-badge-full");

        private final String label;
        private final String styleClass;

        Level(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getLabel() {
            return label;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    /**
     * ASSUMPTION (to confirm with Member 7): a tier is "Limited" once
     * this percentage of its sellable rooms is occupied or reserved.
     */
    public static final int LIMITED_FROM_OCCUPANCY_PERCENT = 75;

    private final RoomType type;
    private final int totalRooms;      // sellable rooms: everything except Maintenance
    private final int availableRooms;  // status AVAILABLE
    private final int occupiedRooms;   // status OCCUPIED or RESERVED
    private final int maintenanceRooms;

    private RoomTierViewModel(RoomType type, int available, int occupied, int maintenance) {
        this.type = type;
        this.availableRooms = available;
        this.occupiedRooms = occupied;
        this.maintenanceRooms = maintenance;
        this.totalRooms = available + occupied;
    }

    /** Builds the snapshot for one tier from the full list of rooms. */
    public static RoomTierViewModel of(RoomType type, List<Room> rooms) {
        int available = 0;
        int occupied = 0;
        int maintenance = 0;
        if (rooms != null) {
            for (Room room : rooms) {
                if (room == null || room.getType() != type) {
                    continue;
                }
                RoomStatus status = room.getStatus();
                if (status == RoomStatus.AVAILABLE) {
                    available++;
                } else if (status == RoomStatus.MAINTENANCE) {
                    maintenance++;
                } else {
                    occupied++;   // OCCUPIED, RESERVED (or an unexpected null)
                }
            }
        }
        return new RoomTierViewModel(type, available, occupied, maintenance);
    }

    /** The first free room of this tier, in inventory order. Empty if none. */
    public static Optional<Room> firstAvailableRoom(RoomType type, List<Room> rooms) {
        if (rooms == null) {
            return Optional.empty();
        }
        for (Room room : rooms) {
            if (room != null && room.getType() == type && room.getStatus() == RoomStatus.AVAILABLE) {
                return Optional.of(room);
            }
        }
        return Optional.empty();
    }

    public RoomType getType() {
        return type;
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

    public int getMaintenanceRooms() {
        return maintenanceRooms;
    }

    /** Whole-number occupancy, e.g. 67. A tier with no sellable rooms reads 0. */
    public int getOccupancyPercent() {
        if (totalRooms == 0) {
            return 0;
        }
        return (int) Math.round(occupiedRooms * 100.0 / totalRooms);
    }

    public Level getLevel() {
        if (availableRooms == 0) {
            return Level.FULLY_BOOKED;
        }
        return getOccupancyPercent() >= LIMITED_FROM_OCCUPANCY_PERCENT ? Level.LIMITED : Level.AVAILABLE;
    }

    /** Fully booked tiers cannot be chosen. */
    public boolean isSelectable() {
        return availableRooms > 0;
    }

    /** Text in the spec's format, e.g. "3 of 8 rooms available". */
    public String getAvailabilityText() {
        return availableRooms + " of " + totalRooms + (totalRooms == 1 ? " room" : " rooms") + " available";
    }

    public String getRateText() {
        return Money.format(type.getRatePerNight()) + " / night";
    }

    public String getCapacityText() {
        return "Sleeps " + type.getCapacity();
    }
}
