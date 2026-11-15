package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.AnimationDataLoader;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSet;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Malformed home-made formats are ResourceExceptions naming the file and the line, never raw
// NumberFormatException / IndexOutOfBoundsException
class TextFormatLoadersTest {
    @TempDir
    Path folder;

    private String write(String name, String content) throws IOException {
        Path file = folder.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toString();
    }

    private void assertFails(Runnable load, String file, String line) {
        ResourceException e = assertThrows(ResourceException.class, load::run);
        assertTrue(e.getMessage().contains(file), e.getMessage());
        assertTrue(e.getMessage().contains("line " + line), e.getMessage());
    }

    @Test
    void tileSetWithBadNumberNamesFileAndLine() throws IOException {
        String path = write("bad.tileset", "texture tiles\ntilesize 16 abc\n");
        assertFails(() -> new TileSetLoader().initWithFile(new TileSet("bad", path)), "bad.tileset", "2");
    }

    @Test
    void tileSetWithMissingArgumentFails() throws IOException {
        String path = write("short.tileset", "texture\n");
        assertFails(() -> new TileSetLoader().initWithFile(new TileSet("short", path)), "short.tileset", "1");
    }

    @Test
    void tileSetWithTruncatedAnimationsFails() throws IOException {
        String path = write("anim.tileset", "texture tiles\ntilesize 16 16\nanimation 2\n3 3 0.5 4 0.5\n");
        ResourceException e = assertThrows(ResourceException.class,
                () -> new TileSetLoader().initWithFile(new TileSet("anim", path)));
        assertTrue(e.getMessage().contains("anim.tileset"), e.getMessage());
    }

    @Test
    void tileMapWithLayerBeforeSizeFails() throws IOException {
        String path = write("early.tilemap", "layer 0\n1 1\n");
        assertFails(() -> new TileMapLoader().initWithFile(new TileMap("early", path)), "early.tilemap", "1");
    }

    @Test
    void tileMapWithBadTileIdFails() throws IOException {
        String path = write("bad.tilemap", "layerSize 2 1\nlayerNumber 1 0\nlayer 0\n1 x\n");
        assertFails(() -> new TileMapLoader().initWithFile(new TileMap("bad", path)), "bad.tilemap", "4");
    }

    @Test
    void tileMapWithMissingRowsFails() throws IOException {
        String path = write("rows.tilemap", "layerSize 2 3\nlayerNumber 1 0\nlayer 0\n1 1\n");
        assertThrows(ResourceException.class,
                () -> new TileMapLoader().initWithFile(new TileMap("rows", path)));
    }

    @Test
    void tileMapWithLayerOutOfRangeFails() throws IOException {
        String path = write("range.tilemap", "layerSize 1 1\nlayerNumber 1 0\nlayer 5\n1\n");
        assertFails(() -> new TileMapLoader().initWithFile(new TileMap("range", path)), "range.tilemap", "3");
    }

    @Test
    void animationWithBadFrameFails() throws IOException {
        String path = write("bad.anim", "tex\n1\n0,0,16,x,0,0,0.1\n");
        assertFails(() -> new AnimationDataLoader().initWithFile(new AnimationData("bad", path)), "bad.anim", "3");
    }

    @Test
    void animationWithWrongPartCountFails() throws IOException {
        String path = write("parts.anim", "tex\n1\n0,0,16\n");
        assertFails(() -> new AnimationDataLoader().initWithFile(new AnimationData("parts", path)), "parts.anim", "3");
    }

    @Test
    void animationWithMissingFramesFails() throws IOException {
        String path = write("few.anim", "tex\n3\n0,0,16,16,0,0,0.1\n");
        assertThrows(ResourceException.class,
                () -> new AnimationDataLoader().initWithFile(new AnimationData("few", path)));
    }

    @Test
    void animationWithoutCountFails() throws IOException {
        String path = write("empty.anim", "tex\n");
        assertThrows(ResourceException.class,
                () -> new AnimationDataLoader().initWithFile(new AnimationData("empty", path)));
    }
}
