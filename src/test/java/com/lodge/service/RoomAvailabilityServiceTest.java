
package com.lodge.service;

import com.lodge.model.RoomAvailability;
import com.lodge.model.RoomType;
import org.junit.Test;

import static org.junit.Assert.*;

public class RoomAvailabilityServiceTest {

    @Test
    public void availabilityCalculatesOccupancyCorrectly() {
        RoomAvailability availability =
                new RoomAvailability(RoomType.STANDARD, 3, 1, 2);

        assertEquals(3, availability.getTotalRooms());
        assertEquals(1, availability.getAvailableRooms());
        assertEquals(2, availability.getOccupiedRooms());
        assertEquals(66.67, availability.getOccupancyPercentage(), 0.01);
        assertEquals("1 of 3 rooms available",
                availability.getAvailabilityText());

        // 1 out of 3 rooms is 33.33% available, so the state is Available.
        assertEquals("Available", availability.getAvailabilityState());
    }

    @Test
    public void allRoomsUnavailableMeansFullyBooked() {
        RoomAvailability availability =
                new RoomAvailability(RoomType.STANDARD, 3, 0, 3);

        assertEquals(0, availability.getAvailableRooms());
        assertEquals("0 of 3 rooms available",
                availability.getAvailabilityText());
        assertEquals("Fully Booked", availability.getAvailabilityState());
        assertEquals(100.0, availability.getOccupancyPercentage(), 0.01);
    }

    @Test
    public void allRoomsAvailableMeansAvailable() {
        RoomAvailability availability =
                new RoomAvailability(RoomType.STANDARD, 3, 3, 0);

        // All rooms are available, so occupancy is 0%.
        assertEquals(0.0, availability.getOccupancyPercentage(), 0.01);
        assertEquals("Available", availability.getAvailabilityState());
    }

    @Test
    public void zeroRoomsDoesNotCauseDivisionByZero() {
        RoomAvailability availability =
                new RoomAvailability(RoomType.STANDARD, 0, 0, 0);

        assertEquals(0.0, availability.getOccupancyPercentage(), 0.01);
        assertEquals("No Rooms", availability.getAvailabilityState());
        assertEquals("0 of 0 rooms available",
                availability.getAvailabilityText());
    }
}
