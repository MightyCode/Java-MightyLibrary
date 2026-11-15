package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontFileTest {
    // A failure of the data: the preload fails on one line and the fallback font replaces it
    @Test
    void aMissingFileIsAResourceFailure() {
        FontFile file = new FontFile("resources/fonts/missing.fnt");
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("missing.fnt"));
    }

    private static final String HEADER = "info face=\"Test\" size=32 padding=0,0,0,0\n"
            + "common lineHeight=40 scaleW=256 scaleH=256\n";

    private static FontFile write(Path folder, String content) throws IOException {
        Path file = folder.resolve("font.fnt");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return new FontFile(file.toString());
    }

    @Test
    void aFileWithoutCountIsTruncated(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER);
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("truncated"), error.getMessage());
    }

    @Test
    void missingCharactersAreTruncated(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER + "chars count=2\n"
                + "char id=65 x=0 y=0 width=10 height=10 xoffset=0 yoffset=0 xadvance=10\n");
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("truncated"), error.getMessage());
    }

    @Test
    void aMissingValueIsMalformed(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER + "chars count=1\n" + "char id=65 x=0 y=0\n");
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("no width"), error.getMessage());
    }

    @Test
    void aBadNumberIsMalformed(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER.replace("size=32", "size=big") + "chars count=0\n");
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("malformed"), error.getMessage());
    }

    @Test
    void aNullSizeIsMalformed(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER.replace("size=32", "size=0") + "chars count=0\n");
        ResourceException error = assertThrows(ResourceException.class, file::load);
        assertTrue(error.getMessage().contains("size 0"), error.getMessage());
    }

    @Test
    void aValidFileLoads(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER + "chars count=1\n"
                + "char id=65 x=0 y=0 width=10 height=10 xoffset=0 yoffset=0 xadvance=10\n");
        assertDoesNotThrow(file::load);
    }
}
