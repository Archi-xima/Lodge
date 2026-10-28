
package com.lodge.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordUtilTest {

    @Test
    public void hashNeverStoresTheRawPassword() {
        String raw = "Strong-Test-Password";
        String hash = PasswordUtil.hash(raw);

        assertNotNull(hash);
        assertTrue(hash.startsWith("sha256$"));
        assertNotEquals(raw, hash);
        assertFalse(hash.contains(raw));

        assertTrue(PasswordUtil.matches(raw, hash));
    }

    @Test
    public void theSamePasswordProducesDifferentHashesBecauseOfRandomSalts() {
        String password = "SkyRidge-123";

        String firstHash = PasswordUtil.hash(password);
        String secondHash = PasswordUtil.hash(password);

        assertNotEquals(firstHash, secondHash);

        // Both hashes must still verify the same password.
        assertTrue(PasswordUtil.matches(password, firstHash));
        assertTrue(PasswordUtil.matches(password, secondHash));
    }

    @Test
    public void differentPasswordsDoNotMatch() {
        String hash = PasswordUtil.hash("first-password");

        assertFalse(PasswordUtil.matches("second-password", hash));
    }

    @Test
    public void matchesAcceptsCorrectPasswordAndRejectsWrongPassword() {
        String password = "Correct-Horse-Password";
        String stored = PasswordUtil.hash(password);

        assertTrue(PasswordUtil.matches(password, stored));
        assertFalse(PasswordUtil.matches("wrong-password", stored));
    }

    @Test
    public void matchesRejectsNullInputs() {
        String stored = PasswordUtil.hash("Correct-Horse-Password");

        assertFalse(PasswordUtil.matches(null, stored));
        assertFalse(PasswordUtil.matches("Correct-Horse-Password", null));
        assertFalse(PasswordUtil.matches(null, null));
    }

    @Test
    public void malformedHashesAreRejected() {
        assertFalse(PasswordUtil.matches("some-password", ""));
        assertFalse(PasswordUtil.matches("some-password", "invalid-hash"));
        assertFalse(PasswordUtil.matches(
                "some-password",
                "sha256$not-valid-hex$1234"
        ));
    }

    @Test
    public void newHashesDoNotNeedRehashing() {
        String hash = PasswordUtil.hash("New-Password-123");

        assertFalse(PasswordUtil.needsRehash(hash));
    }

    @Test
    public void oldHashesAreMarkedForUpgrade() {
        String oldHash =
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        assertTrue(PasswordUtil.needsRehash(oldHash));
    }

    @Test
    public void nullPasswordCannotBeHashed() {
        try {
            PasswordUtil.hash(null);
            fail("Expected IllegalArgumentException for a null password.");
        } catch (IllegalArgumentException expected) {
            // Expected behaviour.
        }
    }
}
