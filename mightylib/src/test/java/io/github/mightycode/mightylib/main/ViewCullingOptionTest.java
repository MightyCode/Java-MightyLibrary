package io.github.mightycode.mightylib.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The view culling of the 2D batches: on by default, the configuration, -Dmightylib.viewCulling2D wins; an unknown
// value is ignored
class ViewCullingOptionTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.VIEW_CULLING_2D_PROPERTY);
    }

    @Test
    void thePropertyWinsOverTheConfiguration() {
        EngineConfig config = new EngineConfig();
        assertTrue(config.isViewCulling2D());

        config.setViewCulling2D(false);
        assertFalse(config.isViewCulling2D());

        System.setProperty(EngineConfig.VIEW_CULLING_2D_PROPERTY, " On ");
        assertTrue(config.isViewCulling2D());
        System.setProperty(EngineConfig.VIEW_CULLING_2D_PROPERTY, "off");
        config.setViewCulling2D(true);
        assertFalse(config.isViewCulling2D());

        System.setProperty(EngineConfig.VIEW_CULLING_2D_PROPERTY, "maybe");
        assertTrue(config.isViewCulling2D());
    }
}
