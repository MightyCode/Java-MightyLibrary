package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** Hot reload on file change: FileChangeDetector alone, then the ResourceWatcher with FakeData. */
class FileWatchTest {
    @TempDir
    Path folder;

    private String previousFolder;
    private ResourceWatcher watcher;

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
        previousFolder = Resources.FOLDER;
    }

    @AfterEach
    void tearDown() {
        if (watcher != null)
            watcher.stop();
        Resources.FOLDER = previousFolder;
    }

    private static void write(Path file, String content, long modified) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(file, FileTime.fromMillis(modified));
    }

    private FileChangeDetector detector(Set<Path> paths, long debounce) {
        return new FileChangeDetector(FileChangeDetector.Mode.POLLING, 100, debounce, 0, () -> paths, c -> { });
    }

    // ---- FileChangeDetector ----

    @Test
    void changeIsReportedOnceAfterTheDebounceDelay() throws IOException {
        Path file = folder.resolve("a.txt").toAbsolutePath();
        write(file, "one", 1_000_000);
        FileChangeDetector detector = detector(Collections.singleton(file), 100);

        assertTrue(detector.tick(0, null).isEmpty(), "first look only records the file");
        assertTrue(detector.tick(50, null).isEmpty(), "unchanged");

        write(file, "two!", 2_000_000);
        assertTrue(detector.tick(100, null).isEmpty(), "seen, waiting for it to stay the same");
        assertTrue(detector.tick(150, null).isEmpty(), "debounce not elapsed");
        assertEquals(Collections.singletonList(file), detector.tick(200, null));
        assertTrue(detector.tick(300, null).isEmpty(), "reported once");
    }

    @Test
    void fileStillChangingRestartsTheDelay() throws IOException {
        Path file = folder.resolve("a.txt").toAbsolutePath();
        write(file, "one", 1_000_000);
        FileChangeDetector detector = detector(Collections.singleton(file), 100);
        detector.tick(0, null);

        write(file, "two", 2_000_000);
        detector.tick(100, null);
        write(file, "three", 3_000_000);
        assertTrue(detector.tick(200, null).isEmpty(), "changed again, delay restarts");
        assertEquals(1, detector.tick(300, null).size());
    }

    @Test
    void deletionIsIgnoredAndRecreationIsAChange() throws IOException {
        Path file = folder.resolve("a.txt").toAbsolutePath();
        write(file, "one", 1_000_000);
        FileChangeDetector detector = detector(Collections.singleton(file), 0);
        detector.tick(0, null);

        Files.delete(file);
        assertTrue(detector.tick(100, null).isEmpty());
        assertTrue(detector.tick(200, null).isEmpty());

        write(file, "back", 2_000_000);
        assertEquals(1, detector.tick(300, null).size());
    }

    @Test
    void fileMissingAtFirstIsReportedWhenItAppears() throws IOException {
        Path file = folder.resolve("later.txt").toAbsolutePath();
        FileChangeDetector detector = detector(Collections.singleton(file), 0);
        assertTrue(detector.tick(0, null).isEmpty());

        write(file, "here", 1_000_000);
        assertEquals(1, detector.tick(100, null).size());
    }

    @Test
    void untrackedFilesAreForgotten() throws IOException {
        Path file = folder.resolve("a.txt").toAbsolutePath();
        write(file, "one", 1_000_000);
        Set<Path> paths = new HashSet<>(Collections.singleton(file));
        FileChangeDetector detector = detector(paths, 0);
        detector.tick(0, null);
        assertEquals(1, detector.trackedCount());

        paths.clear();
        write(file, "two", 2_000_000);
        assertTrue(detector.tick(100, null).isEmpty());
        assertEquals(0, detector.trackedCount());
    }

    @Test
    void watchServiceReportsAChange() throws Exception {
        Path file = folder.resolve("a.txt").toAbsolutePath();
        write(file, "one", 1_000_000);
        List<Path> changed = new CopyOnWriteArrayList<>();
        FileChangeDetector detector = new FileChangeDetector(FileChangeDetector.Mode.WATCH_SERVICE, 50, 50, 50,
                () -> Collections.singleton(file), changed::addAll);
        detector.start();
        try {
            Thread.sleep(300); // registered
            write(file, "two!", 2_000_000);

            long end = System.currentTimeMillis() + 5000;
            while (changed.isEmpty() && System.currentTimeMillis() < end)
                Thread.sleep(20);

            assertEquals(Collections.singletonList(file), new ArrayList<>(changed));
        } finally {
            detector.stop();
        }
    }

    // ---- ResourceSelection ----

    @Test
    void selectionNarrowsByTypeNamesAndRegex() {
        FakeData grass = new FakeData(THREAD_CONTEXT, "grass");
        FakeData tileSand = new FakeData(THREAD_CONTEXT, "tile_sand");

        assertTrue(ResourceSelection.all().matches(FakeData.class, grass));
        assertTrue(ResourceSelection.ofType(FakeData.class).matches(FakeData.class, grass));
        assertFalse(ResourceSelection.ofType(BatchResources.class).matches(FakeData.class, grass));

        ResourceSelection names = ResourceSelection.ofType(FakeData.class).withNames("grass");
        assertTrue(names.matches(FakeData.class, grass));
        assertFalse(names.matches(FakeData.class, tileSand));

        ResourceSelection regex = ResourceSelection.ofType(FakeData.class).withRegex("tile_.*");
        assertFalse(regex.matches(FakeData.class, grass));
        assertTrue(regex.matches(FakeData.class, tileSand));

        ResourceSelection both = names.withRegex("tile_.*");
        assertTrue(both.matches(FakeData.class, grass));
        assertTrue(both.matches(FakeData.class, tileSand));

        assertThrows(IllegalArgumentException.class, () -> ResourceSelection.all().withRegex("(["));
    }

    @Test
    void fileCreatedInATrackedDirectoryIsReported() throws IOException {
        Path directory = folder.toAbsolutePath();
        Files.setLastModifiedTime(directory, FileTime.fromMillis(1_000_000));
        FileChangeDetector detector = detector(Collections.singleton(directory), 0);
        assertTrue(detector.tick(0, null).isEmpty(), "first look only records the directory");

        write(folder.resolve("new.txt"), "new", 2_000_000);
        Files.setLastModifiedTime(directory, FileTime.fromMillis(2_000_000));

        assertEquals(Collections.singletonList(directory), detector.tick(100, null));
    }

    // ---- ResourceWatcher ----

    private void pump(Resources resources, long milliseconds) throws InterruptedException {
        long end = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < end) {
            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(2);
        }
    }

    private void pumpUntilIdle(Resources resources) throws InterruptedException {
        long end = System.currentTimeMillis() + 10_000;
        do {
            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(2);
        } while (!resources.finishedCurrentLoading() && System.currentTimeMillis() < end);
    }

    private Resources newResources(FakeLoader fakeLoader) throws IOException {
        Resources.FOLDER = folder.toString().replace('\\', '/') + "/";
        Files.createDirectories(folder.resolve("batchs"));
        JSONLoader jsonLoader = new JSONLoader();
        return Resources.createForTests(new Resources.BatchResourcesMethod("unused"), 2,
                Arrays.asList(fakeLoader, jsonLoader, new BatchLoader(jsonLoader)));
    }

    @Test
    void watcherReloadsOnlyTheSelectedDataWhoseFileChanged() throws Exception {
        FakeLoader fakeLoader = new FakeLoader();
        Resources resources = newResources(fakeLoader);

        Path fileA = folder.resolve("a.dat").toAbsolutePath();
        Path fileB = folder.resolve("b.dat").toAbsolutePath();
        write(fileA, "a", 1_000_000);
        write(fileB, "b", 1_000_000);

        FakeData a = new FakeData(THREAD_CONTEXT, "a");
        a.sourcePath = fileA.toString();
        FakeData b = new FakeData(THREAD_CONTEXT, "b");
        b.sourcePath = fileB.toString();
        fakeLoader.add(a);
        fakeLoader.add(b);

        Files.write(folder.resolve("batchs").resolve("level.batch"),
                ("{\"batchs\":[{\"type\":\"Fake\",\"files\":[\"a\",\"b\"],\"regex\":[]}]}")
                        .getBytes(StandardCharsets.UTF_8));
        resources.init();
        resources.loadAllOfType(BatchResources.class);
        resources.loadBatch("level");
        pumpUntilIdle(resources);
        assertTrue(a.isLoaded() && b.isLoaded());
        int versionA = a.getVersion();
        int versionB = b.getVersion();

        watcher = resources.watcher();
        assertFalse(watcher.isRunning(), "off until asked");
        watcher.configure(ResourceWatcher.Mode.POLLING, 50);
        watcher.watch(ResourceSelection.ofType(FakeData.class).withNames("a"));
        assertTrue(watcher.isRunning());

        pump(resources, 1500); // the files are tracked
        write(fileA, "a changed", 2_000_000);
        write(fileB, "b changed", 2_000_000);

        long end = System.currentTimeMillis() + 10_000;
        while (a.getVersion() == versionA && System.currentTimeMillis() < end)
            pump(resources, 50);
        pumpUntilIdle(resources);

        assertEquals(versionA + 1, a.getVersion(), "selected and changed: reloaded");
        assertEquals(versionB, b.getVersion(), "changed but not selected");
        assertEquals(1, watcher.getReloadCount());

        watcher.stop();
        assertFalse(watcher.isRunning());
    }

    // The description file and the source of a data are written one after the other: one reload
    @Test
    void changesOfTheFilesOfOneDataMakeASingleReload() throws Exception {
        FakeLoader fakeLoader = new FakeLoader();
        Resources resources = newResources(fakeLoader);

        Path description = folder.resolve("a.desc").toAbsolutePath();
        Path source = folder.resolve("a.src").toAbsolutePath();
        write(description, "d", 1_000_000);
        write(source, "s", 1_000_000);

        FakeData a = new FakeData(THREAD_CONTEXT, "a");
        a.sourcePath = description.toString();
        a.extraSourcePath = source.toString();
        fakeLoader.add(a);

        Files.write(folder.resolve("batchs").resolve("level.batch"),
                ("{\"batchs\":[{\"type\":\"Fake\",\"files\":[\"a\"],\"regex\":[]}]}")
                        .getBytes(StandardCharsets.UTF_8));
        resources.init();
        resources.loadAllOfType(BatchResources.class);
        resources.loadBatch("level");
        pumpUntilIdle(resources);
        int version = a.getVersion();

        watcher = resources.watcher();
        watcher.configure(ResourceWatcher.Mode.POLLING, 50);
        watcher.watchType(FakeData.class);
        pump(resources, 1500);

        write(description, "d changed", 2_000_000);
        pump(resources, 150);
        write(source, "s changed", 2_000_000);

        long end = System.currentTimeMillis() + 10_000;
        while (a.getVersion() == version && System.currentTimeMillis() < end)
            pump(resources, 50);
        pump(resources, 1000);
        pumpUntilIdle(resources);

        assertEquals(1, watcher.getReloadCount());
        assertEquals(version + 1, a.getVersion());
    }

    // A file created after the start gives a new data, usable by name
    @Test
    void newFileGivesANewData() throws Exception {
        FakeLoader fakeLoader = new FakeLoader();
        Resources resources = newResources(fakeLoader);
        resources.init();

        watcher = resources.watcher();
        watcher.configure(ResourceWatcher.Mode.POLLING, 50);
        watcher.watchType(FakeData.class);
        pump(resources, 1500); // the folders are tracked

        FakeData late = new FakeData(THREAD_CONTEXT, "late");
        fakeLoader.add(late);
        write(folder.resolve("late.dat"), "late", 2_000_000);

        long end = System.currentTimeMillis() + 10_000;
        while (resources.getResource(FakeData.class, "late") == null && System.currentTimeMillis() < end)
            pump(resources, 50);

        assertSame(late, resources.getResource(FakeData.class, "late"));
    }
}
