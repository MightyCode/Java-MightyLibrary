package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Shared set up of the Resources tests with FakeData (no file, no GL): a Resources with a fake loader, helpers to
 * describe batches, play the main loop until the queues are empty and check the final state.
 * Each test checks that a data is loaded if and only if it is referenced, and that no FakeData saw an
 * inconsistency (double load, unload while not loaded, load before a dependency...).
 */
abstract class ResourcesTestBase {
    protected static final long PUMP_TIMEOUT_MILLISECONDS = 10_000;
    protected static final int WORKERS = 4;

    @TempDir
    Path folder;

    protected String previousFolder;
    protected Resources resources;
    protected FakeLoader fakeLoader;
    protected final List<FakeData> all = new ArrayList<>();

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

    protected FakeData fake(DataType.TYPE_SET_UP typeSetUp, String name) {
        FakeData data = new FakeData(typeSetUp, name);
        if (typeSetUp != DataType.TYPE_SET_UP.THREAD_CONTEXT)
            data.expectMainThread(Thread.currentThread());

        fakeLoader.add(data);
        all.add(data);
        return data;
    }

    protected void batch(String name, String... files) throws IOException {
        StringBuilder list = new StringBuilder();
        for (String file : files) {
            if (list.length() > 0)
                list.append(',');
            list.append('"').append(file).append('"');
        }

        String content = "{\"batchs\":[{\"type\":\"Fake\",\"files\":[" + list + "],\"regex\":[]}]}";
        Files.write(folder.resolve("batchs").resolve(name + ".batch"), content.getBytes(StandardCharsets.UTF_8));
    }

    protected void batchRegex(String name, String regex) throws IOException {
        String content = "{\"batchs\":[{\"type\":\"Fake\",\"files\":[],\"regex\":[\"" + regex + "\"]}]}";
        Files.write(folder.resolve("batchs").resolve(name + ".batch"), content.getBytes(StandardCharsets.UTF_8));
    }

    // Create the data then load the batch descriptions, as Resources.load does before the first batch
    protected void start() {
        resources.init();
        resources.loadAllOfType(BatchResources.class);
    }

    // Plays the main loop until every queue is empty
    protected void pump() throws InterruptedException {
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
    protected void frame() {
        resources.process(1);
        MainThreadTestAccess.processAll();
    }

    protected void assertConsistent() {
        for (FakeData data : all) {
            String name = data.getDataName();
            assertEquals(0, data.errors.get(), name + ": inconsistencies, see the error output");
            assertEquals(data.isReferenced(), data.isLoaded(), name + ": loaded must match referenced");

            int live = data.loads.get() - data.unloads.get();
            assertEquals(data.isLoaded() ? 1 : 0, live, name + ": loads - unloads");
        }
    }

    // ---- More helpers ----

    // Runs the batch, returns what Resources wrote on System.err meanwhile
    protected String loadCapturingErrors(String batchName) throws Exception {
        PrintStream previous = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            resources.loadBatch(batchName);
            pump();
        } finally {
            System.setErr(previous);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    protected FakeData withFallback() {
        FakeData fallback = new FakeData(MAIN_CONTEXT, Resources.FALLBACK_NAME);
        fakeLoader.fallback = fallback;
        return fallback;
    }

    protected void startWithFallbacks() {
        start();
        resources.loadFallbacks();
    }
}
