package com.lodge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 password hashing so plain-text passwords are never stored on disk.
 */
public final class PasswordUtil {

    private static final String SALT = "lodge-billing-lab-ict261";

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + (rawPassword == null ? "" : rawPassword)).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        return hashedPassword != null && hashedPassword.equals(hash(rawPassword));
    }
}
