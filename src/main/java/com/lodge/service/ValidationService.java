package com.lodge.service;

import com.lodge.model.RoomType;
import com.lodge.util.Constants;
import com.lodge.util.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Central validation rules for the lodge system (Member 4).
 *
 * <p>Pure Java — no JavaFX — so the same rules can be used by the login,
 * registration and bill-creation screens and unit-tested without a UI.
 * Every {@code validateXxx} method returns a {@link Result}; nothing here
 * throws for bad user input unless you ask for it with
 * {@link Result#orThrow(String)} / {@link #requireValid(List)}.</p>
 *
 * <p>Shared limits that already live in {@link Constants} (password length,
 * notes length, e-mail pattern, room rates) are read from there. The limits
 * that only validation needs are declared below, in one block, so a rule can
 * be changed in a single place.</p>
 */
public final class ValidationService {

    private ValidationService() {
        // static rules only
    }

    /* ==================================================================== */
    /* Limits                                                               */
    /* ==================================================================== */

    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 60;

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;

    public static final int MIN_NIGHTS = 1;
    public static final int MAX_NIGHTS = 30;

    public static final double MAX_EXTRAS_AMOUNT = 100_000.00;
    public static final double MAX_PAYMENT_AMOUNT = 1_000_000.00;

    public static final int MIN_PHONE_DIGITS = 7;
    public static final int MAX_PHONE_LENGTH = 20;

    public static final int MAX_EXTRA_QUANTITY = 100;

    /** Letters (any language), spaces, apostrophes, hyphens and full stops. */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}][\\p{L} .'’-]*$");
    /** Starts with a letter; then letters, digits, dot, underscore or hyphen. */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9 ()-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(Constants.EMAIL_REGEX);

    /* ==================================================================== */
    /* Result                                                               */
    /* ==================================================================== */

    /** Outcome of one rule check: either valid, or invalid with a user-facing message. */
    public record Result(boolean valid, String message) {

        private static final Result OK = new Result(true, "");

        public static Result ok() {
            return OK;
        }

        public static Result error(String message) {
            return new Result(false, message);
        }

        public boolean isInvalid() {
            return !valid;
        }

        /** Returns this result, or throws a {@link ValidationException} for {@code field}. */
        public Result orThrow(String field) {
            if (!valid) {
                throw new ValidationException(field, message);
            }
            return this;
        }
    }

    /* ==================================================================== */
    /* Names                                                                */
    /* ==================================================================== */

    /** Guest name on a walk-in invoice or in the guest book. */
    public static Result validateGuestName(String value) {
        return validateName(value, "Guest name");
    }

    /** Full name of a staff member or customer account. */
    public static Result validateFullName(String value) {
        return validateName(value, "Full name");
    }

    private static Result validateName(String value, String label) {
        String text = trim(value);
        if (text.isEmpty()) {
            return Result.error(label + " is required.");
        }
        if (text.length() < MIN_NAME_LENGTH) {
            return Result.error(label + " must be at least " + MIN_NAME_LENGTH + " characters.");
        }
        if (text.length() > MAX_NAME_LENGTH) {
            return Result.error(label + " must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (!NAME_PATTERN.matcher(text).matches()) {
            return Result.error(label + " may only contain letters, spaces, apostrophes, hyphens and full stops.");
        }
        return Result.ok();
    }

    /* ==================================================================== */
    /* Stay, extras and payments                                            */
    /* ==================================================================== */

    /** Number of nights typed into a text field: a whole number from {@value #MIN_NIGHTS} to {@value #MAX_NIGHTS}. */
    public static Result validateNights(String value) {
        String text = trim(value);
        if (text.isEmpty()) {
            return Result.error("Nights is required.");
        }
        int nights;
        try {
            nights = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return Result.error("Nights must be a whole number, e.g. 3.");
        }
        return validateNights(nights);
    }

    public static Result validateNights(int nights) {
        if (nights < MIN_NIGHTS) {
            return Result.error("Nights must be at least " + MIN_NIGHTS + ".");
        }
        if (nights > MAX_NIGHTS) {
            return Result.error("Nights cannot be more than " + MAX_NIGHTS + ".");
        }
        return Result.ok();
    }

    /** Parses a field already accepted by {@link #validateNights(String)}. */
    public static int parseNights(String value) {
        validateNights(value).orThrow("nights");
        return Integer.parseInt(trim(value));
    }

    /**
     * Extras amount on a walk-in invoice. The field is optional: blank means
     * "no extras" (K 0). A value, if present, must be a money amount from 0 to
     * {@link #MAX_EXTRAS_AMOUNT} with at most two decimals.
     */
    public static Result validateExtras(String value) {
        if (trim(value).isEmpty()) {
            return Result.ok();
        }
        Double amount = parseAmount(value);
        if (amount == null) {
            return Result.error("Extras must be a valid amount, e.g. 200 or 150.50.");
        }
        if (hasMoreThanTwoDecimals(value)) {
            return Result.error("Extras can have at most 2 decimal places.");
        }
        if (amount < 0) {
            return Result.error("Extras cannot be negative.");
        }
        if (amount > MAX_EXTRAS_AMOUNT) {
            return Result.error("Extras cannot be more than " + Money.format(MAX_EXTRAS_AMOUNT) + ".");
        }
        return Result.ok();
    }

    /** Extras amount as a number; blank gives 0. Throws if the text is invalid. */
    public static double parseExtras(String value) {
        validateExtras(value).orThrow("extras");
        return trim(value).isEmpty() ? 0 : Money.parse(value);
    }

    /** Quantity of an extra service added to a bill: a whole number from 1 to {@value #MAX_EXTRA_QUANTITY}. */
    public static Result validateQuantity(String value) {
        String text = trim(value);
        if (text.isEmpty()) {
            return Result.error("Quantity is required.");
        }
        int quantity;
        try {
            quantity = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return Result.error("Quantity must be a whole number.");
        }
        if (quantity < 1) {
            return Result.error("Quantity must be at least 1.");
        }
        if (quantity > MAX_EXTRA_QUANTITY) {
            return Result.error("Quantity cannot be more than " + MAX_EXTRA_QUANTITY + ".");
        }
        return Result.ok();
    }

    /** Payment amount with no balance check (use the two-argument overload when a bill is selected). */
    public static Result validateAmountPaid(String value) {
        return validateAmountPaid(value, Double.POSITIVE_INFINITY);
    }

    /**
     * Amount paid against a bill: a positive money amount, at most two
     * decimals, no more than {@code balance} (the amount still owing) and no
     * more than {@link #MAX_PAYMENT_AMOUNT}.
     */
    public static Result validateAmountPaid(String value, double balance) {
        if (trim(value).isEmpty()) {
            return Result.error("Amount paid is required.");
        }
        Double amount = parseAmount(value);
        if (amount == null) {
            return Result.error("Amount paid must be a valid amount, e.g. 1500.");
        }
        if (hasMoreThanTwoDecimals(value)) {
            return Result.error("Amount paid can have at most 2 decimal places.");
        }
        if (amount <= 0) {
            return Result.error("Amount paid must be greater than zero.");
        }
        if (amount > MAX_PAYMENT_AMOUNT) {
            return Result.error("Amount paid cannot be more than " + Money.format(MAX_PAYMENT_AMOUNT) + ".");
        }
        if (amount - balance > 0.009) {
            return Result.error("That is more than the balance of " + Money.format(balance) + ".");
        }
        return Result.ok();
    }

    /** Parses a field already accepted by {@link #validateAmountPaid(String, double)}. */
    public static double parseAmountPaid(String value, double balance) {
        validateAmountPaid(value, balance).orThrow("amountPaid");
        return Money.parse(value);
    }

    /* ==================================================================== */
    /* Room type                                                            */
    /* ==================================================================== */

    public static Result validateRoomType(RoomType roomType) {
        return roomType == null ? Result.error("Please choose a room type.") : Result.ok();
    }

    /** Accepts the display name shown in the UI (e.g. "Deluxe Room"); blank or unknown names are rejected. */
    public static Result validateRoomType(String displayName) {
        String text = trim(displayName);
        if (text.isEmpty()) {
            return Result.error("Please choose a room type.");
        }
        for (RoomType type : RoomType.values()) {
            if (type.getDisplayName().equalsIgnoreCase(text)) {
                return Result.ok();
            }
        }
        return Result.error("\"" + text + "\" is not a room type we offer.");
    }

    /* ==================================================================== */
    /* Notes, phone and e-mail                                              */
    /* ==================================================================== */

    /** Free-text notes are optional but limited to {@link Constants#MAX_NOTES_LENGTH} characters. */
    public static Result validateNotes(String value) {
        if (value != null && value.length() > Constants.MAX_NOTES_LENGTH) {
            return Result.error("Notes must be at most " + Constants.MAX_NOTES_LENGTH + " characters ("
                    + (value.length() - Constants.MAX_NOTES_LENGTH) + " too many).");
        }
        return Result.ok();
    }

    /** Optional phone number: digits with an optional leading + and common separators. */
    public static Result validatePhone(String value) {
        String text = trim(value);
        if (text.isEmpty()) {
            return Result.ok();
        }
        if (text.length() > MAX_PHONE_LENGTH || !PHONE_PATTERN.matcher(text).matches()) {
            return Result.error("Phone may only contain digits, spaces, brackets, hyphens and a leading +.");
        }
        long digits = text.chars().filter(Character::isDigit).count();
        if (digits < MIN_PHONE_DIGITS) {
            return Result.error("Phone must have at least " + MIN_PHONE_DIGITS + " digits.");
        }
        return Result.ok();
    }

    /** Required e-mail (customer registration). */
    public static Result validateEmail(String value) {
        String text = trim(value);
        if (text.isEmpty()) {
            return Result.error("Email is required.");
        }
        if (!EMAIL_PATTERN.matcher(text).matches()) {
            return Result.error("Enter a valid email address, e.g. name@example.com.");
        }
        return Result.ok();
    }

    /* ==================================================================== */
    /* Username and password                                                */
    /* ==================================================================== */

    /** Format rules only; use {@link #validateUsernameAvailable} to also check it is free. */
    public static Result validateUsername(String value) {
        String text = trim(value);
        if (text.isEmpty()) {
            return Result.error("Username is required.");
        }
        if (text.length() < MIN_USERNAME_LENGTH) {
            return Result.error("Username must be at least " + MIN_USERNAME_LENGTH + " characters.");
        }
        if (text.length() > MAX_USERNAME_LENGTH) {
            return Result.error("Username must be at most " + MAX_USERNAME_LENGTH + " characters.");
        }
        if (!USERNAME_PATTERN.matcher(text).matches()) {
            return Result.error("Username must start with a letter and use only letters, digits, dots, "
                    + "underscores or hyphens.");
        }
        return Result.ok();
    }

    /**
     * Format check plus availability.
     *
     * @param isTaken answers "does an account with this username already exist?"
     *                (for example {@code u -> AuthService.getInstance().findByUsername(u) != null});
     *                kept as a predicate so this class never depends on the data layer.
     */
    public static Result validateUsernameAvailable(String value, Predicate<String> isTaken) {
        Result format = validateUsername(value);
        if (format.isInvalid()) {
            return format;
        }
        if (isTaken != null && isTaken.test(trim(value))) {
            return Result.error("That username is already taken.");
        }
        return Result.ok();
    }

    /**
     * Password rules: {@link Constants#MIN_PASSWORD_LENGTH} to
     * {@link Constants#MAX_PASSWORD_LENGTH} characters, and no space at the
     * start or end (AuthService trims passwords, so those would be silently lost).
     */
    public static Result validatePassword(String value) {
        if (value == null || value.isEmpty()) {
            return Result.error("Password is required.");
        }
        if (value.length() < Constants.MIN_PASSWORD_LENGTH) {
            return Result.error("Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters.");
        }
        if (value.length() > Constants.MAX_PASSWORD_LENGTH) {
            return Result.error("Password must be at most " + Constants.MAX_PASSWORD_LENGTH + " characters.");
        }
        if (!value.equals(value.trim())) {
            return Result.error("Password cannot start or end with a space.");
        }
        return Result.ok();
    }

    public static Result validatePasswordConfirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return Result.error("Please confirm your password.");
        }
        if (!confirmation.equals(password)) {
            return Result.error("Passwords do not match.");
        }
        return Result.ok();
    }

    /* ==================================================================== */
    /* Password strength                                                    */
    /* ==================================================================== */

    public enum Strength {
        EMPTY("", 0.0),
        WEAK("Weak", 0.25),
        FAIR("Fair", 0.5),
        GOOD("Good", 0.75),
        STRONG("Strong", 1.0);

        private final String label;
        private final double fraction;

        Strength(String label, double fraction) {
            this.label = label;
            this.fraction = fraction;
        }

        public String getLabel() {
            return label;
        }

        /** 0.0–1.0, ready for a {@code ProgressBar}. */
        public double getFraction() {
            return fraction;
        }
    }

    /** Strength plus a hint on what to improve next. */
    public record PasswordStrength(Strength strength, String hint) {
    }

    /**
     * Scores a password on length and character variety (lower case, upper
     * case, digits, symbols). Anything below the minimum length is always
     * {@link Strength#WEAK}, so the meter never contradicts
     * {@link #validatePassword(String)}.
     */
    public static PasswordStrength evaluatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrength(Strength.EMPTY, "");
        }
        int length = password.length();
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean symbol = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        if (length < Constants.MIN_PASSWORD_LENGTH) {
            int missing = Constants.MIN_PASSWORD_LENGTH - length;
            return new PasswordStrength(Strength.WEAK,
                    "Add " + missing + " more character" + (missing == 1 ? "" : "s") + ".");
        }

        int classes = (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);
        int score = classes;
        if (length >= 12) {
            score++;
        }
        if (length >= 16) {
            score++;
        }

        Strength strength;
        if (score <= 2) {
            strength = Strength.WEAK;
        } else if (score == 3) {
            strength = Strength.FAIR;
        } else if (score == 4) {
            strength = Strength.GOOD;
        } else {
            strength = Strength.STRONG;
        }

        String hint;
        if (!upper || !lower) {
            hint = "Mix upper and lower case letters.";
        } else if (!digit) {
            hint = "Add a number.";
        } else if (!symbol) {
            hint = "Add a symbol such as ! or #.";
        } else if (length < 12) {
            hint = "Use 12 or more characters.";
        } else {
            hint = "";
        }
        if (strength == Strength.STRONG) {
            hint = "";
        }
        return new PasswordStrength(strength, hint);
    }

    /* ==================================================================== */
    /* Whole-form checks                                                    */
    /* ==================================================================== */

    /** Walk-in invoice form: guest name, room type, nights, extras and notes. */
    public static List<ValidationException.FieldError> validateWalkInInvoice(
            String guestName, RoomType roomType, String nights, String extras, String notes) {
        List<ValidationException.FieldError> errors = new ArrayList<>();
        collect(errors, "guestName", validateGuestName(guestName));
        collect(errors, "roomType", validateRoomType(roomType));
        collect(errors, "nights", validateNights(nights));
        collect(errors, "extras", validateExtras(extras));
        collect(errors, "notes", validateNotes(notes));
        return errors;
    }

    /** Staff/customer registration form. Pass a null {@code isTaken} to skip the availability check. */
    public static List<ValidationException.FieldError> validateRegistration(
            String fullName, String username, String password, String confirmation,
            Predicate<String> isTaken) {
        List<ValidationException.FieldError> errors = new ArrayList<>();
        collect(errors, "fullName", validateFullName(fullName));
        collect(errors, "username", validateUsernameAvailable(username, isTaken));
        collect(errors, "password", validatePassword(password));
        if (confirmation != null) {
            collect(errors, "confirmPassword", validatePasswordConfirmation(password, confirmation));
        }
        return errors;
    }

    /** Sign-in only checks that something was typed; it must not reveal which rule an account would break. */
    public static List<ValidationException.FieldError> validateLogin(String username, String password) {
        List<ValidationException.FieldError> errors = new ArrayList<>();
        if (trim(username).isEmpty()) {
            errors.add(new ValidationException.FieldError("username", "Enter your username."));
        }
        if (password == null || password.trim().isEmpty()) {
            errors.add(new ValidationException.FieldError("password", "Enter your password."));
        }
        return errors;
    }

    /** Throws one {@link ValidationException} carrying every error, or does nothing for an empty list. */
    public static void requireValid(List<ValidationException.FieldError> errors) {
        if (errors != null && !errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /* ==================================================================== */
    /* Internals                                                            */
    /* ==================================================================== */

    private static void collect(List<ValidationException.FieldError> errors, String field, Result result) {
        if (result.isInvalid()) {
            errors.add(new ValidationException.FieldError(field, result.message()));
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Parses like {@link Money#parse}, but returns {@code null} instead of throwing and rejects NaN/Infinity. */
    private static Double parseAmount(String value) {
        try {
            double amount = Money.parse(value);
            return Double.isNaN(amount) || Double.isInfinite(amount) ? null : amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean hasMoreThanTwoDecimals(String value) {
        try {
            String cleaned = value.replace(Money.SYMBOL, "").replace(",", "").trim();
            return new BigDecimal(cleaned).stripTrailingZeros().scale() > 2;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
