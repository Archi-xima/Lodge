
package com.lodge.service;

import com.lodge.model.AuditEntry;
import com.lodge.model.Guest;
import com.lodge.model.Role;
import com.lodge.model.User;
import com.lodge.util.PasswordUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Authentication, user management, sessions and the system-wide audit trail.
 */
public class AuthService {

    private static AuthService instance;

    private static final long SESSION_TIMEOUT_MILLIS = 15L * 60L * 1000L;

    private static final Set<String> LEGACY_DEMO_USERNAMES =
            Set.of("admin", "manager", "reception", "accounts", "student");

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<AuditEntry> auditTrail =
            FXCollections.observableArrayList();

    private final DataStore dataStore;
    private final GuestService guestService;

    private User currentUser;
    private long lastActivityTime;

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

        String migration = "legacy-demo-account-cleanup-v1";

        if (!dataStore.isMigrationComplete(migration)) {
            boolean removed = users.removeIf(user ->
                    user.getUsername() != null
                            && LEGACY_DEMO_USERNAMES.contains(
                            user.getUsername().trim().toLowerCase(Locale.ROOT)));

            if (removed) {
                saveUsers();
            }

            dataStore.markMigrationComplete(migration);
        }

        dataStore.getPersistence().registerAutoSave("users", this::saveUsers);
        dataStore.getPersistence().registerAutoSave(
                "audit", () -> dataStore.saveAudit(new ArrayList<>(auditTrail)));
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    // ---------------------------------------------------------------
    // Authentication and sessions
    // ---------------------------------------------------------------

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String key = username.trim();

        for (User user : users) {
            if (user.getUsername() == null
                    || !user.getUsername().equalsIgnoreCase(key)
                    || !user.isActive()
                    || !user.checkPassword(password)) {
                continue;
            }

            // Upgrade old fixed-salt hashes after successful authentication.
            if (PasswordUtil.needsRehash(user.getPasswordHash())) {
                String oldHash = user.getPasswordHash();
                user.setPassword(password);

                try {
                    saveUsers();
                } catch (RuntimeException e) {
                    user.setPasswordHash(oldHash);
                    throw e;
                }
            }

            currentUser = user;
            lastActivityTime = System.currentTimeMillis();

            try {
                log("LOGIN", user.getUsername() + " signed in");
            } catch (RuntimeException e) {
                currentUser = null;
                lastActivityTime = 0L;
                throw e;
            }

            return true;
        }

        return false;
    }

    /**
     * Ends the session and clears session timing information.
     */
    public void logout() {
        User previousUser = currentUser;

        // Clear the session even if audit persistence later fails.
        currentUser = null;
        lastActivityTime = 0L;

        if (previousUser != null) {
            try {
                auditTrail.add(0, new AuditEntry(
                        previousUser.getUsername(), "LOGOUT",
                        previousUser.getUsername() + " signed out"));

                if (auditTrail.size() > 500) {
                    auditTrail.remove(auditTrail.size() - 1);
                }

                dataStore.saveAudit(new ArrayList<>(auditTrail));
            } catch (RuntimeException e) {
                // The session is already cleared. Propagate the persistence error.
                throw e;
            }
        }
    }

    /**
     * Call this when the signed-in user performs an actual UI action.
     */
    public void recordActivity() {
        if (ensureActiveSession()) {
            lastActivityTime = System.currentTimeMillis();
        }
    }

    /**
     * Expires the session after 15 minutes without recorded activity.
     */
    private boolean ensureActiveSession() {
        if (currentUser == null) {
            return false;
        }

        long inactiveFor = System.currentTimeMillis() - lastActivityTime;

        if (!currentUser.isActive()
                || inactiveFor >= SESSION_TIMEOUT_MILLIS) {
            logout();
            return false;
        }

        return true;
    }

    public boolean isLoggedIn() {
        return ensureActiveSession();
    }

    public User getCurrentUser() {
        return ensureActiveSession() ? currentUser : null;
    }

    public String currentUserName() {
        User user = getCurrentUser();
        return user != null ? user.getFullName() : "Staff";
    }

    public boolean hasPermission(Role.Permission permission) {
        if (!ensureActiveSession() || permission == null) {
            return false;
        }

        lastActivityTime = System.currentTimeMillis();
        return currentUser.can(permission);
    }

    // ---------------------------------------------------------------
    // Password changes
    // ---------------------------------------------------------------

    /**
     * Changes the password of the signed-in user after verifying the old one.
     * New passwords must contain at least eight characters.
     */
    public boolean changePassword(String currentPassword, String newPassword) {
        if (!ensureActiveSession()
                || currentPassword == null
                || newPassword == null
                || newPassword.length() < 8
                || !currentUser.checkPassword(currentPassword)
                || currentPassword.equals(newPassword)) {
            return false;
        }

        String previousHash = currentUser.getPasswordHash();
        currentUser.setPassword(newPassword);

        try {
            saveUsers();
        } catch (RuntimeException e) {
            currentUser.setPasswordHash(previousHash);
            throw e;
        }

        lastActivityTime = System.currentTimeMillis();
        log("PASSWORD CHANGE", "Password changed for " + currentUser.getUsername());
        return true;
    }

    // ---------------------------------------------------------------
    // Staff registration and customer self-registration
    // ---------------------------------------------------------------

    public boolean register(
            String username, String rawPassword, String fullName, Role role) {

        if (!hasPermission(Role.Permission.MANAGE_USERS)) {
            return false;
        }

        // Public staff registration must never create customer accounts.
        if (role == Role.CUSTOMER) {
            return false;
        }

        if (username == null || username.trim().isEmpty()
                || rawPassword == null || rawPassword.length() < 8
                || fullName == null || fullName.trim().isEmpty()) {
            return false;
        }

        String key = username.trim();

        if (findByUsername(key) != null) {
            return false;
        }

        Role assignedRole = role == null ? Role.RECEPTIONIST : role;

        User newUser = new User(
                key, rawPassword, fullName.trim(), assignedRole);

        users.add(newUser);

        try {
            saveUsers();
        } catch (RuntimeException e) {
            users.remove(newUser);
            throw e;
        }

        log("REGISTER", "Created account " + key
                + " (" + assignedRole.getLabel() + ")");

        return true;
    }

    /**
     * Public customer registration always assigns Role.CUSTOMER.
     * Customer accounts do not receive staff permissions.
     */
    public boolean registerCustomer(
            String username,
            String rawPassword,
            String fullName,
            String email,
            String phone) {

        if (username == null || username.trim().isEmpty()
                || rawPassword == null || rawPassword.length() < 8
                || fullName == null || fullName.trim().isEmpty()
                || email == null
                || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return false;
        }

        String key = username.trim();

        if (findByUsername(key) != null) {
            return false;
        }

        Guest guest = guestService.createCustomerProfile(
                fullName.trim(),
                email.trim(),
                phone == null ? "" : phone.trim());

        User customer = new User(
                key, rawPassword, fullName.trim(), Role.CUSTOMER);

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
        if (username == null) {
            return null;
        }

        for (User user : users) {
            if (user.getUsername() != null
                    && user.getUsername().equalsIgnoreCase(username.trim())) {
                return user;
            }
        }

        return null;
    }

    public boolean removeUser(User user) {
        if (!hasPermission(Role.Permission.MANAGE_USERS)
                || user == null
                || user == currentUser
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

    // ---------------------------------------------------------------
    // Audit trail
    // ---------------------------------------------------------------

    /** Writes an audit entry attributed to the currently signed-in user. */
    public void log(String action, String detail) {
        String who = currentUser != null
                ? currentUser.getUsername()
                : "system";

        auditTrail.add(0, new AuditEntry(who, action, detail));

        if (auditTrail.size() > 500) {
            auditTrail.remove(auditTrail.size() - 1);
        }

        dataStore.saveAudit(new ArrayList<>(auditTrail));
    }

    public ObservableList<AuditEntry> getAuditTrail() {
        return auditTrail;
    }

    // ---------------------------------------------------------------
    // Initial administrator setup
    // ---------------------------------------------------------------

    public boolean needsInitialAdministratorSetup() {
        return users.stream().noneMatch(
                user -> user.isActive() && user.getRole() == Role.ADMIN);
    }

    public boolean createInitialAdministrator(
            String username, String fullName, String password) {

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
        String previousPasswordHash =
                existing == null ? null : existing.getPasswordHash();

        User administrator = existing != null
                ? existing
                : new User(username.trim(), password,
                        fullName.trim(), Role.ADMIN);

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

        log("INITIAL ADMIN SETUP",
                "Initial administrator account created: " + username.trim());

        return true;
    }

    public ObservableList<User> getUsers() {
        return users;
    }
}
