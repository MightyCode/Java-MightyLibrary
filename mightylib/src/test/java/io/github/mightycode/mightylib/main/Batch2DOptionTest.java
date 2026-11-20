package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The mode of the 2D batches: ORPHAN by default, the configuration, -Dmightylib.batch2D wins; an unknown value is ignored
class Batch2DOptionTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty(EngineConfig.BATCH_2D_PROPERTY);
    }

    @Test
    void thePropertyWinsOverTheConfiguration() {
        EngineConfig config = new EngineConfig();
        assertEquals(EBatch2DMode.ORPHAN, config.getBatch2DMode());

        config.setBatch2DMode(EBatch2DMode.RING);
        assertEquals(EBatch2DMode.RING, config.getBatch2DMode());

        System.setProperty(EngineConfig.BATCH_2D_PROPERTY, " Off ");
        assertEquals(EBatch2DMode.OFF, config.getBatch2DMode());
        System.setProperty(EngineConfig.BATCH_2D_PROPERTY, "instanced");
        assertEquals(EBatch2DMode.INSTANCED, config.getBatch2DMode());

        System.setProperty(EngineConfig.BATCH_2D_PROPERTY, "fast");
        assertEquals(EBatch2DMode.RING, config.getBatch2DMode());
    }

    @Test
    void theModesAreParsedByTheirName() {
        for (EBatch2DMode mode : EBatch2DMode.values())
            assertEquals(mode, EBatch2DMode.parse(mode.optionName()));
        assertNull(EBatch2DMode.parse(null));
        assertNull(EBatch2DMode.parse("orphaned"));
        assertFalse(EBatch2DMode.OFF.isBatched());
        assertTrue(EBatch2DMode.ORPHAN.isBatched());
    }

    @Test
    void aNullModeIsRefused() {
        assertThrows(NullPointerException.class, () -> new EngineConfig().setBatch2DMode(null));
    }
}
