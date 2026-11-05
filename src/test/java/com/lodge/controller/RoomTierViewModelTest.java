package com.lodge.controller;

import com.lodge.model.Room;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class RoomTierViewModelTest {

    private static Room room(String number, RoomType type, RoomStatus status) {
        Room room = new Room(number, type, 1);
        room.setStatus(status);
        return room;
    }

    private static List<Room> deluxeTier(int available, int occupied, int reserved, int maintenance) {
        List<Room> rooms = new ArrayList<>();
        int n = 1;
        for (int i = 0; i < available; i++) rooms.add(room("D" + n++, RoomType.DELUXE, RoomStatus.AVAILABLE));
        for (int i = 0; i < occupied; i++) rooms.add(room("D" + n++, RoomType.DELUXE, RoomStatus.OCCUPIED));
        for (int i = 0; i < reserved; i++) rooms.add(room("D" + n++, RoomType.DELUXE, RoomStatus.RESERVED));
        for (int i = 0; i < maintenance; i++) rooms.add(room("D" + n++, RoomType.DELUXE, RoomStatus.MAINTENANCE));
        return rooms;
    }

    @Test
    public void availabilityTextUsesTheSpecFormat() {
        RoomTierViewModel tier = RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(3, 4, 1, 0));
        assertEquals("3 of 8 rooms available", tier.getAvailabilityText());
    }

    @Test
    public void occupancyCountsOccupiedAndReservedButNotMaintenance() {
        RoomTierViewModel tier = RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(2, 1, 1, 3));
        assertEquals(4, tier.getTotalRooms());          // maintenance rooms are not sellable
        assertEquals(2, tier.getOccupiedRooms());
        assertEquals(3, tier.getMaintenanceRooms());
        assertEquals(50, tier.getOccupancyPercent());
    }

    @Test
    public void roomsOfOtherTiersAreIgnored() {
        List<Room> rooms = deluxeTier(1, 0, 0, 0);
        rooms.add(room("S1", RoomType.STANDARD, RoomStatus.AVAILABLE));
        assertEquals(1, RoomTierViewModel.of(RoomType.DELUXE, rooms).getTotalRooms());
    }

    @Test
    public void levelIsAvailableLimitedOrFullyBooked() {
        assertEquals(RoomTierViewModel.Level.AVAILABLE,
                RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(3, 1, 0, 0)).getLevel());    // 25%
        assertEquals(RoomTierViewModel.Level.LIMITED,
                RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(1, 3, 0, 0)).getLevel());    // 75%
        assertEquals(RoomTierViewModel.Level.FULLY_BOOKED,
                RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(0, 2, 1, 0)).getLevel());    // 100%
    }

    @Test
    public void fullyBookedTierCannotBeSelected() {
        RoomTierViewModel full = RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(0, 2, 0, 0));
        assertFalse(full.isSelectable());
        assertTrue(RoomTierViewModel.of(RoomType.DELUXE, deluxeTier(1, 1, 0, 0)).isSelectable());
    }

    @Test
    public void tierWithNoRoomsIsSafe() {
        RoomTierViewModel empty = RoomTierViewModel.of(RoomType.COTTAGE, new ArrayList<>());
        assertEquals(0, empty.getOccupancyPercent());
        assertFalse(empty.isSelectable());
        assertEquals("0 of 0 rooms available", empty.getAvailabilityText());
        assertEquals(RoomTierViewModel.Level.FULLY_BOOKED, empty.getLevel());
    }

    @Test
    public void nullRoomListIsSafe() {
        assertFalse(RoomTierViewModel.of(RoomType.STANDARD, null).isSelectable());
        assertEquals(Optional.empty(), RoomTierViewModel.firstAvailableRoom(RoomType.STANDARD, null));
    }

    @Test
    public void firstAvailableRoomSkipsBusyRoomsAndKeepsInventoryOrder() {
        List<Room> rooms = new ArrayList<>();
        rooms.add(room("101", RoomType.STANDARD, RoomStatus.OCCUPIED));
        rooms.add(room("102", RoomType.STANDARD, RoomStatus.AVAILABLE));
        rooms.add(room("103", RoomType.STANDARD, RoomStatus.AVAILABLE));
        assertEquals("102", RoomTierViewModel.firstAvailableRoom(RoomType.STANDARD, rooms).get().getRoomNumber());
    }

    @Test
    public void rateTextUsesTheRoomTypeRate() {
        assertEquals("K 550.00 / night", RoomTierViewModel.of(RoomType.DELUXE, new ArrayList<>()).getRateText());
    }
}
