package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FakeData;
import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLResourcesTest {
    // No GL call: loading, reloading and unloading only change the state
    static class FakeElement extends GLElement {
        Runnable onReload = () -> {};

        @Override
        public boolean load(int remainingMilliseconds) {
            return true;
        }

        @Override
        protected boolean reload(int remainingMilliseconds) {
            onReload.run();
            return true;
        }

        @Override
        public void unload(int remainingMilliseconds) {}
    }

    @Test
    void aReloadStepCanReleaseAnElementWaitingToBeReloaded() {
        GLResources glResources = new GLResources(null, -1);
        FakeElement model = glResources.addElementOrReturnIfPresent(FakeElement.class, new FakeElement(), "reload-model");
        FakeElement texture = glResources.addElementOrReturnIfPresent(FakeElement.class, new FakeElement(), "reload-texture");
        glResources.process(Long.MAX_VALUE);
        assertTrue(model.isLoaded() && texture.isLoaded());

        // Like a model whose new version no longer uses a texture reloaded at the same time
        model.onReload = () -> glResources.releaseResource(texture);
        model.requestReload();
        texture.requestReload();
        glResources.process(Long.MAX_VALUE);

        assertTrue(model.isLoaded());
        assertFalse(texture.isLoaded());
        assertNull(glResources.getResource(FakeElement.class, "reload-texture"));

        glResources.releaseResource(model);
    }

    // An element whose dependencies are never ready: it stays waiting
    static class WaitingElement extends FakeElement {
        boolean ready;

        @Override
        public boolean load(int remainingMilliseconds) {
            return ready;
        }
    }

    @Test
    void preloadedElementsDoNotKeepALoadingScreenWaiting() {
        GLResources glResources = new GLResources(null, -1);
        WaitingElement preloaded = glResources.addElementOrReturnIfPresent(WaitingElement.class, new WaitingElement(),
                "preload-only", false);
        glResources.process(Long.MAX_VALUE);

        assertFalse(preloaded.isLoaded());
        assertFalse(glResources.hasElementsToLoad());
        assertTrue(glResources.namesWaiting(false).contains("preload-only"));

        // Wanted now (a renderer): it becomes urgent, and urgent again preloaded once the renderer releases it
        glResources.addElementOrReturnIfPresent(WaitingElement.class, new WaitingElement(), "preload-only");
        assertTrue(glResources.hasElementsToLoad());
        assertTrue(glResources.namesWaiting(true).contains("preload-only"));
        glResources.releaseResource(preloaded);
        assertFalse(glResources.hasElementsToLoad());
        assertTrue(glResources.namesWaiting(false).contains("preload-only"));

        glResources.releaseResource(preloaded, false);
        assertNull(glResources.getResource(WaitingElement.class, "preload-only"));
        assertFalse(glResources.namesWaiting(false).contains("preload-only"));
    }

    // A user unloaded twice (object added as updatable and displayable) must not delete what a batch still holds
    @Test
    void releasingMoreThanReferencedKeepsTheElement() {
        GLResources glResources = new GLResources(null, -1);
        FakeElement held = glResources.addElementOrReturnIfPresent(FakeElement.class, new FakeElement(),
                "over-released", false);
        glResources.addElementOrReturnIfPresent(FakeElement.class, new FakeElement(), "over-released");
        glResources.process(Long.MAX_VALUE);

        glResources.releaseResource(held);
        glResources.releaseResource(held);
        assertTrue(held.isLoaded());
        assertEquals(1, held.getNumberReference());

        glResources.releaseResource(held, false);
        assertNull(glResources.getResource(FakeElement.class, "over-released"));
    }

    @Test
    void urgentElementsAreLoadedBeforePreloadedOnes() {
        GLResources glResources = new GLResources(null, -1);
        FakeElement preloaded = glResources.addElementOrReturnIfPresent(FakeElement.class, new FakeElement(),
                "order-preloaded", false);
        FakeElement urgent = glResources.addElementOrReturnIfPresent(FakeElement.class, new FakeElement(), "order-urgent");

        // A passed deadline: only the one step always done
        glResources.process(0);
        assertTrue(urgent.isLoaded());
        assertFalse(preloaded.isLoaded());

        glResources.process(Long.MAX_VALUE);
        assertTrue(preloaded.isLoaded());

        glResources.releaseResource(urgent);
        glResources.releaseResource(preloaded, false);
        assertNull(glResources.getResource(FakeElement.class, "order-preloaded"));
    }

    // Made from a data (a shader from its sources): fails while its content is broken
    static class BreakableElement extends FakeElement {
        final DataType source;
        boolean broken = true;
        int loads;
        int reloads;

        BreakableElement(DataType source) {
            this.source = source;
        }

        @Override
        boolean dependsOn(DataType data) {
            return data == source;
        }

        @Override
        public boolean load(int remainingMilliseconds) {
            ++loads;
            if (broken)
                throw new ResourceException("does not compile");
            return true;
        }

        @Override
        protected boolean reload(int remainingMilliseconds) {
            ++reloads;
            if (broken)
                throw new ResourceException("does not compile");
            return true;
        }
    }

    @Test
    void aFailedElementIsTriedAgainOnlyWhenItsDataIsReloaded() {
        GLResources glResources = new GLResources(null, -1);
        FakeData source = new FakeData(DataType.TYPE_SET_UP.THREAD_CONTEXT, "broken-source");
        BreakableElement element = glResources.addElementOrReturnIfPresent(BreakableElement.class,
                new BreakableElement(source), "broken");

        glResources.process(Long.MAX_VALUE);
        assertTrue(element.hasFailed());
        assertFalse(element.isLoaded());
        // Out of the queues: no loop, and a loading screen does not wait for it
        assertFalse(glResources.hasElementsToLoad());
        assertTrue(glResources.namesWaiting(true).isEmpty());
        assertEquals(List.of("BreakableElement broken"), glResources.namesFailed());
        glResources.process(Long.MAX_VALUE);
        assertEquals(1, element.loads);

        // Another data reloaded: still failed
        glResources.dataReloaded(new FakeData(DataType.TYPE_SET_UP.THREAD_CONTEXT, "other"));
        glResources.process(Long.MAX_VALUE);
        assertEquals(1, element.loads);

        // Reloaded but still broken: failed again, once
        glResources.dataReloaded(source);
        glResources.process(Long.MAX_VALUE);
        assertEquals(2, element.loads);
        assertTrue(element.hasFailed());

        // Fixed
        element.broken = false;
        glResources.dataReloaded(source);
        assertTrue(glResources.hasElementsToLoad());
        glResources.process(Long.MAX_VALUE);
        assertTrue(element.isLoaded());
        assertFalse(element.hasFailed());
        assertTrue(glResources.namesFailed().isEmpty());

        glResources.releaseResource(element);
        assertNull(glResources.getResource(BreakableElement.class, "broken"));
    }

    @Test
    void aReleasedFailedElementIsForgotten() {
        GLResources glResources = new GLResources(null, -1);
        FakeData source = new FakeData(DataType.TYPE_SET_UP.THREAD_CONTEXT, "released-source");
        BreakableElement element = glResources.addElementOrReturnIfPresent(BreakableElement.class,
                new BreakableElement(source), "released-broken");
        glResources.process(Long.MAX_VALUE);
        assertTrue(element.hasFailed());

        glResources.releaseResource(element);
        assertTrue(glResources.namesFailed().isEmpty());
        glResources.dataReloaded(source);
        assertTrue(glResources.namesWaiting(true).isEmpty());
        assertEquals(1, element.loads);
    }

    @Test
    void aFailedReloadKeepsTheLoadedVersion() {
        GLResources glResources = new GLResources(null, -1);
        BreakableElement element = new BreakableElement(null);
        element.broken = false;
        glResources.addElementOrReturnIfPresent(BreakableElement.class, element, "reload-broken");
        glResources.process(Long.MAX_VALUE);
        assertTrue(element.isLoaded());

        element.broken = true;
        element.requestReload();
        glResources.process(Long.MAX_VALUE);
        assertEquals(1, element.reloads);
        assertTrue(element.isLoaded());
        assertFalse(element.hasFailed());
        assertTrue(glResources.namesFailed().isEmpty());

        // Not stuck in the reload queue: a new reload is accepted
        glResources.process(Long.MAX_VALUE);
        assertEquals(1, element.reloads);
        element.broken = false;
        element.requestReload();
        glResources.process(Long.MAX_VALUE);
        assertEquals(2, element.reloads);

        glResources.releaseResource(element);
    }

    @Test
    void anElementCanBeLoadedNow() {
        GLResources glResources = new GLResources(null, -1);
        BreakableElement element = new BreakableElement(null);
        element.broken = false;

        assertSame(element, glResources.addElementAndLoadNow(BreakableElement.class, element, "now"));
        assertTrue(element.isLoaded());
        assertTrue(glResources.namesWaiting(true).isEmpty());

        glResources.releaseResource(element);
    }
}
