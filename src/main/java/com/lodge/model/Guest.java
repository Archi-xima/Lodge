package com.lodge.model;

import java.time.LocalDateTime;

/** A guest profile kept in the lodge's guest book. */
public class Guest {

    private String guestId;
    private String fullName;
    private String phone;
    private String email;
    private String idPassport;
    private String nationality;
    private String notes;
    private LocalDateTime createdAt;

    public Guest() {
        this.createdAt = LocalDateTime.now();
    }

    public Guest(String fullName, String phone, String email, String idPassport, String nationality, String notes) {
        this();
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.idPassport = idPassport;
        this.nationality = nationality;
        this.notes = notes;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdPassport() {
        return idPassport;
    }

    public void setIdPassport(String idPassport) {
        this.idPassport = idPassport;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + (phone == null || phone.isEmpty() ? guestId : phone) + ")";
    }
}
