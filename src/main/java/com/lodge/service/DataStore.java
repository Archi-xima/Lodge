package com.lodge.service;

import com.lodge.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Typed access to the JSON data files; all file handling is delegated to
 * {@link PersistenceService}. Data lives in {@code ~/.lodge-billing/data} so it
 * survives application restarts.
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
    public static final String FILE_EXPENSES = "expenses.json";

    private static final List<String> ALL_FILES = List.of(
            FILE_USERS, FILE_ROOMS, FILE_GUESTS, FILE_BOOKINGS, FILE_BILLS, FILE_SERVICES, FILE_AUDIT);

    private final Path dataDir;
    private final PersistenceService persistence;

    private DataStore() {
        this(Paths.get(System.getProperty("user.home"), ".lodge-billing"));
        persistence.startAutoSave(PersistenceService.DEFAULT_AUTOSAVE_SECONDS);
    }

    DataStore(Path dataDir) {
        this.dataDir = dataDir;
        this.persistence = new PersistenceService(dataDir);
        this.persistence.initializeStorage(ALL_FILES);
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    /** Application folder ({@code ~/.lodge-billing}); JSON files are in its {@code data/} subfolder. */
    public Path getDataDir() {
        return dataDir;
    }

    public PersistenceService getPersistence() {
        return persistence;
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

    public <T> List<T> loadList(String fileName, Class<T> elementType) {
        return persistence.loadList(fileName, elementType);
    }

    /** Saves a list; failures are logged and swallowed so the UI keeps working (auto-save retries). */
    public <T> void saveList(String fileName, List<T> items, Class<T> elementType) {
        try {
            persistence.saveList(fileName, items, elementType);
        } catch (PersistenceException e) {
            System.err.println("Could not write " + fileName + ": " + e.getMessage());
    public synchronized <T> void saveList(String fileName, List<T> items, Class<T> elementType) {
        saveList(fileName, items, elementType, false);
    }

    private synchronized <T> void saveListStrict(String fileName, List<T> items, Class<T> elementType) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(dataDir, fileName + "-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                gson.toJson(items, TypeToken.getParameterized(List.class, elementType).getType(), writer);
            }
            try {
                Files.move(temporary, dataDir.resolve(fileName),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, dataDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save lodge data file: " + fileName, e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private <T> void saveListStrict(String fileName, List<T> items, Class<T> elementType) {
        try {
            persistence.saveList(fileName, items, elementType);
        } catch (PersistenceException e) {
            throw new UncheckedIOException("Could not write lodge data file: " + e.getPath(),
                    new IOException(e.getMessage(), e));
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

    public void saveBillsStrict(List<Bill> bills) {
        saveListStrict(FILE_BILLS, bills, Bill.class);
    }

    public List<Expense> loadExpenses() {
        Path file = dataDir.resolve(FILE_EXPENSES);
        if (!Files.exists(file)) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Expense> loaded = gson.fromJson(reader,
                    TypeToken.getParameterized(List.class, Expense.class).getType());
            if (loaded == null || loaded.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalStateException("The expense register is invalid.");
            }
            return loaded;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Could not read expenses. The saved file has been preserved.", e);
        }
    }

    public void saveExpensesStrict(List<Expense> expenses) {
        saveListStrict(FILE_EXPENSES, expenses, Expense.class);
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
