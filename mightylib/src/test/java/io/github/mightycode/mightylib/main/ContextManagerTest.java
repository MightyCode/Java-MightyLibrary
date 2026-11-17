package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContextManagerTest {
    @Test
    void theMainWindowIsRequiredOnlyOnceTheEngineIsStarted() {
        ContextManager contexts = new ContextManager();

        assertNull(contexts.getMainWindow());
        assertThrows(IllegalStateException.class, contexts::requireMainWindow);
    }

    // The gamepad mappings: the file of the highest source that has it, nothing when absent
    @Test
    void gamepadMappingsAreReadInTheSources(@TempDir Path low, @TempDir Path high) throws IOException {
        ResourceSources sources = ResourceSources.of(new FolderSource(low), new FolderSource(high));
        assertNull(ContextManager.ReadMappings(sources));
        assertNull(ContextManager.ReadMappings(null));

        Files.writeString(low.resolve(GamepadManager.MAPPINGS_FILE), "low,Pad,a:b0\n");
        assertEquals("low,Pad,a:b0\n", ContextManager.ReadMappings(sources));

        Files.writeString(high.resolve(GamepadManager.MAPPINGS_FILE), "high,Pad,a:b1\n");
        assertEquals("high,Pad,a:b1\n", ContextManager.ReadMappings(sources), "not merged: the highest wins");
    }
}
