package io.github.mightycode.mightylib.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Hot reload: the previous version stays usable, copies are bounded, dependents are reloaded.
 */
class ResourcesReloadTest extends ResourcesTestBase {
    private JSONArray reloadingNames() {
        JSONObject type = resources.describe().getJSONObject("types").getJSONObject("FakeData");
        return type.has("reloading") ? type.getJSONArray("reloading") : new JSONArray();
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

    // The data is given the copy that failed: it can keep what the copy found out (ShaderData: the files to watch)
    @Test
    void aFailedReloadGivesItsCopyToTheData() throws Exception {
        FakeData shader = fake(THREAD_CONTEXT, "shader");
        batch("level", "shader");
        start();
        resources.loadBatch("level");
        pump();

        shader.preloadThrows = new ResourceException("#include \"missing.glsl\" not found");
        resources.reload(shader);
        pump();
        assertTrue(shader.isLoaded());
        assertEquals(1, shader.failedCopies.size());
        assertNotSame(shader, shader.failedCopies.get(0));
        assertEquals("shader", shader.failedCopies.get(0).getDataName());

        // A reload that works is not reported
        shader.preloadThrows = null;
        resources.reload(shader);
        pump();
        assertEquals(1, shader.failedCopies.size());
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
