package com.lodge.util;

import com.lodge.service.ValidationException;
import com.lodge.service.ValidationService;
import com.lodge.service.ValidationService.PasswordStrength;
import com.lodge.service.ValidationService.Result;
import com.lodge.service.ValidationService.Strength;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * JavaFX side of validation (Member 4): turns the {@link Result}s produced by
 * {@link ValidationService} into what the user actually sees — a red border,
 * an error indicator and message under the field, placeholders, character
 * counters, a password-strength meter and a live "username taken" check.
 *
 * <p>The same helper serves the login, registration and bill-creation
 * screens. A typical controller:</p>
 *
 * <pre>{@code
 *   // in initialize()
 *   ValidationHelper.Field name = ValidationHelper.attach(
 *           walkInGuestField, walkInGuestError, "Full name, e.g. Mary Banda",
 *           ValidationService::validateGuestName);
 *   ValidationHelper.Field nights = ValidationHelper.attach(
 *           walkInNightsField, walkInNightsError, "1",
 *           ValidationService::validateNights);
 *   ValidationHelper.attachCounter(walkInNotesField, notesCounter, Constants.MAX_NOTES_LENGTH);
 *
 *   // in the button handler
 *   if (!ValidationHelper.validateAll(name, nights)) {
 *       return;   // first bad field is focused, every bad field is red
 *   }
 * }</pre>
 *
 * <p>Colours come from the shared palette (danger #EF4444, secondary #10B981,
 * secondary text #64748B). They are applied as inline styles so this works
 * even before the colour CSS from the theme work is merged; each control also
 * gets the {@value #STYLE_CLASS_ERROR} / {@value #STYLE_CLASS_VALID} style
 * class so the stylesheet can take over later.</p>
 */
public final class ValidationHelper {

    private ValidationHelper() {
        // static helpers only
    }

    public static final String STYLE_CLASS_ERROR = "input-error";
    public static final String STYLE_CLASS_VALID = "input-valid";

    public static final String COLOR_DANGER = "#EF4444";
    public static final String COLOR_SUCCESS = "#10B981";
    public static final String COLOR_ACCENT = "#F59E0B";
    public static final String COLOR_MUTED = "#64748B";

    /** Indicator shown in front of an error message. */
    public static final String ERROR_ICON = "⚠ ";
    /** Indicator shown in front of a success message. */
    public static final String SUCCESS_ICON = "✓ ";

    private static final String ERROR_BORDER_STYLE =
            "-fx-border-color: " + COLOR_DANGER + "; -fx-border-width: 1.5; -fx-border-radius: 8;";
    private static final String VALID_BORDER_STYLE =
            "-fx-border-color: " + COLOR_SUCCESS + "; -fx-border-width: 1.5; -fx-border-radius: 8;";
    private static final String BASE_STYLE_KEY = "lodge.validation.baseStyle";

    /* ==================================================================== */
    /* Red borders and error indicators                                     */
    /* ==================================================================== */

    /** Red border + error style class on the control, error indicator and message in the label. */
    public static void showError(Control control, Label messageLabel, String message) {
        applyBorder(control, ERROR_BORDER_STYLE, STYLE_CLASS_ERROR);
        if (messageLabel != null) {
            setMessage(messageLabel, ERROR_ICON + message, COLOR_DANGER);
        }
    }

    /** Green border + valid style class on the control and an optional success message. */
    public static void showValid(Control control, Label messageLabel, String message) {
        applyBorder(control, VALID_BORDER_STYLE, STYLE_CLASS_VALID);
        if (messageLabel != null) {
            if (message == null || message.isEmpty()) {
                clearMessage(messageLabel);
            } else {
                setMessage(messageLabel, SUCCESS_ICON + message, COLOR_SUCCESS);
            }
        }
    }

    /** Back to the neutral look: original style restored, message hidden. */
    public static void clear(Control control, Label messageLabel) {
        restoreStyle(control);
        control.getStyleClass().removeAll(STYLE_CLASS_ERROR, STYLE_CLASS_VALID);
        if (messageLabel != null) {
            clearMessage(messageLabel);
        }
    }

    /** Applies a {@link Result} to a field: red + message when invalid, neutral when valid. */
    public static boolean show(Control control, Label messageLabel, Result result) {
        if (result.isInvalid()) {
            showError(control, messageLabel, result.message());
            return false;
        }
        clear(control, messageLabel);
        return true;
    }

    /* ==================================================================== */
    /* Field binding (live validation)                                      */
    /* ==================================================================== */

    /**
     * One validated input. Keeps the control, its message label and the rule
     * together so a form can validate the whole set with {@link #validateAll}.
     */
    public static final class Field {
        private final Control control;
        private final Label messageLabel;
        private final Supplier<Result> rule;
        private boolean touched;

        private Field(Control control, Label messageLabel, Supplier<Result> rule) {
            this.control = control;
            this.messageLabel = messageLabel;
            this.rule = rule;
        }

        public Control getControl() {
            return control;
        }

        /** Runs the rule now and updates the screen. Returns {@code true} when the input is valid. */
        public boolean validate() {
            touched = true;
            return show(control, messageLabel, rule.get());
        }

        /** Runs the rule without touching the screen. */
        public boolean isValid() {
            return rule.get().valid();
        }

        /** Resets the field to neutral, e.g. after a successful submit. */
        public void reset() {
            touched = false;
            clear(control, messageLabel);
        }
    }

    /**
     * Binds a text field to a rule. The prompt text (placeholder) is set,
     * the field is validated when it loses focus and then live on every
     * keystroke, so errors appear after the user has had a chance to type
     * but disappear the moment the input is fixed.
     *
     * @param placeholder prompt text, or {@code null} to leave the existing one
     */
    public static Field attach(TextInputControl input, Label messageLabel, String placeholder,
                               Function<String, Result> rule) {
        return attachRule(input, messageLabel, placeholder, () -> rule.apply(input.getText()));
    }

    /**
     * Like {@link #attach} for rules that depend on other inputs, such as
     * password confirmation. (Separate name so method references to the
     * overloaded {@code ValidationService.validateXxx} methods stay unambiguous.)
     */
    public static Field attachDependent(TextInputControl input, Label messageLabel, String placeholder,
                                        Supplier<Result> rule) {
        return attachRule(input, messageLabel, placeholder, rule);
    }

    private static Field attachRule(TextInputControl input, Label messageLabel, String placeholder,
                                    Supplier<Result> rule) {
        if (placeholder != null) {
            input.setPromptText(placeholder);
        }
        Field field = new Field(input, messageLabel, rule);
        input.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow && (field.touched || !blank(input.getText()))) {
                field.validate();
            }
        });
        input.textProperty().addListener((obs, old, now) -> {
            if (field.touched) {
                field.validate();
            }
        });
        return field;
    }

    /**
     * Validates every field (so all bad ones turn red at once), focuses the
     * first invalid control and returns whether the whole form is valid.
     */
    public static boolean validateAll(Field... fields) {
        Field firstInvalid = null;
        for (Field field : fields) {
            if (!field.validate() && firstInvalid == null) {
                firstInvalid = field;
            }
        }
        if (firstInvalid != null) {
            firstInvalid.control.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Shows the errors of a {@link ValidationException} on the matching
     * fields, looked up by the field names used in {@link ValidationService}
     * ({@code "guestName"}, {@code "nights"}, {@code "username"} …).
     */
    public static void showErrors(ValidationException exception, Map<String, Field> fieldsByName) {
        for (ValidationException.FieldError error : exception.getErrors()) {
            Field field = fieldsByName.get(error.field());
            if (field != null) {
                field.touched = true;
                showError(field.control, field.messageLabel, error.message());
            }
        }
    }

    /** Convenience: validate the field errors from {@link ValidationService#validateRegistration} etc. */
    public static boolean showErrors(List<ValidationException.FieldError> errors, Map<String, Field> fieldsByName) {
        if (errors.isEmpty()) {
            return true;
        }
        showErrors(new ValidationException(new ArrayList<>(errors)), fieldsByName);
        return false;
    }

    /* ==================================================================== */
    /* Character counter                                                    */
    /* ==================================================================== */

    /**
     * Keeps {@code counterLabel} showing "12 / 500". Turns amber from 90% of
     * the limit and red once it is exceeded (typing is not blocked, so pasted
     * text is never silently cut — the validator reports the overflow).
     */
    public static void attachCounter(TextInputControl input, Label counterLabel, int maxLength) {
        Runnable update = () -> {
            int length = input.getText() == null ? 0 : input.getText().length();
            counterLabel.setText(length + " / " + maxLength);
            String color = length > maxLength ? COLOR_DANGER
                    : length >= maxLength * 0.9 ? COLOR_ACCENT : COLOR_MUTED;
            counterLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";"
                    + (length > maxLength ? " -fx-font-weight: bold;" : ""));
        };
        input.textProperty().addListener((obs, old, now) -> update.run());
        update.run();
    }

    /* ==================================================================== */
    /* Password strength                                                    */
    /* ==================================================================== */

    /**
     * Live strength meter: fills {@code meter} (may be {@code null}) and writes
     * "Strong" / "Add a number." style feedback into {@code feedbackLabel}.
     */
    public static void bindPasswordStrength(TextInputControl passwordField, ProgressBar meter, Label feedbackLabel) {
        Runnable update = () -> {
            PasswordStrength result = ValidationService.evaluatePasswordStrength(passwordField.getText());
            String color = strengthColor(result.strength());
            if (meter != null) {
                meter.setProgress(result.strength().getFraction());
                meter.setStyle("-fx-accent: " + color + ";");
                meter.setVisible(result.strength() != Strength.EMPTY);
            }
            if (feedbackLabel != null) {
                if (result.strength() == Strength.EMPTY) {
                    clearMessage(feedbackLabel);
                } else {
                    String hint = result.hint().isEmpty() ? "" : " — " + result.hint();
                    setMessage(feedbackLabel, "Password strength: " + result.strength().getLabel() + hint, color);
                }
            }
        };
        passwordField.textProperty().addListener((obs, old, now) -> update.run());
        update.run();
    }

    private static String strengthColor(Strength strength) {
        switch (strength) {
            case WEAK:
                return COLOR_DANGER;
            case FAIR:
                return COLOR_ACCENT;
            case GOOD:
            case STRONG:
                return COLOR_SUCCESS;
            case EMPTY:
            default:
                return COLOR_MUTED;
        }
    }

    /* ==================================================================== */
    /* Username availability                                                */
    /* ==================================================================== */

    /**
     * Live "is this username free?" feedback while the user types. Shows the
     * format error first, then "Username is taken" in red or "Username is
     * available" in green. Returns a {@link Field} so the form can include it
     * in {@link #validateAll}.
     *
     * @param isTaken e.g. {@code u -> AuthService.getInstance().findByUsername(u) != null}
     */
    public static Field bindUsernameAvailability(TextInputControl usernameField, Label messageLabel,
                                                 Predicate<String> isTaken) {
        Field field = new Field(usernameField, messageLabel,
                () -> ValidationService.validateUsernameAvailable(usernameField.getText(), isTaken));
        usernameField.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow && !blank(usernameField.getText())) {
                field.validate();
            }
        });
        usernameField.textProperty().addListener((obs, old, now) -> {
            if (blank(now)) {
                field.reset();
                return;
            }
            Result result = ValidationService.validateUsernameAvailable(now, isTaken);
            field.touched = true;
            if (result.isInvalid()) {
                showError(usernameField, messageLabel, result.message());
            } else {
                showValid(usernameField, messageLabel, "Username is available.");
            }
        });
        return field;
    }

    /* ==================================================================== */
    /* Internals                                                            */
    /* ==================================================================== */

    private static void applyBorder(Control control, String borderStyle, String styleClass) {
        Map<Object, Object> props = control.getProperties();
        if (!props.containsKey(BASE_STYLE_KEY)) {
            props.put(BASE_STYLE_KEY, control.getStyle() == null ? "" : control.getStyle());
        }
        control.setStyle(props.get(BASE_STYLE_KEY) + " " + borderStyle);
        control.getStyleClass().removeAll(STYLE_CLASS_ERROR, STYLE_CLASS_VALID);
        control.getStyleClass().add(styleClass);
    }

    private static void restoreStyle(Control control) {
        Object base = control.getProperties().remove(BASE_STYLE_KEY);
        if (base != null) {
            control.setStyle(base.toString());
        }
    }

    private static void setMessage(Label label, String text, String color) {
        label.setText(text);
        label.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void clearMessage(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private static boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
