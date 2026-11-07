package io.github.mightycode.mightylib.resources;

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

    @Test
    void missingFileThrowsResourceException() {
        String path = folder.resolve("missing.json").toString();

        ResourceException e = assertThrows(ResourceException.class, () -> FileMethods.readFileAsString(path));
        assertTrue(e.getMessage().contains("missing.json"), e.getMessage());
        assertEquals(0, e.getStackTrace().length);
    }

    @Test
    void invalidJSONThrowsResourceException() throws IOException {
        Path file = folder.resolve("broken.json");
        Files.write(file, "{ \"a\": ".getBytes(StandardCharsets.UTF_8));

        ResourceException e = assertThrows(ResourceException.class, () -> FileMethods.readJSONObject(file.toString()));
        assertTrue(e.getMessage().contains("invalid JSON"), e.getMessage());
    }

    @Test
    void validJSONIsRead() throws IOException {
        Path file = folder.resolve("fine.json");
        Files.write(file, "{ \"a\": 3 }".getBytes(StandardCharsets.UTF_8));

        assertEquals(3, FileMethods.readJSONObject(file.toString()).getInt("a"));
    }
}
