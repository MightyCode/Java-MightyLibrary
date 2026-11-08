package io.github.mightycode.mightylib.resources;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Failed loads, error reports, fallbacks and the strict mode.
 */
class ResourcesFailureTest extends ResourcesTestBase {
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
}
