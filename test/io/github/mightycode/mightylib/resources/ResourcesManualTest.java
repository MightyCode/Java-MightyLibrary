package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.Workers;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Data added by the code: owned resources, deletion, no reload of what has no file.
 */
class ResourcesManualTest extends ResourcesTestBase {
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

    // GLLoading: data deleted and created again with the same name in the same frame
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

    // A data does not read a global Resources when it is created: the one registering it attaches itself
    @Test
    void dataKnowsTheResourcesRegisteringIt() throws Exception {
        FakeData fromLoader = fake(MAIN_CONTEXT, "fromLoader");
        FakeData manual = new FakeData(MAIN_CONTEXT, "manual");
        all.add(manual);
        assertThrows(IllegalStateException.class, fromLoader::resources, "not registered before init");
        assertThrows(IllegalStateException.class, manual::resources, "not registered before being added");

        start();
        resources.addResourceAndSetUp(FakeData.class, manual);
        pump();

        assertSame(resources, fromLoader.resources());
        assertSame(resources, manual.resources());
        assertConsistent();
    }

    @Test
    void dataCannotMoveToAnotherResources() {
        FakeData data = fake(MAIN_CONTEXT, "data");
        start();

        Resources other = new Resources(new Resources.AllResourcesMethod(), 1,
                java.util.Collections.singletonList(new FakeLoader()));
        assertThrows(IllegalStateException.class, () -> data.attachTo(other));
        assertSame(resources, data.resources());
    }
}
