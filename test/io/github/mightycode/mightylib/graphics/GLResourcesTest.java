package io.github.mightycode.mightylib.graphics;

import org.junit.jupiter.api.Test;

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
        GLResources glResources = GLResources.createInstance(-1);
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
}
