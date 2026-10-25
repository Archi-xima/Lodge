package com.lodge.service;

import com.lodge.model.AuditEntry;
import com.lodge.model.Guest;
import com.lodge.model.Role;
import com.lodge.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Authentication, user management and the system-wide audit trail.
 */
public class AuthService {

    private static AuthService instance;
    private static final Set<String> LEGACY_DEMO_USERNAMES =
            Set.of("admin", "manager", "reception", "accounts", "student");

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<AuditEntry> auditTrail = FXCollections.observableArrayList();
    private final DataStore dataStore;
    private final GuestService guestService;
    private User currentUser;

    private AuthService() {
        this(DataStore.getInstance(), GuestService.getInstance());
    }

    AuthService(DataStore dataStore) {
        this(dataStore, new GuestService(dataStore));
    }

    AuthService(DataStore dataStore, GuestService guestService) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.guestService = Objects.requireNonNull(guestService);
        users.setAll(dataStore.loadUsers());
        auditTrail.setAll(dataStore.loadAudit());
        String demoCleanupMigration = "legacy-demo-account-cleanup-v1";
        if (!dataStore.isMigrationComplete(demoCleanupMigration)) {
            if (users.removeIf(user -> user.getUsername() != null
                    && LEGACY_DEMO_USERNAMES.contains(user.getUsername().trim().toLowerCase(Locale.ROOT)))) {
                saveUsers();
            }
            dataStore.markMigrationComplete(demoCleanupMigration);
        }
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        for (User user : users) {
            if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(username.trim())
                    && user.checkPassword(password.trim()) && user.isActive()) {
                currentUser = user;
                try {
                    log("LOGIN", user.getUsername() + " signed in");
                } catch (RuntimeException e) {
                    currentUser = null;
                    throw e;
                }
                return true;
            }
        }
        return false;
    }

    public void logout() {
        if (currentUser != null) {
            log("LOGOUT", currentUser.getUsername() + " signed out");
        }
        currentUser = null;
    }

    public boolean register(String username, String rawPassword, String fullName, Role role) {
        if (!hasPermission(Role.Permission.MANAGE_USERS) || role == Role.CUSTOMER) {
            return false;
        }
        if (username == null || username.trim().isEmpty()
                || rawPassword == null || rawPassword.trim().length() < 8
                || fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        String key = username.trim();
        if (findByUsername(key) != null) {
            return false;
        }
        User newUser = new User(key, rawPassword.trim(), fullName.trim(),
                role == null ? Role.RECEPTIONIST : role);
        users.add(newUser);
        try {
            saveUsers();
        } catch (RuntimeException e) {
            users.remove(newUser);
            throw e;
        }
        log("REGISTER", "Created account " + key + " (" + (role == null ? Role.RECEPTIONIST.getLabel() : role.getLabel()) + ")");
        return true;
    }

    public boolean registerCustomer(String username, String rawPassword, String fullName,
                                    String email, String phone) {
        if (username == null || username.trim().isEmpty()
                || rawPassword == null || rawPassword.trim().length() < 8
                || fullName == null || fullName.trim().isEmpty()
                || email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return false;
        }
        String key = username.trim();
        if (findByUsername(key) != null) {
            return false;
        }

        Guest guest = guestService.createCustomerProfile(
                fullName.trim(), email.trim(), phone == null ? "" : phone.trim());
        User customer = new User(key, rawPassword.trim(), fullName.trim(), Role.CUSTOMER);
        customer.setGuestId(guest.getGuestId());
        users.add(customer);
        try {
            saveUsers();
        } catch (RuntimeException e) {
            users.remove(customer);
            try {
                guestService.removeCustomerProfile(guest);
            } catch (RuntimeException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        }
        log("CUSTOMER REGISTER", "Created customer account " + key);
        return true;
    }

    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    public boolean removeUser(User user) {
        if (!hasPermission(Role.Permission.MANAGE_USERS) || user == null || user == currentUser
                || "achizima".equalsIgnoreCase(user.getUsername())) {
            return false;
        }
        boolean removed = users.remove(user);
        if (removed) {
            try {
                saveUsers();
            } catch (RuntimeException e) {
                users.add(user);
                throw e;
            }
            log("DELETE USER", "Removed account " + user.getUsername());
        }
        return removed;
    }

    public void saveUsers() {
        List<User> copy = new ArrayList<>(users);
        dataStore.saveUsersStrict(copy);
    }

    // ------------------------------------------------------------------ audit trail

    /** Writes an entry attributed to whoever is signed in. */
    public void log(String action, String detail) {
        String who = currentUser != null ? currentUser.getUsername() : "system";
        auditTrail.add(0, new AuditEntry(who, action, detail));
        if (auditTrail.size() > 500) {
            auditTrail.remove(auditTrail.size() - 1);
        }
        dataStore.saveAudit(new ArrayList<>(auditTrail));
    }

    public ObservableList<AuditEntry> getAuditTrail() {
        return auditTrail;
    }

    // ------------------------------------------------------------------ session helpers

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String currentUserName() {
        return currentUser != null ? currentUser.getFullName() : "Staff";
    }

    public boolean hasPermission(Role.Permission permission) {
        return currentUser != null && currentUser.can(permission);
    }

    public boolean needsInitialAdministratorSetup() {
        return users.stream().noneMatch(user -> user.isActive() && user.getRole() == Role.ADMIN);
    }

    public boolean createInitialAdministrator(String username, String fullName, String password) {
        if (!needsInitialAdministratorSetup()
                || username == null || username.trim().isEmpty()
                || fullName == null || fullName.trim().isEmpty()
                || password == null || password.length() < 8) {
            return false;
        }
        User existing = findByUsername(username.trim());
        String previousFullName = existing == null ? null : existing.getFullName();
        Role previousRole = existing == null ? null : existing.getRole();
        boolean previousActive = existing != null && existing.isActive();
        String previousPasswordHash = existing == null ? null : existing.getPasswordHash();
        User administrator = existing != null
                ? existing
                : new User(username.trim(), password, fullName.trim(), Role.ADMIN);
        if (existing != null) {
            existing.setFullName(fullName.trim());
            existing.setRole(Role.ADMIN);
            existing.setActive(true);
            existing.setPassword(password);
        } else {
            users.add(administrator);
        }
        try {
            saveUsers();
        } catch (RuntimeException e) {
            if (existing != null) {
                existing.setFullName(previousFullName);
                existing.setRole(previousRole);
                existing.setActive(previousActive);
                existing.setPasswordHash(previousPasswordHash);
            } else {
                users.remove(administrator);
            }
            throw e;
        }
        return true;
    }

    public ObservableList<User> getUsers() {
        return users;
    }
}
