package com.lodge.service;

import com.lodge.model.User;

import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static AuthService instance;
    private final Map<String, User> userDatabase = new HashMap<>();
    private User currentUser;

    private AuthService() {
        // Pre-seed default users
        userDatabase.put("admin", new User("admin", "admin123", "Mr. E. Nyirenda", "Administrator"));
        userDatabase.put("reception", new User("reception", "lodge123", "Front Desk Clerk", "Staff"));
        userDatabase.put("student", new User("student", "ict261", "ICT261 Student", "Student"));
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        User user = userDatabase.get(username.trim().toLowerCase());
        if (user != null && user.getPassword().equals(password.trim())) {
            currentUser = user;
            return true;
        }
        return false;
    }

    public boolean register(String username, String password, String fullName, String role) {
        if (username == null || username.trim().isEmpty()) return false;
        if (password == null || password.trim().isEmpty()) return false;
        String key = username.trim().toLowerCase();
        if (userDatabase.containsKey(key)) return false;

        User newUser = new User(key, password.trim(), fullName.trim(), role != null ? role : "Staff");
        userDatabase.put(key, newUser);
        return true;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
