package io.github.mightycode.mightylib.resources;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Batches (load, unload, shared data, regex, reloaded descriptions), what a batch weighs and the progress of a loading.
 */
class ResourcesBatchTest extends ResourcesTestBase {
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

    @Test
    void batchPreviewCountsTheDataAndTheMemoryStillToLoad() throws Exception {
        FakeData a = fake(THREAD_CONTEXT, "a");
        FakeData b = fake(THREAD_CONTEXT, "b");
        batch("level", "a", "b");
        start();

        JSONObject before = resources.describeBatch("level");
        assertEquals(2, before.getInt("listed"));
        assertEquals(0, before.getInt("loaded"));
        assertFalse(before.getBoolean("dependenciesKnown"));

        resources.loadBatch("level");
        pump();

        JSONObject after = resources.describeBatch("level");
        assertEquals(2, after.getInt("loaded"));
        assertTrue(after.getBoolean("dependenciesKnown"));
        assertEquals(0, after.getLong("cpuBytesToLoad"));
        assertTrue(resources.describeBatch("unknown").has("error"));
    }

    @Test
    void wantedProgressCountsWhatIsWantedNowAndLoaded() throws Exception {
        FakeData a = fake(THREAD_CONTEXT, "a");
        FakeData b = fake(THREAD_CONTEXT, "b");
        FakeData next = fake(THREAD_CONTEXT, "next");
        batch("level", "a", "b");
        batch("later", "next");
        start();

        int[] before = resources.wantedProgress();
        resources.loadBatch("level");
        resources.preloadBatch("later");
        int[] asked = resources.wantedProgress();
        // The preloaded data is not wanted now
        assertEquals(before[1] + 2, asked[1]);

        pump();
        int[] done = resources.wantedProgress();
        assertEquals(done[1], done[0]);
        assertEquals(before[1] + 2, done[1]);
        assertTrue(a.isLoaded() && b.isLoaded());
    }
}
