package com.lodge.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordUtilTest {

    @Test
    public void hashNeverStoresTheRawPassword() {
        String raw = "Strong-Test-Password";
        String hash = PasswordUtil.hash(raw);

        assertNotNull(hash);
        assertEquals(64, hash.length());          // SHA-256 → 32 bytes → 64 hex chars
        assertNotEquals(raw, hash);
        assertFalse(hash.contains(raw));
    }

    @Test
    public void theSamePasswordAlwaysProducesTheSameHash() {
        assertEquals(PasswordUtil.hash("SkyRidge-123"), PasswordUtil.hash("SkyRidge-123"));
    }

    @Test
    public void differentPasswordsProduceDifferentHashes() {
        assertNotEquals(PasswordUtil.hash("first-password"), PasswordUtil.hash("second-password"));
    }

    @Test
    public void matchesAcceptsTheCorrectPasswordAndRejectsEverythingElse() {
        String stored = PasswordUtil.hash("Correct-Horse-Password");

        assertTrue(PasswordUtil.matches("Correct-Horse-Password", stored));
        assertFalse(PasswordUtil.matches("wrong-password", stored));
        assertFalse(PasswordUtil.matches(null, stored));
        assertFalse(PasswordUtil.matches("Correct-Horse-Password", null));
        assertTrue(PasswordUtil.matches(null, PasswordUtil.hash(null)));
    }
}
