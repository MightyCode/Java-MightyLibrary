package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// A wrong icon file is a failure of the data (one line, the default icon stays), not a stack trace
class IconLoaderTest {
    private final IconLoader loader = new IconLoader();

    @TempDir
    Path folder;

    @Test
    void aMissingFileIsAResourceFailure() {
        Icon icon = new Icon("icon", folder.resolve("missing.png").toString());
        ResourceException error = assertThrows(ResourceException.class, () -> loader.initWithFile(icon));
        assertTrue(error.getMessage().contains("missing.png"));
    }

    @Test
    void aFileThatIsNotAnImageIsAResourceFailure() throws IOException {
        Path file = Files.write(folder.resolve("text.png"), "not an image".getBytes());
        Icon icon = new Icon("icon", file.toString());
        assertThrows(ResourceException.class, () -> loader.initWithFile(icon));
    }
}
