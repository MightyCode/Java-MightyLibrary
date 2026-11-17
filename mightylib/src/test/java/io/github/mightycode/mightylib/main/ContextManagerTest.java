package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.resources.source.ClasspathSource;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
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

    // The engine provides the mappings (its source is the lowest); the file of the game at the root replaces them
    @Test
    void gamepadMappingsOfTheEngineAreReplacedByTheGame(@TempDir Path game) throws IOException {
        IResourceSource engine = new ClasspathSource(ContextManagerTest.class.getClassLoader(), "assets/mightylib/");
        ResourceSources engineOnly = ResourceSources.of(engine);
        String provided = ContextManager.ReadMappings(engineOnly);
        assertNotNull(provided);
        assertTrue(provided.contains(",platform:Windows"));

        Files.writeString(game.resolve(GamepadManager.MAPPINGS_FILE), "game,Pad,a:b1\n");
        ResourceSources stack = ResourceSources.of(engine, new FolderSource(game));
        assertEquals("game,Pad,a:b1\n", ContextManager.ReadMappings(stack));
    }
}
