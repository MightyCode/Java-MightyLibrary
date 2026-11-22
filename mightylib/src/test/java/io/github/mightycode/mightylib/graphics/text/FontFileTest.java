package io.github.mightycode.mightylib.graphics.text;

import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontFileTest {
    // A failure of the data: the preload fails on one line and the fallback font replaces it
    @Test
    void aMissingFileIsAResourceFailure(@TempDir Path folder) {
        FontFile file = new FontFile("fonts/missing.fnt");
        ResourceException error = assertThrows(ResourceException.class, () -> file.load(ResourceSources.of(new FolderSource(folder))));
        assertTrue(error.getMessage().contains("missing.fnt"));
    }

    private static final String HEADER = "info face=\"Test\" size=32 padding=0,0,0,0\n"
            + "common lineHeight=40 scaleW=256 scaleH=256\n";

    private static FontFile write(Path folder, String content) throws IOException {
        Path file = folder.resolve("font.fnt");
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return new FontFile("font.fnt");
    }

    @Test
    void aFileWithoutCountIsTruncated(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER);
        ResourceException error = assertThrows(ResourceException.class, () -> file.load(ResourceSources.of(new FolderSource(folder))));
        assertTrue(error.getMessage().contains("truncated"), error.getMessage());
    }

    @Test
    void missingCharactersAreTruncated(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER + "chars count=2\n"
                + "char id=65 x=0 y=0 width=10 height=10 xoffset=0 yoffset=0 xadvance=10\n");
        ResourceException error = assertThrows(ResourceException.class, () -> file.load(ResourceSources.of(new FolderSource(folder))));
        assertTrue(error.getMessage().contains("truncated"), error.getMessage());
    }

    @Test
    void aMissingValueIsMalformed(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER + "chars count=1\n" + "char id=65 x=0 y=0\n");
        ResourceException error = assertThrows(ResourceException.class, () -> file.load(ResourceSources.of(new FolderSource(folder))));
        assertTrue(error.getMessage().contains("no width"), error.getMessage());
    }

    @Test
    void aBadNumberIsMalformed(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER.replace("size=32", "size=big") + "chars count=0\n");
        ResourceException error = assertThrows(ResourceException.class, () -> file.load(ResourceSources.of(new FolderSource(folder))));
        assertTrue(error.getMessage().contains("malformed"), error.getMessage());
    }

    @Test
    void aNullSizeIsMalformed(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER.replace("size=32", "size=0") + "chars count=0\n");
        ResourceException error = assertThrows(ResourceException.class, () -> file.load(ResourceSources.of(new FolderSource(folder))));
        assertTrue(error.getMessage().contains("size 0"), error.getMessage());
    }

    @Test
    void aValidFileLoads(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, HEADER + "chars count=1\n"
                + "char id=65 x=0 y=0 width=10 height=10 xoffset=0 yoffset=0 xadvance=10\n");
        assertDoesNotThrow(() -> file.load(ResourceSources.of(new FolderSource(folder))));
    }

    // The atlas is a 2D texture: the part of a character in 0..1 of the atlas, inset by half a texel
    @Test
    void theTextureBoundsAreNormalizedAndInsetByHalfATexel(@TempDir Path folder) throws IOException {
        FontFile file = write(folder, "info face=\"Test\" size=32 padding=1,2,3,4\n"
                + "common lineHeight=40 scaleW=256 scaleH=128\n" + "chars count=1\n"
                + "char id=65 x=20 y=10 width=16 height=24 xoffset=0 yoffset=0 xadvance=10\n");
        file.load(ResourceSources.of(new FolderSource(folder)));

        // Padding up 1, right 2, down 3, left 4: the glyph starts at x + 4, y + 1, is 16 - 4 - 2 by 24 - 1 - 3
        Vector4f bounds = new Vector4f();
        file.textureBounds(file.getCharacter('A'), bounds);
        assertEquals((24 + 0.5f) / 256f, bounds.x, 1e-6f);
        assertEquals((24 + 10 - 0.5f) / 256f, bounds.y, 1e-6f);
        assertEquals((11 + 0.5f) / 128f, bounds.z, 1e-6f);
        assertEquals((11 + 20 - 0.5f) / 128f, bounds.w, 1e-6f);
    }

    // The boxes of the fallback font cover their whole atlas, inside its border texels
    @Test
    void aBoxCoversTheWholeAtlas() {
        FontFile file = FontFile.CreateBoxes(16);
        Vector4f bounds = new Vector4f();
        file.textureBounds(file.getCharacter('A'), bounds);
        assertEquals(new Vector4f(0.5f / 16, 15.5f / 16, 0.5f / 16, 15.5f / 16), bounds);
    }
}
