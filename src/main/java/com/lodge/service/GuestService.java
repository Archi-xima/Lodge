package com.lodge.service;

import com.lodge.model.Guest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.ArrayList;
import java.util.List;

/** The guest book: profiles of everyone who has stayed at the lodge. */
public class GuestService {

    private static GuestService instance;

    private final ObservableList<Guest> guests = FXCollections.observableArrayList();
    private final DataStore dataStore;

    private GuestService() {
        this(DataStore.getInstance());
    }

    GuestService(DataStore dataStore) {
        this.dataStore = dataStore;
        guests.setAll(dataStore.loadGuests());
        if (guests.isEmpty()) {
            seedGuests();
        }
        dataStore.getPersistence().registerAutoSave("guests", this::save);
    }

    public static synchronized GuestService getInstance() {
        if (instance == null) {
            instance = new GuestService();
        }
        return instance;
    }

    private void seedGuests() {
        Guest g1 = new Guest("John Phiri", "+260 977 123 456", "john.phiri@example.com", "ZM1234567", "Zambia", "Prefers a quiet room.");
        g1.setGuestId("G001");
        Guest g2 = new Guest("Mary Banda", "+260 966 222 111", "mary.banda@example.com", "ZM7654321", "Zambia", "Allergic to feather pillows.");
        g2.setGuestId("G002");
        Guest g3 = new Guest("Peter Zulu", "+27 82 555 0199", "p.zulu@example.com", "SA9988776", "South Africa", "Diving enthusiast.");
        g3.setGuestId("G003");
        Guest g4 = new Guest("Grace Mutale", "+260 955 887 700", "grace.mutale@example.com", "ZM3344556", "Zambia", "Corporate account — invoices to Mutale Ltd.");
        g4.setGuestId("G004");
        guests.addAll(g1, g2, g3, g4);
        save();
    }

    public ObservableList<Guest> getGuests() {
        return guests;
    }

    public FilteredList<Guest> search(String text) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        return new FilteredList<>(guests, guest -> needle.isEmpty()
                || (guest.getFullName() != null && guest.getFullName().toLowerCase().contains(needle))
                || (guest.getPhone() != null && guest.getPhone().toLowerCase().contains(needle))
                || (guest.getEmail() != null && guest.getEmail().toLowerCase().contains(needle)));
    }

    public Guest findById(String guestId) {
        for (Guest guest : guests) {
            if (guestId != null && guestId.equals(guest.getGuestId())) {
                return guest;
            }
        }
        return null;
    }

    public Guest findByName(String name) {
        if (name == null) {
            return null;
        }
        for (Guest guest : guests) {
            if (name.equalsIgnoreCase(guest.getFullName())) {
                return guest;
            }
        }
        return null;
    }

    /** Finds an existing profile or creates a brand new one for a walk-in guest. */
    public Guest findOrCreate(String fullName, String phone) {
        Guest existing = findByName(fullName);
        if (existing != null) {
            return existing;
        }
        Guest guest = new Guest(fullName, phone, "", "", "", "Created at reception");
        saveGuest(guest);
        return guest;
    }

    public void saveGuest(Guest guest) {
        if (guest.getGuestId() == null || guest.getGuestId().isEmpty()) {
            guest.setGuestId(nextGuestId());
            guests.add(guest);
            AuthService.getInstance().log("ADD GUEST", guest.getGuestId() + " — " + guest.getFullName());
        } else if (!guests.contains(guest)) {
            guests.add(guest);
        }
        save();
    }

    Guest createCustomerProfile(String fullName, String email, String phone) {
        Guest guest = new Guest(fullName, phone, email, "", "", "Customer account");
        guest.setGuestId(nextGuestId());
        guests.add(guest);
        try {
            dataStore.saveGuestsStrict(new ArrayList<>(guests));
        } catch (RuntimeException e) {
            guests.remove(guest);
            throw e;
        }
        return guest;
    }

    void removeCustomerProfile(Guest guest) {
        if (guests.remove(guest)) {
            dataStore.saveGuestsStrict(new ArrayList<>(guests));
        }
    }

    public boolean removeGuest(Guest guest) {
        boolean removed = guests.remove(guest);
        if (removed) {
            save();
            AuthService.getInstance().log("DELETE GUEST", guest.getGuestId() + " — " + guest.getFullName());
        }
        return removed;
    }

    public String nextGuestId() {
        List<String> ids = new ArrayList<>();
        for (Guest guest : guests) {
            ids.add(guest.getGuestId());
        }
        return dataStore.nextId("G", ids);
    }

    public void save() {
        dataStore.saveGuests(new ArrayList<>(guests));
    }
}
