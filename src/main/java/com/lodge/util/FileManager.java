package com.lodge.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thin wrapper around the {@code java.nio.file} operations this application
 * performs.
 *
 * <p>Every method creates missing parent folders, logs what it did and
 * translates {@link IOException} into {@link UncheckedIOException} carrying a
 * human-readable message — so call sites stay free of try/catch boilerplate
 * while failures remain diagnosable in the log file.</p>
 *
 * <pre>{@code
 *   Path file = Constants.DATA_DIR.resolve(Constants.FILE_ROOMS);
 *   FileManager.writeString(file, json);
 *   String stored = FileManager.readString(file);
 * }</pre>
 */
public final class FileManager {

    private static final Logger LOG = Logger.getLogger(FileManager.class);

    private FileManager() {
        // utility class
    }

    /** Creates the directory (and any missing parents) and returns its path. */
    public static Path ensureDirectory(Path directory) {
        try {
            Path created = Files.createDirectories(directory);
            LOG.debug("Directory ready: {}", created);
            return created;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create directory: " + directory, e);
        }
    }

    /** True when the path exists (null-safe). */
    public static boolean exists(Path file) {
        return file != null && Files.exists(file);
    }

    /**
     * Reads a whole file as UTF-8 text.
     *
     * @throws UncheckedIOException when the file cannot be read
     */
    public static String readString(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            LOG.debug("Read {} bytes from {}", content.length(), file);
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file: " + file, e);
        }
    }

    /** Reads the file, or returns {@code fallback} when it does not exist. */
    public static String readStringOrDefault(Path file, String fallback) {
        return exists(file) ? readString(file) : fallback;
    }

    /**
     * Writes UTF-8 text, creating parent folders as needed.
     *
     * @throws UncheckedIOException when the file cannot be written
     */
    public static void writeString(Path file, String content) {
        Path parent = file.getParent();
        if (parent != null) {
            ensureDirectory(parent);
        }
        try {
            Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
            LOG.debug("Wrote {} to {}", file.getFileName(), file.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write file: " + file, e);
        }
    }

    /** Appends one line (with trailing newline), creating the file if needed. */
    public static void appendLine(Path file, String line) {
        Path parent = file.getParent();
        if (parent != null) {
            ensureDirectory(parent);
        }
        try {
            Files.writeString(file, (line == null ? "" : line) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not append to file: " + file, e);
        }
    }

    /** Deletes the file when present; returns {@code true} if something went away. */
    public static boolean delete(Path file) {
        try {
            boolean removed = Files.deleteIfExists(file);
            if (removed) {
                LOG.debug("Deleted {}", file);
            }
            return removed;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete file: " + file, e);
        }
    }

    /**
     * Lists regular files in a directory whose name ends with {@code suffix}
     * (e.g. {@code ".json"}), sorted by name. Returns an empty list when the
     * directory does not exist yet.
     */
    public static List<Path> listFiles(Path directory, String suffix) {
        if (!exists(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(path -> suffix == null || suffix.isEmpty()
                            || path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list directory: " + directory, e);
        }
    }
}
