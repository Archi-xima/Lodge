package com.lodge.service;

import com.lodge.model.Room;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Room inventory: creating, updating and looking up rooms, plus availability.
 */
public class RoomService {

    private static RoomService instance;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();

    private RoomService() {
        rooms.setAll(DataStore.getInstance().loadRooms());
        if (rooms.isEmpty()) {
            seedRooms();
        }
    }

    public static synchronized RoomService getInstance() {
        if (instance == null) {
            instance = new RoomService();
        }
        return instance;
    }

    private void seedRooms() {
        rooms.addAll(
                room("101", RoomType.STANDARD, 1, RoomStatus.OCCUPIED, "Double bed", "En-suite shower", "Wi-Fi"),
                room("102", RoomType.STANDARD, 1, RoomStatus.AVAILABLE, "Double bed", "En-suite shower", "Wi-Fi"),
                room("103", RoomType.STANDARD, 1, RoomStatus.AVAILABLE, "Twin beds", "En-suite shower", "Garden view"),
                room("104", RoomType.DELUXE, 1, RoomStatus.OCCUPIED, "Queen bed", "Aircon", "Pool view", "Wi-Fi"),
                room("105", RoomType.DELUXE, 1, RoomStatus.RESERVED, "Queen bed", "Aircon", "Balcony"),
                room("201", RoomType.DELUXE, 2, RoomStatus.AVAILABLE, "King bed", "Aircon", "Lounge corner"),
                room("202", RoomType.FAMILY, 2, RoomStatus.OCCUPIED, "Two bedrooms", "Sleeps 4", "Fridge"),
                room("203", RoomType.FAMILY, 2, RoomStatus.AVAILABLE, "Two bedrooms", "Sleeps 4", "Kids corner"),
                room("204", RoomType.EXECUTIVE, 2, RoomStatus.AVAILABLE, "Four-poster bed", "Private lounge", "Balcony"),
                room("205", RoomType.EXECUTIVE, 2, RoomStatus.MAINTENANCE, "Four-poster bed", "Aircon", "Work desk"),
                room("C1", RoomType.COTTAGE, 0, RoomStatus.AVAILABLE, "Thatched cottage", "Veranda", "Butler service"),
                room("C2", RoomType.COTTAGE, 0, RoomStatus.OCCUPIED, "Thatched cottage", "Veranda", "Private garden")
        );
        save();
    }

    private Room room(String number, RoomType type, int floor, RoomStatus status, String... features) {
        Room room = new Room(number, type, floor);
        room.setStatus(status);
        room.setFeatures(new ArrayList<>(Arrays.asList(features)));
        return room;
    }

    public ObservableList<Room> getRooms() {
        return rooms;
    }

    /** Rooms currently matching a type (null = all types). */
    public FilteredList<Room> filteredBy(RoomType type) {
        return new FilteredList<>(rooms, room -> type == null || room.getType() == type);
    }

    public Room findByNumber(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }
        for (Room room : rooms) {
            if (roomNumber.equalsIgnoreCase(room.getRoomNumber())) {
                return room;
            }
        }
        return null;
    }

    /** Rooms that are free to sell right now (available or under maintenance). */
    public List<Room> sellableRooms() {
        List<Room> sellable = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getStatus() == RoomStatus.AVAILABLE || room.getStatus() == RoomStatus.RESERVED) {
                sellable.add(room);
            }
        }
        return sellable;
    }

    public String nextRoomNumber() {
        return DataStore.getInstance().nextId("R", existingNumbers());
    }

    private List<String> existingNumbers() {
        List<String> ids = new ArrayList<>();
        for (Room room : rooms) {
            ids.add(room.getRoomNumber());
        }
        return ids;
    }

    public boolean addRoom(Room room) {
        if (room == null || findByNumber(room.getRoomNumber()) != null) {
            return false;
        }
        rooms.add(room);
        save();
        AuthService.getInstance().log("ADD ROOM", room.getRoomNumber() + " (" + room.getTypeName() + ")");
        return true;
    }

    public void updateRoom(Room room) {
        save();
        AuthService.getInstance().log("UPDATE ROOM", room.getRoomNumber() + " is now " + room.getStatusLabel());
    }

    public boolean removeRoom(Room room) {
        boolean removed = rooms.remove(room);
        if (removed) {
            save();
            AuthService.getInstance().log("DELETE ROOM", room.getRoomNumber());
        }
        return removed;
    }

    public void setStatus(Room room, RoomStatus status) {
        room.setStatus(status);
        save();
    }

    public void save() {
        DataStore.getInstance().saveRooms(new ArrayList<>(rooms));
    }

    public long countWithStatus(RoomStatus status) {
        return rooms.stream().filter(r -> r.getStatus() == status).count();
    }
}
