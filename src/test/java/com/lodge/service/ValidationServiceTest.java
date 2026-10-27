package com.lodge.service;

import com.lodge.model.RoomType;
import com.lodge.service.ValidationService.Strength;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ValidationServiceTest {

    // ------------------------------------------------------------ names

    @Test
    public void guestNameAcceptsRealNames() {
        assertTrue(ValidationService.validateGuestName("Mary Banda").valid());
        assertTrue(ValidationService.validateGuestName("  Chanda O'Brien-Phiri  ").valid());
        assertTrue(ValidationService.validateGuestName("José Müller").valid());
    }

    @Test
    public void guestNameRejectsBlankShortLongAndSymbols() {
        assertFalse(ValidationService.validateGuestName(null).valid());
        assertFalse(ValidationService.validateGuestName("   ").valid());
        assertFalse(ValidationService.validateGuestName("A").valid());
        assertFalse(ValidationService.validateGuestName("x".repeat(61)).valid());
        assertFalse(ValidationService.validateGuestName("John123").valid());
        assertFalse(ValidationService.validateGuestName("<script>").valid());
        assertTrue(ValidationService.validateGuestName("x".repeat(60)).valid());
    }

    @Test
    public void fullNameMessageMentionsFullName() {
        assertTrue(ValidationService.validateFullName("").message().startsWith("Full name"));
    }

    // ------------------------------------------------------------ nights

    @Test
    public void nightsBoundaries() {
        assertTrue(ValidationService.validateNights("1").valid());
        assertTrue(ValidationService.validateNights(" 30 ").valid());
        assertFalse(ValidationService.validateNights("0").valid());
        assertFalse(ValidationService.validateNights("-2").valid());
        assertFalse(ValidationService.validateNights("31").valid());
        assertFalse(ValidationService.validateNights("").valid());
        assertFalse(ValidationService.validateNights("2.5").valid());
        assertFalse(ValidationService.validateNights("three").valid());
    }

    @Test
    public void parseNightsThrowsValidationExceptionForBadInput() {
        assertEquals(4, ValidationService.parseNights(" 4 "));
        ValidationException e = assertThrows(ValidationException.class, () -> ValidationService.parseNights("x"));
        assertEquals("nights", e.getField());
    }

    // ------------------------------------------------------------ extras

    @Test
    public void extrasAreOptionalButMustBeSensibleMoney() {
        assertTrue(ValidationService.validateExtras("").valid());
        assertTrue(ValidationService.validateExtras(null).valid());
        assertTrue(ValidationService.validateExtras("200").valid());
        assertTrue(ValidationService.validateExtras("K 1,200.50").valid());
        assertFalse(ValidationService.validateExtras("abc").valid());
        assertFalse(ValidationService.validateExtras("-5").valid());
        assertFalse(ValidationService.validateExtras("10.999").valid());
        assertFalse(ValidationService.validateExtras("100000.01").valid());
        assertFalse(ValidationService.validateExtras("NaN").valid());
        assertFalse(ValidationService.validateExtras("Infinity").valid());
        assertEquals(0.0, ValidationService.parseExtras(""), 0.0);
        assertEquals(150.5, ValidationService.parseExtras("150.50"), 0.0001);
    }

    @Test
    public void quantityMustBeAWholeNumberFromOneToMax() {
        assertTrue(ValidationService.validateQuantity("1").valid());
        assertTrue(ValidationService.validateQuantity("100").valid());
        assertFalse(ValidationService.validateQuantity("0").valid());
        assertFalse(ValidationService.validateQuantity("101").valid());
        assertFalse(ValidationService.validateQuantity("").valid());
        assertFalse(ValidationService.validateQuantity("1.5").valid());
    }

    // ------------------------------------------------------------ amount paid

    @Test
    public void amountPaidChecksPositiveFormatAndBalance() {
        assertTrue(ValidationService.validateAmountPaid("500", 1000).valid());
        assertTrue(ValidationService.validateAmountPaid("1000", 1000).valid());
        assertTrue(ValidationService.validateAmountPaid("1000.00", 999.995).valid());
        assertFalse(ValidationService.validateAmountPaid("", 1000).valid());
        assertFalse(ValidationService.validateAmountPaid("0", 1000).valid());
        assertFalse(ValidationService.validateAmountPaid("-10", 1000).valid());
        assertFalse(ValidationService.validateAmountPaid("12.345", 1000).valid());
        assertFalse(ValidationService.validateAmountPaid("abc", 1000).valid());
        ValidationService.Result over = ValidationService.validateAmountPaid("1200", 1000);
        assertFalse(over.valid());
        assertTrue(over.message().contains("balance"));
    }

    @Test
    public void amountPaidWithoutBalanceStillHasAnUpperLimit() {
        assertTrue(ValidationService.validateAmountPaid("999999").valid());
        assertFalse(ValidationService.validateAmountPaid("1000001").valid());
    }

    // ------------------------------------------------------------ room type

    @Test
    public void roomTypeMustBeChosenAndKnown() {
        assertTrue(ValidationService.validateRoomType(RoomType.DELUXE).valid());
        assertFalse(ValidationService.validateRoomType((RoomType) null).valid());
        assertTrue(ValidationService.validateRoomType("deluxe room").valid());
        assertFalse(ValidationService.validateRoomType("").valid());
        assertFalse(ValidationService.validateRoomType("Penthouse").valid());
    }

    // ------------------------------------------------------------ notes / phone / email

    @Test
    public void notesAreOptionalWithA500CharacterLimit() {
        assertTrue(ValidationService.validateNotes(null).valid());
        assertTrue(ValidationService.validateNotes("x".repeat(500)).valid());
        ValidationService.Result tooLong = ValidationService.validateNotes("x".repeat(503));
        assertFalse(tooLong.valid());
        assertTrue(tooLong.message().contains("3 too many"));
    }

    @Test
    public void phoneIsOptionalButMustLookLikeAPhoneNumber() {
        assertTrue(ValidationService.validatePhone("").valid());
        assertTrue(ValidationService.validatePhone("+260 966 222 111").valid());
        assertFalse(ValidationService.validatePhone("12345").valid());
        assertFalse(ValidationService.validatePhone("call me").valid());
    }

    @Test
    public void emailIsRequiredAndMustMatchThePattern() {
        assertTrue(ValidationService.validateEmail("mary@example.com").valid());
        assertFalse(ValidationService.validateEmail("").valid());
        assertFalse(ValidationService.validateEmail("mary@example").valid());
        assertFalse(ValidationService.validateEmail("mary example@x.com").valid());
    }

    // ------------------------------------------------------------ username

    @Test
    public void usernameFormat() {
        assertTrue(ValidationService.validateUsername("achizima").valid());
        assertTrue(ValidationService.validateUsername("mary.banda_1").valid());
        assertFalse(ValidationService.validateUsername("").valid());
        assertFalse(ValidationService.validateUsername("ab").valid());
        assertFalse(ValidationService.validateUsername("a".repeat(21)).valid());
        assertFalse(ValidationService.validateUsername("1mary").valid());
        assertFalse(ValidationService.validateUsername("mary banda").valid());
    }

    @Test
    public void usernameAvailabilityIsCaseInsensitiveWhenThePredicateIs() {
        Set<String> existing = Set.of("achizima", "mary");
        java.util.function.Predicate<String> taken = u -> existing.contains(u.toLowerCase());
        assertFalse(ValidationService.validateUsernameAvailable("Achizima", taken).valid());
        assertTrue(ValidationService.validateUsernameAvailable("newuser", taken).valid());
        // format errors win over availability
        assertTrue(ValidationService.validateUsernameAvailable("1x", taken).message().contains("at least"));
        // no predicate → format check only
        assertTrue(ValidationService.validateUsernameAvailable("mary", null).valid());
    }

    // ------------------------------------------------------------ password

    @Test
    public void passwordRules() {
        assertTrue(ValidationService.validatePassword("Sky-Ridge-1").valid());
        assertTrue(ValidationService.validatePassword("correct horse battery").valid());
        assertFalse(ValidationService.validatePassword(null).valid());
        assertFalse(ValidationService.validatePassword("").valid());
        assertFalse(ValidationService.validatePassword("short1").valid());
        assertFalse(ValidationService.validatePassword("x".repeat(65)).valid());
        assertFalse(ValidationService.validatePassword(" leading-space1").valid());
        assertTrue(ValidationService.validatePassword("x".repeat(64)).valid());
    }

    @Test
    public void passwordConfirmationMustMatch() {
        assertTrue(ValidationService.validatePasswordConfirmation("abc12345", "abc12345").valid());
        assertFalse(ValidationService.validatePasswordConfirmation("abc12345", "abc12346").valid());
        assertFalse(ValidationService.validatePasswordConfirmation("abc12345", "").valid());
    }

    @Test
    public void passwordStrengthLevels() {
        assertEquals(Strength.EMPTY, ValidationService.evaluatePasswordStrength("").strength());
        assertEquals(Strength.WEAK, ValidationService.evaluatePasswordStrength("Ab1!").strength());
        assertEquals(Strength.WEAK, ValidationService.evaluatePasswordStrength("abcdefgh").strength());
        assertEquals(Strength.FAIR, ValidationService.evaluatePasswordStrength("abcdefG1").strength());
        assertEquals(Strength.GOOD, ValidationService.evaluatePasswordStrength("abcdeFG1!").strength());
        assertEquals(Strength.STRONG, ValidationService.evaluatePasswordStrength("Abcdefgh1!xyz").strength());
        assertTrue(ValidationService.evaluatePasswordStrength("Ab1!").hint().contains("4 more"));
        assertEquals("", ValidationService.evaluatePasswordStrength("Abcdefgh1!xyz").hint());
    }

    @Test
    public void strengthNeverContradictsTheMinimumLength() {
        // 7 chars with every character class is still weak
        assertEquals(Strength.WEAK, ValidationService.evaluatePasswordStrength("Abc12!x").strength());
    }

    // ------------------------------------------------------------ whole forms

    @Test
    public void walkInInvoiceCollectsEveryError() {
        List<ValidationException.FieldError> errors = ValidationService.validateWalkInInvoice(
                "", null, "0", "abc", "x".repeat(501));
        assertEquals(5, errors.size());
        assertEquals("guestName", errors.get(0).field());
        assertEquals("notes", errors.get(4).field());

        ValidationException e = assertThrows(ValidationException.class, () -> ValidationService.requireValid(errors));
        assertEquals(5, e.getErrors().size());
        assertNotNull(e.getMessageFor("nights"));
        assertNull(e.getMessageFor("phone"));
    }

    @Test
    public void validWalkInInvoicePasses() {
        List<ValidationException.FieldError> errors = ValidationService.validateWalkInInvoice(
                "Mary Banda", RoomType.STANDARD, "3", "", "");
        assertTrue(errors.isEmpty());
        ValidationService.requireValid(errors); // does not throw
    }

    @Test
    public void registrationChecksNameUsernamePasswordAndConfirmation() {
        List<ValidationException.FieldError> errors = ValidationService.validateRegistration(
                "Mary Banda", "mary", "Sky-Ridge-1", "Sky-Ridge-2", u -> u.equals("mary"));
        assertEquals(2, errors.size());
        assertEquals("username", errors.get(0).field());
        assertEquals("confirmPassword", errors.get(1).field());

        assertTrue(ValidationService.validateRegistration(
                "Mary Banda", "mary2", "Sky-Ridge-1", null, u -> false).isEmpty());
    }

    @Test
    public void loginOnlyChecksThatSomethingWasTyped() {
        assertEquals(2, ValidationService.validateLogin("", " ").size());
        assertTrue(ValidationService.validateLogin("a", "b").isEmpty());
    }

    @Test
    public void resultOrThrowCarriesTheFieldName() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> ValidationService.validateNights("0").orThrow("nights"));
        assertEquals("nights", e.getField());
        assertEquals(e.getMessage(), e.getFirstError().message());
    }
}
