
package com.lodge.service;

import com.lodge.model.Room;
import com.lodge.model.RoomAvailability;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;

import java.util.ArrayList;
import java.util.List;

public class RoomAvailabilityService {

    private static RoomAvailabilityService instance;

    private final RoomService roomService;

    private RoomAvailabilityService() {
        roomService = RoomService.getInstance();
    }

    public static synchronized RoomAvailabilityService getInstance() {
        if (instance == null) {
            instance = new RoomAvailabilityService();
        }
        return instance;
    }

    public RoomAvailability getAvailability(RoomType type) {

        int total = 0;
        int available = 0;
        int occupied = 0;
        int reserved = 0;
        int maintenance = 0;

        for (Room room : roomService.getRooms()) {

            if (room.getType() == type) {

                total++;

                if (room.getStatus() == RoomStatus.AVAILABLE) {
                    available++;
                } else if (room.getStatus() == RoomStatus.OCCUPIED) {
                    occupied++;
                } else if (room.getStatus() == RoomStatus.RESERVED) {
                    reserved++;
                } else if (room.getStatus() == RoomStatus.MAINTENANCE) {
                    maintenance++;
                }
            }
        }

        return new RoomAvailability(
                type,
                total,
                available,
                occupied,
                reserved,
                maintenance
        );
    }

    public List<RoomAvailability> getAllAvailability() {

        List<RoomAvailability> results = new ArrayList<>();

        for (RoomType type : RoomType.values()) {
            results.add(getAvailability(type));
        }

        return results;
    }

    public int getTotalRooms() {
        return roomService.getRooms().size();
    }

    public int getTotalAvailableRooms() {
        int available = 0;

        for (Room room : roomService.getRooms()) {
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                available++;
            }
        }

        return available;
    }

    public int getTotalOccupiedRooms() {
        int occupied = 0;

        for (Room room : roomService.getRooms()) {
            if (room.getStatus() == RoomStatus.OCCUPIED) {
                occupied++;
            }
        }

        return occupied;
    }

    public int getTotalReservedRooms() {
        int reserved = 0;

        for (Room room : roomService.getRooms()) {
            if (room.getStatus() == RoomStatus.RESERVED) {
                reserved++;
            }
        }

        return reserved;
    }

    public int getTotalMaintenanceRooms() {
        int maintenance = 0;

        for (Room room : roomService.getRooms()) {
            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                maintenance++;
            }
        }

        return maintenance;
    }

    public double getOverallOccupancyPercentage() {

        int total = getTotalRooms();

        if (total == 0) {
            return 0;
        }

        return ((double) getTotalOccupiedRooms() / total) * 100;
    }
}
