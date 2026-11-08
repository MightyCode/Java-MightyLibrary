package io.github.mightycode.mightylib.resources;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Batches loaded in advance: they never hold what is wanted now, and their data is kept once the batch is loaded.
 */
class ResourcesPreloadTest extends ResourcesTestBase {
    // Plays the main loop until the data are all loaded, preloads included
    private void pumpUntilLoaded(FakeData... data) throws InterruptedException {
        long end = System.currentTimeMillis() + PUMP_TIMEOUT_MILLISECONDS;
        for (FakeData one : data) {
            while (!one.isLoaded()) {
                if (System.currentTimeMillis() > end)
                    fail(one.getDataName() + " still not loaded after " + PUMP_TIMEOUT_MILLISECONDS + " ms");

                frame();
                Thread.sleep(1);
            }
        }
    }

    @Test
    void preloadedBatchDoesNotHoldTheLoadingOfWhatIsWantedNow() throws Exception {
        List<String> slowNames = new ArrayList<>();
        FakeData[] slow = new FakeData[WORKERS + 2];
        for (int i = 0; i < slow.length; ++i) {
            slow[i] = fake(THREAD_CONTEXT, "slow" + i);
            slow[i].preloadMilliseconds = 400;
            slowNames.add(slow[i].getDataName());
        }
        FakeData now = fake(THREAD_CONTEXT, "now");
        batch("next", slowNames.toArray(new String[0]));
        batch("current", "now");
        start();

        resources.preloadBatch("next");
        // Steps of the preload are running on the workers when the batch wanted now is asked
        for (int i = 0; i < 20; ++i) {
            frame();
            Thread.sleep(1);
        }
        long start = System.currentTimeMillis();
        resources.loadBatch("current");

        pump();
        assertTrue(now.isLoaded());
        // One worker stays free for it: it does not wait for the preloads (400 ms each, two rounds)
        assertTrue(System.currentTimeMillis() - start < 300, "the data wanted now waited for the preloads");
        assertTrue(resources.finishedCurrentLoading());

        // The preload goes on in the background, and ends
        pumpUntilLoaded(slow);
        assertConsistent();
    }

    @Test
    void loadingAPreloadedBatchKeepsItsData() throws Exception {
        FakeData a = fake(THREAD_CONTEXT, "a");
        FakeData b = fake(MAIN_CONTEXT, "b");
        batch("next", "a", "b");
        start();

        resources.preloadBatch("next");
        pumpUntilLoaded(a, b);
        assertEquals(1, a.loads.get());

        // The scene is set: the data are asked for real, then the preload is released
        resources.loadBatch("next");
        resources.cancelPreloadBatch("next");
        pump();

        assertTrue(a.isLoaded());
        assertTrue(b.isLoaded());
        assertEquals(1, a.loads.get(), "loaded again");
        assertEquals(1, b.loads.get(), "loaded again");
        assertConsistent();
    }

    @Test
    void cancelledPreloadUnloadsWhatNothingElseWants() throws Exception {
        FakeData shared = fake(THREAD_CONTEXT, "shared");
        FakeData onlyNext = fake(THREAD_CONTEXT, "onlyNext");
        batch("current", "shared");
        batch("next", "shared", "onlyNext");
        start();

        resources.loadBatch("current");
        resources.preloadBatch("next");
        pumpUntilLoaded(shared, onlyNext);

        resources.cancelPreloadBatch("next");
        pump();

        assertTrue(shared.isLoaded());
        assertFalse(onlyNext.isLoaded());
        assertEquals(1, shared.loads.get());
        assertConsistent();
    }
}
