package com.lodge.model;

import com.lodge.util.Money;

import java.util.ArrayList;
import java.util.List;

/** A physical room (or cottage) in the lodge. */
public class Room {

    private String roomNumber;
    private RoomType type;
    private int floor;
    private RoomStatus status;
    private double ratePerNight;   // 0 means "use the standard rate of the room type"
    private List<String> features = new ArrayList<>();

    public Room() {
        this.status = RoomStatus.AVAILABLE;
    }

    public Room(String roomNumber, RoomType type, int floor) {
        this();
        this.roomNumber = roomNumber;
        this.type = type;
        this.floor = floor;
    }

    /** Effective nightly rate: a room specific rate wins over the type default. */
    public double getEffectiveRate() {
        if (ratePerNight > 0) {
            return ratePerNight;
        }
        return type != null ? type.getRatePerNight() : 0.0;
    }

    public String getFormattedRate() {
        return Money.format(getEffectiveRate());
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public String getTypeName() {
        return type != null ? type.getDisplayName() : "";
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return status == null ? "" : status.getLabel();
    }

    public double getRatePerNight() {
        return ratePerNight;
    }

    public void setRatePerNight(double ratePerNight) {
        this.ratePerNight = ratePerNight;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features != null ? features : new ArrayList<>();
    }

    public String getFeaturesText() {
        return features == null || features.isEmpty() ? "—" : String.join(", ", features);
    }

    public String getImageResource() {
        return type != null ? type.getImageResource() : "com/lodge/images/hero-lodge.jpg";
    }

    @Override
    public String toString() {
        return roomNumber + " — " + getTypeName();
    }
}
