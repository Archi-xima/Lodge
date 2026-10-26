package com.lodge.service;

import com.lodge.model.Room;
import com.lodge.model.RoomStatus;
import com.lodge.model.RoomType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PersistenceServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path base;
    private PersistenceService service;

    @Before
    public void setUp() {
        base = temporaryFolder.getRoot().toPath().resolve("app");
        service = new PersistenceService(base);
    }

    private List<Room> rooms() {
        Room room = new Room("101", RoomType.STANDARD, 1);
        room.setStatus(RoomStatus.OCCUPIED);
        List<Room> list = new ArrayList<>();
        list.add(room);
        return list;
    }

    @Test
    public void createsMissingDirectoriesAndEmptySchemaFiles() {
        service.initializeStorage(List.of("rooms.json", "bills.json"));
        assertTrue(Files.isDirectory(base.resolve("data")));
        assertTrue(Files.exists(base.resolve("data/rooms.json")));
        assertTrue(service.loadList("bills.json", Room.class).isEmpty());
    }

    @Test
    public void savesAndLoadsAList() {
        service.saveList("rooms.json", rooms(), Room.class);
        List<Room> loaded = service.loadList("rooms.json", Room.class);
        assertEquals(1, loaded.size());
        assertEquals("101", loaded.get(0).getRoomNumber());
        assertEquals(RoomStatus.OCCUPIED, loaded.get(0).getStatus());
    }

    @Test
    public void keepsPreviousVersionAsBackupAndLeavesNoTempFile() throws Exception {
        service.saveList("rooms.json", rooms(), Room.class);
        service.saveList("rooms.json", new ArrayList<Room>(), Room.class);
        assertTrue(Files.exists(base.resolve("data/rooms.json.bak")));
        assertFalse(Files.exists(base.resolve("data/rooms.json.tmp")));
    }

    @Test
    public void recoversCorruptFileFromBackup() throws Exception {
        service.saveList("rooms.json", rooms(), Room.class);
        service.saveList("rooms.json", rooms(), Room.class); // unchanged: no-op
        List<Room> two = rooms();
        two.add(new Room("102", RoomType.DELUXE, 1));
        service.saveList("rooms.json", two, Room.class); // .bak now holds the 1-room version
        Files.writeString(base.resolve("data/rooms.json"), "{ not json");

        List<Room> loaded = service.loadList("rooms.json", Room.class);

        assertEquals(1, loaded.size());
        assertFalse(service.getRecoveryLog().isEmpty());
        try (var files = Files.list(base.resolve("data"))) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().contains(".corrupt-")));
        }
        assertEquals(1, service.loadList("rooms.json", Room.class).size());
    }

    @Test
    public void corruptFileWithNoBackupYieldsEmptyListInsteadOfCrashing() throws Exception {
        Files.writeString(base.resolve("data/users.json"), "garbage");
        assertTrue(service.loadList("users.json", Room.class).isEmpty());
    }

    @Test
    public void recoversFromStartupSnapshot() throws Exception {
        service.saveList("rooms.json", rooms(), Room.class);
        assertNotNull(service.snapshotAll());
        Files.deleteIfExists(base.resolve("data/rooms.json.bak"));
        Files.writeString(base.resolve("data/rooms.json"), "[[[");
        assertEquals(1, service.loadList("rooms.json", Room.class).size());
    }

    @Test
    public void migratesLegacyFilesIntoDataFolder() throws Exception {
        Path legacyBase = temporaryFolder.getRoot().toPath().resolve("legacy");
        Files.createDirectories(legacyBase);
        Files.writeString(legacyBase.resolve("rooms.json"), "[]");
        PersistenceService migrated = new PersistenceService(legacyBase);
        assertTrue(Files.exists(migrated.fileFor("rooms.json")));
        assertFalse(Files.exists(legacyBase.resolve("rooms.json")));
    }

    @Test
    public void saveAllNowRunsEveryTaskEvenIfOneFails() {
        AtomicInteger runs = new AtomicInteger();
        service.registerAutoSave("bad", () -> { throw new PersistenceException(
                PersistenceException.Operation.WRITE, null, "boom"); });
        service.registerAutoSave("good", runs::incrementAndGet);
        assertEquals(1, service.saveAllNow());
        assertEquals(1, runs.get());
    }

    @Test
    public void writeFailureRaisesPersistenceException() throws Exception {
        Files.createDirectories(base.resolve("data/rooms.json")); // a directory where the file should be
        try {
            service.saveList("rooms.json", rooms(), Room.class);
            fail("expected PersistenceException");
        } catch (PersistenceException e) {
            assertEquals(PersistenceException.Operation.WRITE, e.getOperation());
        }
    }

    @Test
    public void dataStoreUsesDataSubfolder() {
        DataStore store = new DataStore(base.resolve("store"));
        store.saveRooms(rooms());
        assertTrue(Files.exists(base.resolve("store/data/rooms.json")));
        assertEquals(1, store.loadRooms().size());
    }
}