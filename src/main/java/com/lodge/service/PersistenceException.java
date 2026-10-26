package com.lodge.service;

import java.nio.file.Path;

/**
 * Raised when the JSON persistence layer cannot read, write, back up or
 * recover a data file, or when the data directory is missing and cannot be
 * created.
 *
 * <p>It is unchecked so existing service code is not forced to add try/catch
 * blocks, but every instance carries the failing {@link Operation} and the
 * {@link Path} involved so callers (and the log) can report precisely what
 * went wrong.</p>
 */
public class PersistenceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** What the persistence layer was doing when it failed. */
    public enum Operation {
        CREATE_DIRECTORY, READ, WRITE, PARSE, BACKUP, RECOVER
    }

    private final Operation operation;
    private final transient Path path;

    public PersistenceException(Operation operation, Path path, String message) {
        super(message);
        this.operation = operation;
        this.path = path;
    }

    public PersistenceException(Operation operation, Path path, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.path = path;
    }

    public Operation getOperation() {
        return operation;
    }

    /** File or directory that caused the failure (may be {@code null}). */
    public Path getPath() {
        return path;
    }
}
