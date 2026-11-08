package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.Workers;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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

    private int skippedSteps() {
        return resources.describe().getJSONObject("pipeline").getInt("skippedSteps");
    }

    private static int sum(List<FakeData> data, java.util.function.ToIntFunction<FakeData> count) {
        return data.stream().mapToInt(count).sum();
    }

    // Scene left during its loading: the reads still queued are never started
    @Test
    void leavingDuringLoadingSkipsTheQueuedReads() throws Exception {
        List<FakeData> level = new ArrayList<>();
        String[] names = new String[20];
        for (int i = 0; i < names.length; ++i) {
            names[i] = "data" + i;
            FakeData data = fake(THREAD_CONTEXT, names[i]);
            data.preloadMilliseconds = 50;
            level.add(data);
        }
        batch("level", names);
        start();

        resources.loadBatch("level");
        frame();
        resources.unloadBatch("level");
        pump();

        assertTrue(sum(level, d -> d.preloads.get()) <= WORKERS, "only the reads already started");
        assertEquals(0, sum(level, d -> d.loads.get()));
        assertTrue(skippedSteps() >= names.length - WORKERS);
        assertConsistent();
    }

    // Same for the steps run on the main thread
    @Test
    void leavingDuringLoadingSkipsTheQueuedMainContextSteps() throws Exception {
        List<FakeData> level = new ArrayList<>();
        String[] names = new String[10];
        for (int i = 0; i < names.length; ++i) {
            names[i] = "data" + i;
            FakeData data = fake(MAIN_CONTEXT, names[i]);
            data.preloadMilliseconds = 5;
            level.add(data);
        }
        batch("level", names);
        start();

        resources.loadBatch("level");
        frame();
        resources.unloadBatch("level");
        pump();

        assertTrue(sum(level, d -> d.preloads.get()) < names.length);
        assertEquals(0, sum(level, d -> d.loads.get()));
        assertTrue(skippedSteps() > 0);
        assertConsistent();
    }

    // Checked when the step starts, not when the data is unloaded: wanted again meanwhile, it keeps its step
    @Test
    void wantedAgainBeforeItsStepKeepsIt() throws Exception {
        List<FakeData> level = new ArrayList<>();
        String[] names = new String[12];
        for (int i = 0; i < names.length; ++i) {
            names[i] = "data" + i;
            FakeData data = fake(THREAD_CONTEXT, names[i]);
            data.preloadMilliseconds = 20;
            level.add(data);
        }
        batch("level", names);
        start();

        resources.loadBatch("level");
        frame();
        resources.unloadBatch("level");
        resources.loadBatch("level");
        pump();

        for (FakeData data : level) {
            assertTrue(data.isLoaded(), data.getDataName());
            assertEquals(1, data.preloads.get(), data.getDataName() + " read once");
        }
        assertEquals(0, skippedSteps());
        assertConsistent();
    }

    // Batch loaded then unloaded while the slow preload is still running in a worker
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

    // Runs the batch, returns what Resources wrote on System.err meanwhile
    private String loadCapturingErrors(String batchName) throws Exception {
        PrintStream previous = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, "UTF-8"));
        try {
            resources.loadBatch(batchName);
            pump();
        } finally {
            System.setErr(previous);
        }
        return captured.toString("UTF-8");
    }

    // Missing file, bad format...: the data is wrong, not the code, one line without stack trace
    @Test
    void expectedFailureIsLoggedWithoutStackTrace() throws Exception {
        FakeData missing = fake(THREAD_CONTEXT, "missing");
        missing.preloadThrows = new ResourceException("file not found: textures/missing.png");
        FakeData fine = fake(MAIN_CONTEXT, "fine");
        batch("level", "missing", "fine");
        start();

        String errors = loadCapturingErrors("level");

        assertTrue(missing.failed);
        assertFalse(missing.isPreLoaded());
        assertTrue(fine.isLoaded());
        assertTrue(errors.contains("FakeData missing failed: file not found: textures/missing.png"), errors);
        assertFalse(errors.contains("\tat "), errors);
    }

    // Any other exception is a bug: its stack trace is kept
    @Test
    void unexpectedFailureKeepsTheStackTrace() throws Exception {
        FakeData buggy = fake(THREAD_CONTEXT, "buggy");
        buggy.preloadThrows = new IllegalStateException("bug in the loader");
        batch("level", "buggy");
        start();

        String errors = loadCapturingErrors("level");

        assertTrue(buggy.failed);
        assertTrue(errors.contains("bug in the loader"), errors);
        assertTrue(errors.contains("\tat "), errors);
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

    @Test
    void reloadReadsAgain() throws Exception {
        FakeData worker = fake(THREAD_CONTEXT, "worker");
        FakeData main = fake(MAIN_CONTEXT, "main");
        batch("level", "worker", "main");
        start();

        resources.loadBatch("level");
        pump();
        resources.reloadAllOfType(FakeData.class);
        pump();

        for (FakeData data : Arrays.asList(worker, main)) {
            assertTrue(data.isLoaded(), data.getDataName());
            assertEquals(2, data.preloads.get(), data.getDataName());
            assertEquals(2, data.loads.get(), data.getDataName());
            assertEquals(1, data.unloads.get(), data.getDataName());
        }
        assertConsistent();
    }

    // Reloading many data at once keeps a bounded number of copies in memory, and still reloads them all
    @Test
    void reloadCopiesAreBounded() throws Exception {
        List<FakeData> data = new ArrayList<>();
        String[] names = new String[8];
        for (int i = 0; i < names.length; ++i) {
            names[i] = "data" + i;
            FakeData fake = fake(i % 2 == 0 ? THREAD_CONTEXT : MAIN_CONTEXT, names[i]);
            fake.preloadMilliseconds = 5;
            data.add(fake);
        }
        batch("level", names);
        start();
        resources.setMaxReloadCopies(2);

        resources.loadBatch("level");
        pump();
        resources.reloadAllOfType(FakeData.class);

        int maxCopies = 0;
        long end = System.currentTimeMillis() + 10000;
        while (!resources.finishedCurrentLoading() || data.stream().anyMatch(d -> d.reloadCopy != null)) {
            assertTrue(System.currentTimeMillis() < end, "reload still running");
            frame();
            int copies = 0;
            for (FakeData d : data) {
                if (d.reloadCopy != null)
                    ++copies;
            }
            maxCopies = Math.max(maxCopies, copies);
            Thread.sleep(1);
        }
        pump();

        assertTrue(maxCopies <= 2, "copies alive: " + maxCopies);
        for (FakeData d : data) {
            assertTrue(d.isLoaded(), d.getDataName());
            assertEquals(2, d.preloads.get(), d.getDataName());
        }
        assertConsistent();
    }

    // Reload asked while the first preload runs: read once more once it ends
    @Test
    void reloadDuringPreloadReadsAgain() throws Exception {
        FakeData slow = fake(THREAD_CONTEXT, "slow");
        slow.preloadMilliseconds = 30;
        batch("level", "slow");
        start();

        resources.loadBatch("level");
        frame();
        resources.reloadAllOfType(FakeData.class);
        pump();

        assertTrue(slow.isLoaded());
        assertEquals(2, slow.preloads.get());
        assertConsistent();
    }

    @Test
    void deleteManualResource() throws Exception {
        start();
        FakeData manual = new FakeData(MAIN_CONTEXT, "manual").expectMainThread(Thread.currentThread());
        all.add(manual);

        resources.addResourceAndSetUp(FakeData.class, manual);
        pump();
        assertTrue(manual.isLoaded());

        resources.deleteResource(manual);
        pump();
        assertFalse(manual.isLoaded());
        assertFalse(resources.isExistingResource(FakeData.class, "manual"));
        assertEquals(1, manual.unloads.get());
    }

    // TestGLLoading: data deleted and created again with the same name in the same frame
    @Test
    void deleteThenAddSameNameKeepsTheNewOne() throws Exception {
        start();
        FakeData first = new FakeData(THREAD_CONTEXT, "same");
        first.setPreloaded();
        resources.addPreLoadedResource(first);
        pump();

        FakeData second = new FakeData(THREAD_CONTEXT, "same");
        second.setPreloaded();
        resources.deleteResource(first);
        resources.addPreLoadedResource(second);
        pump();

        assertFalse(first.isLoaded());
        assertTrue(second.isLoaded());
        assertSame(second, resources.getResource(FakeData.class, "same"));
    }

    // A data built by the code has no file: reload leaves it as it is
    @Test
    void reloadSkipsDataPreloadedByCode() throws Exception {
        start();
        FakeData built = new FakeData(THREAD_CONTEXT, "built");
        all.add(built);
        built.setPreloaded();
        resources.addPreLoadedResource(built);
        pump();

        resources.reloadAllOfType(FakeData.class);
        pump();

        assertTrue(built.isLoaded());
        assertEquals(0, built.preloads.get());
        assertEquals(1, built.loads.get());
        assertConsistent();
    }

    @Test
    void allResourcesMethodLoadsEverything() throws Exception {
        fakeLoader = new FakeLoader();
        resources = Resources.createForTests(new Resources.AllResourcesMethod(), WORKERS, Arrays.asList(fakeLoader));
        fake(THREAD_CONTEXT, "a");
        fake(MAIN_CONTEXT, "b");
        resources.init();

        resources.loadAllOfType(FakeData.class);
        pump();

        for (FakeData data : all)
            assertTrue(data.isLoaded(), data.getDataName());
        assertConsistent();
    }

    @Test
    void chainedDependenciesLoadInOrder() throws Exception {
        FakeData a = fake(MAIN_CONTEXT, "a");
        a.preloadMilliseconds = 10;
        FakeData b = fake(THREAD_CONTEXT, "b");
        b.dependencyToAdd = a;
        FakeData c = fake(MAIN_CONTEXT, "c");
        c.dependencyToAdd = b;
        batch("level", "c", "b", "a");
        start();

        resources.loadBatch("level");
        pump();

        for (FakeData data : all)
            assertTrue(data.isLoaded(), data.getDataName());
        assertConsistent();
    }

    // Data waiting for each other would wait forever: the cycle is detected and every member fails
    @Test
    void dependencyCycleFailsInsteadOfWaitingForever() throws Exception {
        FakeData a = fake(THREAD_CONTEXT, "a");
        FakeData b = fake(THREAD_CONTEXT, "b");
        FakeData c = fake(THREAD_CONTEXT, "c");
        a.dependencyToAdd = b;
        b.dependencyToAdd = c;
        c.dependencyToAdd = a;
        FakeData fine = fake(MAIN_CONTEXT, "fine");
        batch("level", "a", "b", "c", "fine");
        start();

        String errors = loadCapturingErrors("level");

        for (FakeData data : new FakeData[]{a, b, c}) {
            assertTrue(data.failed, data.getDataName());
            assertFalse(data.isLoaded(), data.getDataName());
        }
        assertTrue(fine.isLoaded());
        assertTrue(errors.contains("dependency cycle"), errors);
        assertTrue(errors.contains("FakeData a -> FakeData b -> FakeData c -> FakeData a")
                || errors.contains("FakeData b -> FakeData c -> FakeData a -> FakeData b")
                || errors.contains("FakeData c -> FakeData a -> FakeData b -> FakeData c"), errors);
        assertEquals(0, resources.dependencies().waitingCount());
        for (FakeData data : all)
            assertEquals(0, data.errors.get(), data.getDataName());
    }

    // The dependency is not listed by the batch: the data references it, and releases it when unloaded
    @Test
    void dependencyNotListedIsLoadedThenReleased() throws Exception {
        FakeData texture = fake(MAIN_CONTEXT, "texture");
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = texture;
        batch("level", "model");
        start();

        resources.loadBatch("level");
        pump();
        assertTrue(texture.isLoaded());
        assertTrue(model.isLoaded());
        assertConsistent();

        JSONArray onlyAsDependency = resources.describe().getJSONObject("types").getJSONObject("FakeData")
                .getJSONArray("onlyAsDependency");
        assertEquals(1, onlyAsDependency.length());
        assertEquals("texture", onlyAsDependency.getString(0));

        resources.unloadBatch("level");
        pump();
        assertFalse(texture.isLoaded());
        assertFalse(model.isLoaded());
        assertEquals(0, texture.referenceCount());
        assertConsistent();
    }

    // Unloaded while waiting for a slow dependency: both end unloaded
    @Test
    void unloadWhileWaitingForDependency() throws Exception {
        FakeData slow = fake(THREAD_CONTEXT, "slow");
        slow.preloadMilliseconds = 100;
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = slow;
        batch("level", "model");
        start();

        resources.loadBatch("level");
        for (int i = 0; i < 20 && !model.isPreLoaded(); ++i) {
            frame();
            Thread.sleep(1);
        }
        frame();
        assertFalse(model.isLoaded());

        resources.unloadBatch("level");
        pump();
        assertFalse(model.isLoaded());
        assertFalse(model.isPreLoaded());
        assertFalse(slow.isLoaded());
        assertConsistent();
    }

    // The dependency fails after the data started waiting: the data fails instead of waiting forever
    @Test
    void failedDependencyFailsTheWaitingData() throws Exception {
        FakeData broken = fake(THREAD_CONTEXT, "broken");
        broken.preloadMilliseconds = 30;
        broken.failLoad = true;
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = broken;
        batch("level", "model");
        start();

        resources.loadBatch("level");
        pump();

        assertFalse(model.isLoaded());
        assertFalse(model.isPreLoaded());
        assertTrue(model.failed);
        // Released by the failed data: nothing keeps the broken dependency
        assertEquals(0, broken.referenceCount());
        assertEquals(0, model.errors.get());
        assertEquals(0, broken.errors.get());
    }

    // The dependency already failed when the data is preloaded
    @Test
    void alreadyFailedDependencyFailsTheData() throws Exception {
        FakeData broken = fake(MAIN_CONTEXT, "broken");
        broken.failLoad = true;
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.preloadMilliseconds = 30;
        model.dependencyToAdd = broken;
        batch("level", "broken", "model");
        start();

        resources.loadBatch("level");
        pump();

        assertTrue(broken.failed);
        assertTrue(model.failed);
        assertFalse(model.isLoaded());
        assertEquals(1, broken.referenceCount(), "only the batch references it");
    }

    // ---- Fallbacks ----

    private FakeData withFallback() {
        FakeData fallback = new FakeData(MAIN_CONTEXT, Resources.FALLBACK_NAME);
        fakeLoader.fallback = fallback;
        return fallback;
    }

    private void startWithFallbacks() {
        start();
        resources.loadFallbacks();
    }

    @Test
    void usableGivesTheFallbackUntilLoadedThenWhenFailed() throws Exception {
        FakeData fallback = withFallback();
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        texture.preloadMilliseconds = 30;
        batch("level", "texture");
        startWithFallbacks();
        pump();
        assertTrue(fallback.isLoaded());

        resources.loadBatch("level");
        frame();
        assertSame(fallback, resources.usable(FakeData.class, texture), "still loading");
        pump();
        assertSame(texture, resources.usable(FakeData.class, texture));

        // A broken reload keeps the previous version
        texture.failLoad = true;
        resources.reloadAllOfType(FakeData.class);
        pump();
        assertSame(texture, resources.usable(FakeData.class, texture));
        assertEquals(1, texture.content());

        // Broken while read in place: the fallback replaces it, a fixed reload brings it back
        texture.reloadApart = false;
        resources.reloadAllOfType(FakeData.class);
        pump();
        assertSame(fallback, resources.usable(FakeData.class, texture));
        JSONArray usingFallback = resources.describe().getJSONObject("types").getJSONObject("FakeData")
                .getJSONArray("usingFallback");
        assertEquals("texture", usingFallback.getString(0));

        texture.failLoad = false;
        resources.reloadAllOfType(FakeData.class);
        pump();
        assertSame(texture, resources.usable(FakeData.class, texture));
        assertTrue(fallback.isLoaded(), "the fallback is never reloaded nor unloaded");
        assertEquals(0, fallback.errors.get());
    }

    @Test
    void unknownNameGivesTheFallback() throws Exception {
        FakeData fallback = withFallback();
        startWithFallbacks();
        pump();

        assertSame(fallback, resources.getResourceOrFallback(FakeData.class, "missing"));
        assertSame(fallback, resources.getResource(FakeData.class, Resources.FALLBACK_NAME));
    }

    // In debug, a broken texture must not break everything using it: its fallback is used instead
    @Test
    void failedDependencyWithFallbackDoesNotFailTheData() throws Exception {
        withFallback();
        FakeData broken = fake(THREAD_CONTEXT, "broken");
        broken.preloadMilliseconds = 20;
        broken.failLoad = true;
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = broken;
        batch("level", "model");
        startWithFallbacks();

        resources.loadBatch("level");
        pump();

        assertTrue(broken.failed);
        assertTrue(model.isLoaded());
        assertFalse(model.failed);
        assertEquals(0, model.errors.get());

        resources.unloadBatch("level");
        pump();
        assertFalse(model.isLoaded());
        assertFalse(broken.failed);
        assertEquals(0, broken.referenceCount());
    }

    @Test
    void failingFallbackThrows() {
        FakeData fallback = withFallback();
        fallback.failLoad = true;
        startWithFallbacks();

        assertThrows(IllegalStateException.class, this::pump);
    }

    @Test
    void strictFailureThrows() throws Exception {
        FakeData broken = fake(MAIN_CONTEXT, "broken");
        broken.failLoad = true;
        batch("level", "broken");
        start();
        resources.setStrict(true);

        resources.loadBatch("level");
        IllegalStateException error = assertThrows(IllegalStateException.class, this::pump);
        assertTrue(error.getMessage().contains("broken"));
    }

    @Test
    void failureGoesUpTheDependencyChain() throws Exception {
        FakeData a = fake(MAIN_CONTEXT, "a");
        a.preloadMilliseconds = 20;
        a.failLoad = true;
        FakeData b = fake(THREAD_CONTEXT, "b");
        b.dependencyToAdd = a;
        FakeData c = fake(MAIN_CONTEXT, "c");
        c.dependencyToAdd = b;
        batch("level", "c");
        start();

        resources.loadBatch("level");
        pump();

        // Only c is still wanted (by the batch): a and b were released by the failed data, which clears their failure
        assertTrue(c.failed);
        assertEquals(0, a.referenceCount());
        assertEquals(0, b.referenceCount());
        for (FakeData data : all) {
            assertFalse(data.isLoaded(), data.getDataName());
            assertFalse(data.isPreLoaded(), data.getDataName());
            assertEquals(0, data.errors.get(), data.getDataName());
        }

        // Unloading the batch clears the failures
        resources.unloadBatch("level");
        pump();
        for (FakeData data : all) {
            assertFalse(data.failed, data.getDataName());
            assertEquals(0, data.referenceCount(), data.getDataName());
        }
    }

    @Test
    void randomCyclesWithOtherSeeds() throws Exception {
        for (int seed = 1; seed <= 5; ++seed)
            randomCycles(seed);
    }

    // Random load / unload of overlapping batches, with a few frames between them, then a final check
    @Test
    void randomCyclesStayConsistent() throws Exception {
        randomCycles(42);
    }

    private void randomCycles(int seed) throws Exception {
        resources = Resources.createForTests(new Resources.BatchResourcesMethod("unused"), WORKERS,
                Arrays.asList(fakeLoader = new FakeLoader(), new JSONLoader(), new BatchLoader(new JSONLoader())));
        all.clear();

        String[] names = new String[12];
        for (int i = 0; i < names.length; ++i) {
            FakeData data = fake(i % 3 == 0 ? MAIN_CONTEXT : THREAD_CONTEXT, "data" + i);
            data.preloadMilliseconds = i % 4;
            names[i] = data.getDataName();
        }
        // A few dependencies, some of them crossing batches (data4 -> data9 is in no batch with data4 but b)
        all.get(0).dependencyToAdd = all.get(7);
        all.get(4).dependencyToAdd = all.get(9);
        all.get(10).dependencyToAdd = all.get(1);
        batch("a", Arrays.copyOfRange(names, 0, 6));
        batch("b", Arrays.copyOfRange(names, 4, 10));
        batch("c", Arrays.copyOfRange(names, 8, 12));
        start();

        String[] batches = {"a", "b", "c"};
        boolean[] loaded = new boolean[batches.length];
        Random random = new Random(seed);

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

    // ---- Hot reload ----

    private JSONArray reloadingNames() {
        JSONObject type = resources.describe().getJSONObject("types").getJSONObject("FakeData");
        return type.has("reloading") ? type.getJSONArray("reloading") : new JSONArray();
    }

    // The new version is read apart: the data stays loaded with the previous one until it is replaced
    @Test
    void reloadKeepsThePreviousVersionUntilTheNewOneIsLoaded() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        batch("level", "texture");
        start();
        resources.loadBatch("level");
        pump();
        assertEquals(1, texture.content());
        int version = texture.getVersion();

        texture.preloadMilliseconds = 50;
        resources.reload(texture);
        frame();
        assertTrue(texture.isLoaded());
        assertEquals(1, texture.content(), "previous version while reading");
        assertSame(texture, resources.usable(FakeData.class, texture));
        assertEquals("texture", reloadingNames().getString(0));

        pump();
        assertEquals(2, texture.content());
        assertEquals(version + 1, texture.getVersion());
        assertTrue(reloadingNames().isEmpty());
        assertConsistent();
    }

    @Test
    void failedReloadKeepsThePreviousVersion() throws Exception {
        FakeData texture = fake(MAIN_CONTEXT, "texture");
        batch("level", "texture");
        start();
        resources.loadBatch("level");
        pump();
        int version = texture.getVersion();

        texture.failLoad = true;
        resources.reload(texture);
        pump();
        assertTrue(texture.isLoaded());
        assertFalse(texture.hasFailed());
        assertEquals(1, texture.content());
        assertEquals(version, texture.getVersion());
        assertConsistent();

        // Fixed: the next reload brings the new version
        texture.failLoad = false;
        resources.reload(texture);
        pump();
        assertEquals(3, texture.content());
        assertConsistent();
    }

    // The data using a reloaded one are read again, in chain
    @Test
    void reloadedDependencyReloadsItsDependents() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        FakeData tileSet = fake(THREAD_CONTEXT, "tileset");
        tileSet.dependencyToAdd = texture;
        FakeData map = fake(MAIN_CONTEXT, "map");
        map.dependencyToAdd = tileSet;
        FakeData other = fake(THREAD_CONTEXT, "other");
        batch("level", "map", "other");
        start();
        resources.loadBatch("level");
        pump();
        int mapVersion = map.getVersion();

        resources.reload(texture);
        pump();
        assertEquals(2, texture.preloads.get());
        assertEquals(2, tileSet.preloads.get());
        assertEquals(2, map.preloads.get());
        assertEquals(1, other.preloads.get());
        assertEquals(mapVersion + 1, map.getVersion());
        assertEquals(1, texture.referenceCount(), "only the tileset references it");
        assertConsistent();
    }

    // Everything reloaded at once: a data waits for its dependencies to be replaced, then is computed once
    @Test
    void reloadAllComputesEachDataOnce() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        FakeData tileSet = fake(MAIN_CONTEXT, "tileset");
        tileSet.dependencyToAdd = texture;
        FakeData map = fake(THREAD_CONTEXT, "map");
        map.dependencyToAdd = tileSet;
        batch("level", "map");
        start();
        resources.loadBatch("level");
        pump();

        texture.preloadMilliseconds = 40;
        resources.reloadAllOfType(FakeData.class);
        pump();
        for (FakeData data : all)
            assertEquals(2, data.preloads.get(), data.getDataName());
        assertConsistent();
    }

    // Plays frames until the condition holds (false after the timeout)
    private boolean frameUntil(java.util.function.BooleanSupplier condition, long timeoutMilliseconds)
            throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMilliseconds;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > end)
                return false;
            frame();
            Thread.sleep(1);
        }
        return true;
    }

    // The source changed again while its new version is read: a new read starts at once, the first one is dropped
    @Test
    void secondReloadAbandonsTheFirstRead() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        batch("level", "texture");
        start();
        resources.loadBatch("level");
        pump();
        int version = texture.getVersion();

        texture.preloadMilliseconds = 300;
        resources.reload(texture);
        assertTrue(frameUntil(() -> texture.preloads.get() == 2, 1000));

        resources.reload(texture);
        assertTrue(frameUntil(() -> texture.preloads.get() == 3, 200), "second read started without waiting");
        assertEquals(1, texture.content(), "previous version while reading");

        pump();
        assertEquals(3, texture.content(), "the latest read wins");
        assertEquals(version + 1, texture.getVersion(), "replaced once");
        assertTrue(reloadingNames().isEmpty());
        assertConsistent();
    }

    // An abandoned copy waiting for its dependencies is dropped at once and releases them
    @Test
    void abandonedCopyWaitingDependenciesIsReleased() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = texture;
        batch("level", "model");
        start();
        resources.loadBatch("level");
        pump();

        texture.preloadMilliseconds = 100;
        resources.reloadAllOfType(FakeData.class);
        assertTrue(frameUntil(() -> model.preloads.get() == 2 && texture.preloads.get() == 2, 1000));
        resources.reload(model);
        pump();

        assertEquals(3, model.preloads.get());
        assertEquals(1, texture.referenceCount(), "only the model references it");
        assertTrue(reloadingNames().isEmpty());
        assertConsistent();
    }

    // No longer wanted while its new version is read: the copy is dropped with it
    @Test
    void unloadDuringReloadDropsTheNewVersion() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        FakeData model = fake(THREAD_CONTEXT, "model");
        model.dependencyToAdd = texture;
        batch("level", "model");
        start();
        resources.loadBatch("level");
        pump();

        model.preloadMilliseconds = 50;
        resources.reload(model);
        frame();
        resources.unloadBatch("level");
        pump();
        assertFalse(model.isLoaded());
        assertFalse(texture.isLoaded());
        assertEquals(0, texture.referenceCount());
        assertConsistent();
    }

    // A type without reload copy is emptied then read again
    @Test
    void typeWithoutReloadCopyIsReadAgainInPlace() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        texture.reloadApart = false;
        batch("level", "texture");
        start();
        resources.loadBatch("level");
        pump();

        texture.preloadMilliseconds = 50;
        resources.reload(texture);
        frame();
        assertFalse(texture.isLoaded());
        pump();
        assertEquals(2, texture.content());
        assertConsistent();
    }

    // Loaded with the fallback of a failed dependency: read again once the dependency is fixed
    @Test
    void fixedDependencyReloadsTheDataUsingItsFallback() throws Exception {
        withFallback();
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        texture.failLoad = true;
        FakeData tileSet = fake(THREAD_CONTEXT, "tileset");
        tileSet.dependencyToAdd = texture;
        batch("level", "tileset");
        startWithFallbacks();
        resources.loadBatch("level");
        pump();
        assertTrue(texture.hasFailed());
        assertTrue(tileSet.isLoaded());

        texture.failLoad = false;
        resources.reload(texture);
        pump();
        assertTrue(texture.isLoaded());
        assertEquals(2, tileSet.preloads.get());
        assertConsistent();
    }

    // The description of a loaded batch changes: the new list is loaded, and unloadBatch releases exactly it
    @Test
    void reloadedBatchAppliesItsNewList() throws Exception {
        FakeData a = fake(THREAD_CONTEXT, "a");
        FakeData b = fake(THREAD_CONTEXT, "b");
        FakeData c = fake(MAIN_CONTEXT, "c");
        batch("level", "a", "b");
        start();
        resources.loadBatch("level");
        pump();

        batch("level", "b", "c");
        resources.reloadAllOfType(BatchResources.class);
        pump();
        assertFalse(a.isLoaded());
        assertTrue(b.isLoaded());
        assertTrue(c.isLoaded());
        assertEquals(1, b.loads.get(), "kept loaded while the list changed");
        assertConsistent();

        resources.unloadBatch("level");
        pump();
        for (FakeData data : all)
            assertEquals(0, data.referenceCount(), data.getDataName());
        assertConsistent();
    }

    // A new description listing an unknown data is not applied: the batch keeps what it loaded
    @Test
    void reloadedBatchWithUnknownDataKeepsItsList() throws Exception {
        FakeData a = fake(THREAD_CONTEXT, "a");
        batch("level", "a");
        start();
        resources.loadBatch("level");
        pump();

        batch("level", "a", "unknown");
        resources.reloadAllOfType(BatchResources.class);
        pump();
        assertTrue(a.isLoaded());

        resources.unloadBatch("level");
        pump();
        assertEquals(0, a.referenceCount());
        assertConsistent();
    }

    // Generated by the code: not read again by a reload, but computed again when its dependency changes
    @Test
    void generatedDataIsOnlyComputedAgainWhenItsDependencyChanges() throws Exception {
        FakeData texture = fake(THREAD_CONTEXT, "texture");
        FakeData atlas = fake(MAIN_CONTEXT, "atlas");
        atlas.generated = true;
        atlas.dependencyToAdd = texture;
        batch("level", "atlas");
        start();
        resources.loadBatch("level");
        pump();

        resources.reload(atlas);
        pump();
        assertEquals(1, atlas.preloads.get());

        resources.reload(texture);
        pump();
        assertEquals(2, atlas.loads.get());
        assertConsistent();
    }
}
