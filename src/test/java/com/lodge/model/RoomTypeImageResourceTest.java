package com.lodge.model;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class RoomTypeImageResourceTest {

    @Test
    public void everyRoomTypePointsToAnImageBundledWithTheApplication() {
        ClassLoader classLoader = RoomType.class.getClassLoader();

        for (RoomType roomType : RoomType.values()) {
            assertNotNull(roomType + " image is missing: " + roomType.getImageResource(),
                    classLoader.getResource(roomType.getImageResource()));
        }
    }
}
