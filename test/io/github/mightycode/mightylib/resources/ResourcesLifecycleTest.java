package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.Workers;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Load / unload / reload cycles of Resources with FakeData (no file, no GL).
 * Each test checks the final state once the queues are empty: a data is loaded if and only if it is referenced,
 * and no FakeData saw an inconsistency (double load, unload while not loaded, load before a dependency...).
 */
class ResourcesLifecycleTest {
    private static final long PUMP_TIMEOUT_MILLISECONDS = 10_000;
    private static final int WORKERS = 4;

    // The UNLOAD queued while the PRELOAD runs is dropped as obsolete (nothing preloaded yet), then the PRELOAD
    // ends and queues a LOAD: the data ends loaded while nothing references it (leak)
    private static final String KNOWN_BUG_UNLOAD_DURING_PRELOAD =
            "Known bug, fixed by the Resources rework (generations, docs/current.md)";

    @TempDir
    Path folder;

    private String previousFolder;
    private Resources resources;
    private FakeLoader fakeLoader;
    private final List<FakeData> all = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        MainThreadTestAccess.setCurrentAsMain();

        previousFolder = Resources.FOLDER;
        Resources.FOLDER = folder.toString().replace('\\', '/') + "/";
        Files.createDirectories(folder.resolve("batchs"));

        fakeLoader = new FakeLoader();
        JSONLoader jsonLoader = new JSONLoader();
        resources = Resources.createForTests(new Resources.BatchResourcesMethod("unused"), WORKERS,
                Arrays.asList(fakeLoader, jsonLoader, new BatchLoader(jsonLoader)));
    }

    @AfterEach
    void tearDown() {
        Resources.FOLDER = previousFolder;
    }

    // ---- Helpers ----

    private FakeData fake(DataType.TYPE_SET_UP typeSetUp, String name) {
        FakeData data = new FakeData(typeSetUp, name);
        if (typeSetUp == MAIN_CONTEXT)
            data.expectMainThread(Thread.currentThread());

        fakeLoader.add(data);
        all.add(data);
        return data;
    }

    private void batch(String name, String... files) throws IOException {
        StringBuilder list = new StringBuilder();
        for (String file : files) {
            if (list.length() > 0)
                list.append(',');
            list.append('"').append(file).append('"');
        }

        String content = "{\"batchs\":[{\"type\":\"Fake\",\"files\":[" + list + "],\"regex\":[]}]}";
        Files.write(folder.resolve("batchs").resolve(name + ".batch"), content.getBytes(StandardCharsets.UTF_8));
    }

    private void batchRegex(String name, String regex) throws IOException {
        String content = "{\"batchs\":[{\"type\":\"Fake\",\"files\":[],\"regex\":[\"" + regex + "\"]}]}";
        Files.write(folder.resolve("batchs").resolve(name + ".batch"), content.getBytes(StandardCharsets.UTF_8));
    }

    // Create the data then load the batch descriptions, as Resources.load does before the first batch
    private void start() {
        resources.init();
        resources.loadAllOfType(BatchResources.class);
    }

    // Plays the main loop until every queue is empty
    private void pump() throws InterruptedException {
        long end = System.currentTimeMillis() + PUMP_TIMEOUT_MILLISECONDS;
        while (!resources.finishedCurrentLoading()) {
            if (System.currentTimeMillis() > end)
                fail("Resources still loading after " + PUMP_TIMEOUT_MILLISECONDS + " ms");

            resources.process(2);
            MainThreadTestAccess.processAll();
            Thread.sleep(1);
        }

        MainThreadTestAccess.processAll();
    }

    // One frame: a little main thread work while the workers go on
    private void frame() {
        resources.process(1);
        MainThreadTestAccess.processAll();
    }

    private void assertConsistent() {
        for (FakeData data : all) {
            String name = data.getDataName();
            assertEquals(0, data.errors.get(), name + ": inconsistencies, see the error output");
            assertEquals(!data.notReferenced(), data.isLoaded(), name + ": loaded must match referenced");

            int live = data.loads.get() - data.unloads.get();
            assertEquals(data.isLoaded() ? 1 : 0, live, name + ": loads - unloads");
        }
    }

    // ---- Tests ----

    @Test
    void loadBatchLoadsWorkerAndMainContextData() throws Exception {
        FakeData worker = fake(THREAD_CONTEXT, "worker");
        FakeData main = fake(MAIN_CONTEXT, "main");
        batch("level", "worker", "main");
        start();

        resources.loadBatch("level");
        pump();

        assertTrue(worker.isLoaded());
        assertTrue(main.isLoaded());
        assertEquals(1, worker.loads.get());
        assertEquals(1, main.loads.get());
        assertConsistent();
    }

    @Test
    void unloadBatchUnloadsEverything() throws Exception {
        fake(THREAD_CONTEXT, "worker");
        fake(MAIN_CONTEXT, "main");
        batch("level", "worker", "main");
        start();

        resources.loadBatch("level");
        pump();
        resources.unloadBatch("level");
        pump();

        for (FakeData data : all) {
            assertFalse(data.isLoaded(), data.getDataName());
            assertFalse(data.isPreLoaded(), data.getDataName());
            assertEquals(1, data.unloads.get(), data.getDataName());
        }
        assertConsistent();
    }

    @Test
    void sharedDataStaysLoadedWhileAnotherBatchUsesIt() throws Exception {
        FakeData shared = fake(THREAD_CONTEXT, "shared");
        FakeData onlyA = fake(MAIN_CONTEXT, "onlyA");
        batch("a", "shared", "onlyA");
        batch("b", "shared");
        start();

        resources.loadBatch("a");
        resources.loadBatch("b");
        pump();
        resources.unloadBatch("a");
        pump();

        assertTrue(shared.isLoaded());
        assertEquals(1, shared.loads.get());
        assertFalse(onlyA.isLoaded());
        assertConsistent();
    }

    @Test
    void regexSelectsData() throws Exception {
        FakeData tile1 = fake(THREAD_CONTEXT, "tile1");
        FakeData tile2 = fake(MAIN_CONTEXT, "tile2");
        FakeData other = fake(THREAD_CONTEXT, "other");
        batchRegex("tiles", "tile[0-9]+");
        start();

        resources.loadBatch("tiles");
        pump();

        assertTrue(tile1.isLoaded());
        assertTrue(tile2.isLoaded());
        assertFalse(other.isLoaded());
        assertConsistent();
    }

    // Batch unloaded then loaded again before the unload was processed (scene left and entered again)
    @Test
    void unloadThenLoadBeforeProcessingKeepsDataLoaded() throws Exception {
        fake(THREAD_CONTEXT, "worker");
        fake(MAIN_CONTEXT, "main");
        batch("level", "worker", "main");
        start();

        resources.loadBatch("level");
        pump();
        resources.unloadBatch("level");
        resources.loadBatch("level");
        pump();

        for (FakeData data : all)
            assertTrue(data.isLoaded(), data.getDataName());
        assertConsistent();
    }

    // Batch loaded then unloaded while the slow preload is still running in a worker
    @Disabled(KNOWN_BUG_UNLOAD_DURING_PRELOAD)
    @Test
    void unloadDuringPreloadEndsUnloaded() throws Exception {
        FakeData slow = fake(THREAD_CONTEXT, "slow");
        slow.preloadMilliseconds = 50;
        batch("level", "slow");
        start();

        resources.loadBatch("level");
        frame();
        resources.unloadBatch("level");
        pump();

        assertFalse(slow.isLoaded());
        assertConsistent();
    }

    @Test
    void dependencyIsLoadedFirst() throws Exception {
        FakeData texture = fake(MAIN_CONTEXT, "texture");
        texture.preloadMilliseconds = 20;
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = texture;
        batch("level", "model", "texture");
        start();

        resources.loadBatch("level");
        pump();

        assertTrue(texture.isLoaded());
        assertTrue(model.isLoaded());
        assertConsistent();
    }

    @Test
    void failedLoadDoesNotBlockTheQueue() throws Exception {
        FakeData broken = fake(THREAD_CONTEXT, "broken");
        broken.failLoad = true;
        FakeData fine = fake(MAIN_CONTEXT, "fine");
        batch("level", "broken", "fine");
        start();

        resources.loadBatch("level");
        pump();

        assertFalse(broken.isLoaded());
        assertFalse(broken.isPreLoaded());
        assertTrue(fine.isLoaded());
        assertEquals(0, broken.errors.get());
    }

    // Owned resources (textures of a model) are registered from worker threads
    @Test
    void ownedResourceIsSharedAndReleased() throws Exception {
        start();
        FakeData first = new FakeData(THREAD_CONTEXT, "owned");
        all.add(first);

        FakeData[] registered = new FakeData[2];
        CompletableFuture<Void> a = Workers.run(() -> registered[0] = resources.addOwnedResource(FakeData.class, first, "modelA"));
        CompletableFuture<Void> b = Workers.run(() -> registered[1] = resources.addOwnedResource(FakeData.class,
                new FakeData(THREAD_CONTEXT, "owned"), "modelB"));
        CompletableFuture.allOf(a, b).get(5, TimeUnit.SECONDS);
        pump();

        assertSame(registered[0], registered[1], "same name: shared instance");
        FakeData owned = registered[0];
        if (owned != first)
            all.set(0, owned);

        assertTrue(owned.isLoaded());
        assertEquals(2, owned.referenceCount());

        resources.releaseOwnedResource(owned, "modelA");
        pump();
        assertTrue(owned.isLoaded());

        resources.releaseOwnedResource(owned, "modelB");
        pump();
        assertFalse(owned.isLoaded());
        assertConsistent();
    }

    // Random load / unload of overlapping batches, with a few frames between them, then a final check
    @Disabled(KNOWN_BUG_UNLOAD_DURING_PRELOAD)
    @Test
    void randomCyclesStayConsistent() throws Exception {
        String[] names = new String[12];
        for (int i = 0; i < names.length; ++i) {
            FakeData data = fake(i % 3 == 0 ? MAIN_CONTEXT : THREAD_CONTEXT, "data" + i);
            data.preloadMilliseconds = i % 4;
            names[i] = data.getDataName();
        }
        batch("a", Arrays.copyOfRange(names, 0, 6));
        batch("b", Arrays.copyOfRange(names, 4, 10));
        batch("c", Arrays.copyOfRange(names, 8, 12));
        start();

        String[] batches = {"a", "b", "c"};
        boolean[] loaded = new boolean[batches.length];
        Random random = new Random(42);

        for (int step = 0; step < 300; ++step) {
            int index = random.nextInt(batches.length);
            if (loaded[index])
                resources.unloadBatch(batches[index]);
            else
                resources.loadBatch(batches[index]);
            loaded[index] = !loaded[index];

            int frames = random.nextInt(4);
            for (int f = 0; f < frames; ++f)
                frame();

            if (step % 50 == 49) {
                pump();
                assertConsistent();
            }
        }

        pump();
        assertConsistent();
    }
}
