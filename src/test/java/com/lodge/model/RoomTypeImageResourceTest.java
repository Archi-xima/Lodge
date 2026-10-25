package com.lodge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoomTypeImageResourceTest {

    @Test
    void everyRoomTypePointsToAnImageBundledWithTheApplication() {
        ClassLoader classLoader = RoomType.class.getClassLoader();

        for (RoomType roomType : RoomType.values()) {
            assertNotNull(classLoader.getResource(roomType.getImageResource()),
                    roomType + " image is missing: " + roomType.getImageResource());
        }
    }
}
