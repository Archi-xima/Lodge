package com.lodge.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.lodge.service.PersistenceException.Operation;
import com.lodge.util.JsonAdapters;
import com.lodge.util.Logger;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * JSON input/output for the application data layer.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li><b>Saving / loading</b> lists of entities to {@code data/*.json}
 *       (bills, users, rooms, services, guests, bookings, audit).</li>
 *   <li><b>Safe writes</b>: content goes to a temp file first and is moved
 *       into place atomically, after the previous version is kept as
 *       {@code <file>.bak}. A crash mid-write never leaves a half-written file.</li>
 *   <li><b>Auto-save</b>: components register a save callback; a background
 *       timer calls them periodically and once more on JVM shutdown.</li>
 *   <li><b>Data recovery</b>: an unreadable or corrupt file is quarantined
 *       ({@code *.corrupt-<time>}) and restored from its {@code .bak} copy or the
 *       newest snapshot in {@code backups/}. A snapshot of every file is taken
 *       on start-up.</li>
 *   <li><b>Missing directories</b> are created on demand; failure raises
 *       {@link PersistenceException}.</li>
 * </ul>
 */
public class PersistenceService {

    private static final Logger LOG = Logger.getLogger(PersistenceService.class);

    public static final String DATA_SUBDIR = "data";
    public static final String BACKUP_SUBDIR = "backups";
    public static final String BACKUP_SUFFIX = ".bak";
    public static final String EMPTY_LIST_JSON = "[]";
    public static final long DEFAULT_AUTOSAVE_SECONDS = 60;
    public static final int MAX_SNAPSHOTS = 10;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path baseDir;
    private final Path dataDir;
    private final Path backupDir;
    private final Gson gson;
    private final Map<String, Runnable> autoSaveTasks = new LinkedHashMap<>();
    private final List<String> recoveryLog = Collections.synchronizedList(new ArrayList<>());

    private ScheduledExecutorService scheduler;
    private Thread shutdownHook;

    /**
     * @param baseDir application folder; JSON files live in {@code baseDir/data}
     * @throws PersistenceException if the folders cannot be created
     */
    public PersistenceService(Path baseDir) {
        this.baseDir = baseDir;
        this.dataDir = baseDir.resolve(DATA_SUBDIR);
        this.backupDir = baseDir.resolve(BACKUP_SUBDIR);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(JsonAdapters.localDateType(), JsonAdapters.localDateSerializer())
                .registerTypeAdapter(JsonAdapters.localDateType(), JsonAdapters.localDateDeserializer())
                .registerTypeAdapter(JsonAdapters.localDateTimeType(), JsonAdapters.localDateTimeSerializer())
                .registerTypeAdapter(JsonAdapters.localDateTimeType(), JsonAdapters.localDateTimeDeserializer())
                .create();
        ensureDirectory(dataDir);
        ensureDirectory(backupDir);
        migrateLegacyFiles();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getBackupDir() {
        return backupDir;
    }

    public Path fileFor(String fileName) {
        return dataDir.resolve(fileName);
    }

    /** Human-readable notes about every recovery action taken this session. */
    public List<String> getRecoveryLog() {
        synchronized (recoveryLog) {
            return new ArrayList<>(recoveryLog);
        }
    }

    // ------------------------------------------------------------------ schema / start-up

    /**
     * Makes sure each named file exists (as an empty JSON array) and takes a
     * start-up snapshot of the existing data.
     */
    public synchronized void initializeStorage(List<String> fileNames) {
        ensureDirectory(dataDir);
        snapshotAll();
        for (String name : fileNames) {
            Path file = fileFor(name);
            if (!Files.exists(file)) {
                writeAtomically(file, EMPTY_LIST_JSON);
            }
        }
    }

    // ------------------------------------------------------------------ load / save

    /**
     * Loads a JSON array into a list. A missing file yields an empty list. A
     * corrupt or unreadable file triggers recovery from backups; if nothing can
     * be recovered the broken file is set aside and an empty list is returned
     * so the application can still start.
     */
    public synchronized <T> List<T> loadList(String fileName, Class<T> elementType) {
        Path file = fileFor(fileName);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            return parse(file, elementType);
        } catch (PersistenceException firstFailure) {
            LOG.error("Could not load " + fileName + "; attempting recovery", firstFailure);
            return recover(fileName, elementType, firstFailure);
        }
    }

    /**
     * Saves a list as pretty-printed JSON. Skips the write when nothing
     * changed, keeps the previous version as {@code .bak}.
     *
     * @throws PersistenceException if the file cannot be written
     */
    public synchronized <T> void saveList(String fileName, List<T> items, Class<T> elementType) {
        Path file = fileFor(fileName);
        Type listType = TypeToken.getParameterized(List.class, elementType).getType();
        String json = gson.toJson(items == null ? new ArrayList<T>() : items, listType);
        try {
            if (Files.exists(file) && json.equals(Files.readString(file, StandardCharsets.UTF_8))) {
                return;
            }
        } catch (IOException ignored) {
            // unreadable existing file: fall through and overwrite it
        }
        backupCurrent(file);
        writeAtomically(file, json);
    }

    // ------------------------------------------------------------------ auto-save

    /** Registers (or replaces) a callback that persists one component's data. */
    public void registerAutoSave(String name, Runnable saveTask) {
        synchronized (autoSaveTasks) {
            autoSaveTasks.put(name, saveTask);
        }
    }

    public void unregisterAutoSave(String name) {
        synchronized (autoSaveTasks) {
            autoSaveTasks.remove(name);
        }
    }

    /** Runs every registered save callback now; a failing one never blocks the rest. */
    public int saveAllNow() {
        List<Map.Entry<String, Runnable>> tasks;
        synchronized (autoSaveTasks) {
            tasks = new ArrayList<>(autoSaveTasks.entrySet());
        }
        int ok = 0;
        for (Map.Entry<String, Runnable> task : tasks) {
            try {
                task.getValue().run();
                ok++;
            } catch (RuntimeException e) {
                LOG.error("Auto-save of '" + task.getKey() + "' failed; will retry", e);
            }
        }
        return ok;
    }

    /** Starts periodic auto-save plus a final save when the JVM exits. Idempotent. */
    public synchronized void startAutoSave(long periodSeconds) {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lodge-autosave");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::saveAllNow, periodSeconds, periodSeconds, TimeUnit.SECONDS);
        shutdownHook = new Thread(this::saveAllNow, "lodge-shutdown-save");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        LOG.info("Auto-save enabled every {} seconds", periodSeconds);
    }

    /** Stops the timer after one last save. */
    public synchronized void stopAutoSave() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        scheduler = null;
        saveAllNow();
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM already shutting down
        }
        shutdownHook = null;
    }

    public synchronized boolean isAutoSaveRunning() {
        return scheduler != null;
    }

    // ------------------------------------------------------------------ backups

    /**
     * Copies every JSON file in the data directory into a new time-stamped
     * folder under {@code backups/}, pruning to the newest {@value #MAX_SNAPSHOTS}.
     *
     * @return the snapshot folder, or {@code null} if there was nothing to copy
     */
    public synchronized Path snapshotAll() {
        List<Path> files = listJson(dataDir);
        if (files.isEmpty()) {
            return null;
        }
        Path target = backupDir.resolve(LocalDateTime.now().format(STAMP));
        ensureDirectory(target);
        for (Path file : files) {
            try {
                Files.copy(file, target.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new PersistenceException(Operation.BACKUP, file, "Could not back up " + file, e);
            }
        }
        pruneSnapshots();
        return target;
    }

    private void pruneSnapshots() {
        List<Path> snapshots = listSnapshots();
        for (int i = MAX_SNAPSHOTS; i < snapshots.size(); i++) {
            try (Stream<Path> walk = Files.walk(snapshots.get(i))) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch (IOException e) {
                LOG.warn("Could not prune old snapshot {}", snapshots.get(i));
            }
        }
    }

    /** Snapshot folders, newest first. */
    private List<Path> listSnapshots() {
        if (!Files.isDirectory(backupDir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(backupDir)) {
            List<Path> dirs = new ArrayList<>();
            entries.filter(Files::isDirectory).forEach(dirs::add);
            dirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
            return dirs;
        } catch (IOException e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------ internals

    private <T> List<T> parse(Path file, Class<T> elementType) {
        Type listType = TypeToken.getParameterized(List.class, elementType).getType();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<T> loaded = gson.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            throw new PersistenceException(Operation.READ, file, "Could not read " + file, e);
        } catch (JsonParseException | IllegalStateException | DateTimeException e) {
            throw new PersistenceException(Operation.PARSE, file, "Invalid JSON in " + file, e);
        }
    }

    private <T> List<T> recover(String fileName, Class<T> elementType, PersistenceException cause) {
        Path file = fileFor(fileName);
        quarantine(file);

        List<Path> candidates = new ArrayList<>();
        candidates.add(file.resolveSibling(fileName + BACKUP_SUFFIX));
        for (Path snapshot : listSnapshots()) {
            candidates.add(snapshot.resolve(fileName));
        }
        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }
            try {
                List<T> restored = parse(candidate, elementType);
                Files.copy(candidate, file, StandardCopyOption.REPLACE_EXISTING);
                note("Restored " + fileName + " from " + candidate);
                return restored;
            } catch (PersistenceException | IOException e) {
                LOG.warn("Backup {} is not usable: {}", candidate, e.getMessage());
            }
        }
        note("No usable backup for " + fileName + "; starting with an empty file (" + cause.getMessage() + ")");
        return new ArrayList<>();
    }

    private void quarantine(Path file) {
        Path target = file.resolveSibling(file.getFileName() + ".corrupt-" + LocalDateTime.now().format(STAMP));
        try {
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            note("Set aside unreadable file as " + target.getFileName());
        } catch (IOException e) {
            throw new PersistenceException(Operation.RECOVER, file, "Could not quarantine " + file, e);
        }
    }

    private void note(String message) {
        LOG.warn(message);
        recoveryLog.add(message);
    }

    private void backupCurrent(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            Files.copy(file, file.resolveSibling(file.getFileName() + BACKUP_SUFFIX),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // A failed backup must not block saving the new data.
            LOG.warn("Could not refresh backup of {}: {}", file.getFileName(), e.getMessage());
        }
    }

    private void writeAtomically(Path file, String content) {
        ensureDirectory(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best effort clean-up
            }
            throw new PersistenceException(Operation.WRITE, file, "Could not write " + file, e);
        }
    }

    private static void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new PersistenceException(Operation.CREATE_DIRECTORY, directory,
                    "Could not create directory " + directory, e);
        }
    }

    private static List<Path> listJson(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            List<Path> result = new ArrayList<>();
            entries.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(result::add);
            return result;
        } catch (IOException e) {
            throw new PersistenceException(Operation.READ, directory, "Could not list " + directory, e);
        }
    }

    /** Earlier versions kept the JSON files directly in the base folder; move them into data/. */
    private void migrateLegacyFiles() {
        for (Path legacy : listJson(baseDir)) {
            Path target = dataDir.resolve(legacy.getFileName());
            if (Files.exists(target)) {
                continue;
            }
            try {
                Files.move(legacy, target);
                LOG.info("Moved legacy data file {} into {}", legacy.getFileName(), dataDir);
            } catch (IOException e) {
                throw new PersistenceException(Operation.WRITE, legacy, "Could not migrate " + legacy, e);
            }
        }
    }
}
