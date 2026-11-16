package io.github.mightycode.mightylib.resources;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Dependencies between data: load order, chains, cycles, failures that go up the chain.
 */
class ResourcesDependenciesTest extends ResourcesTestBase {
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
}
