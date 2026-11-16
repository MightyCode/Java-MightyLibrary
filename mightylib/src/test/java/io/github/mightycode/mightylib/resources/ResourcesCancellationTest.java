package io.github.mightycode.mightylib.resources;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A scene left while its data load: the steps that are no longer wanted are stopped or skipped.
 */
class ResourcesCancellationTest extends ResourcesTestBase {
    private int skippedSteps() {
        return resources.describe().getJSONObject("pipeline").getInt("skippedSteps");
    }

    private static int sum(List<FakeData> data, java.util.function.ToIntFunction<FakeData> count) {
        return data.stream().mapToInt(count).sum();
    }

    // Scene left while a read runs: the loader stops at its next checkCancelled instead of going on
    @Test
    void leavingDuringARunningReadStopsIt() throws Exception {
        FakeData slow = fake(THREAD_CONTEXT, "slow");
        slow.preloadMilliseconds = 300;
        FakeData dependency = fake(THREAD_CONTEXT, "dependency");
        slow.dependencyToAdd = dependency;
        batch("level", "slow");
        start();

        resources.loadBatch("level");
        long end = System.currentTimeMillis() + 5000;
        while (slow.preloads.get() == 0 && System.currentTimeMillis() < end)
            frame();
        resources.unloadBatch("level");
        frame();
        long unloadedAt = System.currentTimeMillis();
        pump();

        assertTrue(System.currentTimeMillis() - unloadedAt < 290, "the read must not wait for its end");
        assertFalse(slow.isPreLoaded());
        assertTrue(dependency.getDependencies().isEmpty() && !dependency.isLoaded());
        assertEquals(0, slow.getDependencies().size(), "stopped before it added its dependency");
        assertEquals(0, slow.loads.get());
        assertTrue(skippedSteps() >= 1);
        assertConsistent();
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
}
