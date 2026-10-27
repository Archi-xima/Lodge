package com.lodge.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when user input breaks a validation rule.
 *
 * <p>Carries one or more {@link FieldError}s so a screen can highlight every
 * offending field at once instead of stopping at the first problem. It is
 * unchecked on purpose: controllers that pre-validate with
 * {@link ValidationService} never see it, and services can use it as a safety
 * net without cluttering every signature with {@code throws}.</p>
 *
 * <pre>{@code
 *   try {
 *       ValidationService.requireValid(ValidationService.validateNights(text), "nights");
 *   } catch (ValidationException e) {
 *       showError(e.getFirstError().message());
 *   }
 * }</pre>
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** One failed field: which input it belongs to and what to tell the user. */
    public record FieldError(String field, String message) {
    }

    private final List<FieldError> errors;

    /** Single problem on one field. */
    public ValidationException(String field, String message) {
        super(message);
        this.errors = List.of(new FieldError(field, message));
    }

    /** Several problems collected from one form. */
    public ValidationException(List<FieldError> errors) {
        super(summarise(errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /** Every failed field, in the order they were checked. */
    public List<FieldError> getErrors() {
        return errors;
    }

    /** The first failure, or {@code null} if the exception was built with an empty list. */
    public FieldError getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /** Name of the first failed field (e.g. {@code "nights"}), or {@code null}. */
    public String getField() {
        FieldError first = getFirstError();
        return first == null ? null : first.field();
    }

    /** Message for a specific field, or {@code null} when that field passed. */
    public String getMessageFor(String field) {
        for (FieldError error : errors) {
            if (error.field() != null && error.field().equals(field)) {
                return error.message();
            }
        }
        return null;
    }

    private static String summarise(List<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed.";
        }
        if (errors.size() == 1) {
            return errors.get(0).message();
        }
        StringBuilder sb = new StringBuilder("Please fix " + errors.size() + " problems:");
        for (FieldError error : errors) {
            sb.append("\n • ").append(error.message());
        }
        return sb.toString();
    }
}
