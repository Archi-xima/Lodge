
package com.lodge.model;

import com.lodge.util.PasswordUtil;

import java.time.LocalDateTime;

/** Represents a lodge user with role-based permissions. */
public class User {

    private String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private String guestId;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public User(String username, String rawPassword, String fullName, Role role) {
        this();
        this.username = username;
        setPassword(rawPassword);
        this.fullName = fullName;
        this.role = role;
    }

    /** Checks a raw password against the stored password hash. */
    public boolean checkPassword(String rawPassword) {
        return PasswordUtil.matches(rawPassword, passwordHash);
    }

    /** Stores a salted password hash, never the raw password. */
    public void setPassword(String rawPassword) {
        this.passwordHash = PasswordUtil.hash(rawPassword);
    }

    /** Checks both account status and the requested permission. */
    public boolean can(Role.Permission permission) {
        return active && role != null && permission != null && role.can(permission);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Persistence compatibility: DataStore may need to restore an existing hash.
     * Do not pass a raw password to this method.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getRoleLabel() {
        return role == null ? "" : role.getLabel();
    }
}
