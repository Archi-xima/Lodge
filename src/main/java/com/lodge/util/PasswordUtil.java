
package com.lodge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Password hashing utility for the Lodge Billing System.
 *
 * New hashes use a unique random salt per password.
 * Legacy hashes from the previous fixed-salt implementation
 * remain verifiable so existing accounts can continue to log in.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final String PREFIX = "sha256$";
    private static final int SALT_LENGTH = 16;

    // Used only to verify hashes created by the previous implementation.
    private static final String LEGACY_SALT = "lodge-billing-lab-ict261";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Prevent instantiation.
    }

    /**
     * Hashes a password using SHA-256 and a unique random salt.
     *
     * Format: sha256$<salt>$<hash>
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        byte[] passwordBytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        byte[] passwordHash = digest(salt, passwordBytes);

        return PREFIX
                + HexFormat.of().formatHex(salt)
                + "$"
                + HexFormat.of().formatHex(passwordHash);
    }

    /**
     * Verifies a password against a stored password hash.
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        try {
            // Verify the new salted hash format.
            if (storedHash.startsWith(PREFIX)) {
                String[] parts = storedHash.split("\\$", -1);

                if (parts.length != 3 || !"sha256".equals(parts[0])) {
                    return false;
                }

                byte[] salt = HexFormat.of().parseHex(parts[1]);
                byte[] expectedHash = HexFormat.of().parseHex(parts[2]);

                if (salt.length != SALT_LENGTH || expectedHash.length != 32) {
                    return false;
                }

                byte[] actualHash = digest(
                        salt, rawPassword.getBytes(StandardCharsets.UTF_8));

                return MessageDigest.isEqual(expectedHash, actualHash);
            }

            // Verify older 64-character SHA-256 hashes.
            if (storedHash.matches("[0-9a-fA-F]{64}")) {
                byte[] expectedHash = HexFormat.of().parseHex(storedHash);
                byte[] actualHash = legacyHash(rawPassword);

                return MessageDigest.isEqual(expectedHash, actualHash);
            }

            return false;

        } catch (IllegalArgumentException exception) {
            // Malformed hashes must never authenticate.
            return false;
        }
    }

    /**
     * Determines whether a stored password hash needs upgrading.
     */
    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.startsWith(PREFIX);
    }

    private static byte[] digest(byte[] salt, byte[] passwordBytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            messageDigest.update(salt);
            return messageDigest.digest(passwordBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", exception);
        }
    }

    /**
     * Reproduces the previous fixed-salt format for legacy verification only.
     */
    private static byte[] legacyHash(String rawPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);

            byte[] input = (LEGACY_SALT + rawPassword)
                    .getBytes(StandardCharsets.UTF_8);

            return messageDigest.digest(input);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", exception);
        }
    }
}
