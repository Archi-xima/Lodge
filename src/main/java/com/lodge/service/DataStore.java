package com.lodge.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lodge.model.*;
import com.lodge.util.JsonAdapters;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JSON file persistence for every entity in the system.
 * Data lives in {@code ~/.lodge-billing} so it survives application restarts.
 */
public class DataStore {

    private static DataStore instance;

    public static final String FILE_USERS = "users.json";
    public static final String FILE_ROOMS = "rooms.json";
    public static final String FILE_GUESTS = "guests.json";
    public static final String FILE_BOOKINGS = "bookings.json";
    public static final String FILE_BILLS = "bills.json";
    public static final String FILE_SERVICES = "services.json";
    public static final String FILE_AUDIT = "audit.json";

    private final Gson gson;
    private final Path dataDir;

    private DataStore() {
        this(Paths.get(System.getProperty("user.home"), ".lodge-billing"));
    }

    DataStore(Path dataDir) {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(JsonAdapters.localDateType(), JsonAdapters.localDateSerializer())
                .registerTypeAdapter(JsonAdapters.localDateType(), JsonAdapters.localDateDeserializer())
                .registerTypeAdapter(JsonAdapters.localDateTimeType(), JsonAdapters.localDateTimeSerializer())
                .registerTypeAdapter(JsonAdapters.localDateTimeType(), JsonAdapters.localDateTimeDeserializer())
                .create();
        this.dataDir = dataDir;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create lodge data directory: " + dataDir, e);
        }
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public synchronized boolean isMigrationComplete(String migrationName) {
        return Files.exists(dataDir.resolve("." + migrationName + ".done"));
    }

    public synchronized void markMigrationComplete(String migrationName) {
        Path marker = dataDir.resolve("." + migrationName + ".done");
        try {
            Files.createFile(marker);
        } catch (java.nio.file.FileAlreadyExistsException ignored) {
            // Another startup already recorded the migration.
        } catch (IOException e) {
            throw new UncheckedIOException("Could not record data migration: " + migrationName, e);
        }
    }

    // ------------------------------------------------------------------ generic list IO

    @SuppressWarnings("unchecked")
    public synchronized <T> List<T> loadList(String fileName, Class<T> elementType) {
        Path file = dataDir.resolve(fileName);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Type listType = TypeToken.getParameterized(List.class, elementType).getType();
            List<T> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not read " + fileName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized <T> void saveList(String fileName, List<T> items, Class<T> elementType) {
        saveList(fileName, items, elementType, false);
    }

    private synchronized <T> void saveListStrict(String fileName, List<T> items, Class<T> elementType) {
        saveList(fileName, items, elementType, true);
    }

    private <T> void saveList(String fileName, List<T> items, Class<T> elementType, boolean failOnError) {
        Path file = dataDir.resolve(fileName);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            Type listType = TypeToken.getParameterized(List.class, elementType).getType();
            gson.toJson(items, listType, writer);
        } catch (IOException e) {
            if (failOnError) {
                throw new UncheckedIOException("Could not write lodge data file: " + file, e);
            }
            System.err.println("Could not write " + fileName + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ convenience loaders

    public List<User> loadUsers() {
        List<User> users = loadList(FILE_USERS, User.class);
        return users.isEmpty() ? new ArrayList<>() : users;
    }

    public void saveUsers(List<User> users) {
        saveList(FILE_USERS, users, User.class);
    }

    public void saveUsersStrict(List<User> users) {
        saveListStrict(FILE_USERS, users, User.class);
    }

    public List<Room> loadRooms() {
        return loadList(FILE_ROOMS, Room.class);
    }

    public void saveRooms(List<Room> rooms) {
        saveList(FILE_ROOMS, rooms, Room.class);
    }

    public List<Guest> loadGuests() {
        return loadList(FILE_GUESTS, Guest.class);
    }

    public void saveGuests(List<Guest> guests) {
        saveList(FILE_GUESTS, guests, Guest.class);
    }

    public void saveGuestsStrict(List<Guest> guests) {
        saveListStrict(FILE_GUESTS, guests, Guest.class);
    }

    public List<Booking> loadBookings() {
        return loadList(FILE_BOOKINGS, Booking.class);
    }

    public void saveBookings(List<Booking> bookings) {
        saveList(FILE_BOOKINGS, bookings, Booking.class);
    }

    public List<Bill> loadBills() {
        return loadList(FILE_BILLS, Bill.class);
    }

    public void saveBills(List<Bill> bills) {
        saveList(FILE_BILLS, bills, Bill.class);
    }

    public List<ServiceItem> loadServices() {
        return loadList(FILE_SERVICES, ServiceItem.class);
    }

    public void saveServices(List<ServiceItem> services) {
        saveList(FILE_SERVICES, services, ServiceItem.class);
    }

    public List<AuditEntry> loadAudit() {
        return loadList(FILE_AUDIT, AuditEntry.class);
    }

    public void saveAudit(List<AuditEntry> entries) {
        saveList(FILE_AUDIT, entries, AuditEntry.class);
    }

    // ------------------------------------------------------------------ id generation

    /**
     * Produces the next sequential id such as {@code G004} by scanning the ids
     * already in use, so ids are never reused after a delete.
     */
    public synchronized String nextId(String prefix, List<String> existingIds) {
        int max = 0;
        for (String id : existingIds) {
            if (id != null && id.startsWith(prefix)) {
                String digits = id.substring(prefix.length()).replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    max = Math.max(max, Integer.parseInt(digits));
                }
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    /** Latest activity first, useful for audit trails. */
    public static Comparator<AuditEntry> newestFirst() {
        return Comparator.comparing(AuditEntry::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
