package io.github.mightycode.mightylib.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
}
