package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Expected failures of file reading are ResourceException (logged without stack trace by Resources)
class FileMethodsTest {
    @TempDir
    Path folder;

    private ResourceSources sources;

    @BeforeEach
    void setUp() {
        sources = ResourceSources.of(new FolderSource(folder));
    }

    @Test
    void missingFileThrowsResourceException() {
        ResourceException e = assertThrows(ResourceException.class,
                () -> FileMethods.readString(sources, "missing.json"));
        assertTrue(e.getMessage().contains("missing.json"), e.getMessage());
        assertEquals(0, e.getStackTrace().length);
    }

    @Test
    void pathOutOfTheSourcesThrowsResourceException() {
        ResourceException e = assertThrows(ResourceException.class,
                () -> FileMethods.readString(sources, "../outside.json"));
        assertEquals(0, e.getStackTrace().length);
    }

    @Test
    void invalidJSONThrowsResourceException() throws IOException {
        Files.write(folder.resolve("broken.json"), "{ \"a\": ".getBytes(StandardCharsets.UTF_8));

        ResourceException e = assertThrows(ResourceException.class,
                () -> FileMethods.readJSONObject(sources, "broken.json"));
        assertTrue(e.getMessage().contains("invalid JSON"), e.getMessage());
    }

    @Test
    void validJSONIsRead() throws IOException {
        Files.write(folder.resolve("fine.json"), "{ \"a\": 3 }".getBytes(StandardCharsets.UTF_8));

        assertEquals(3, FileMethods.readJSONObject(sources, "fine.json").getInt("a"));
    }
}
