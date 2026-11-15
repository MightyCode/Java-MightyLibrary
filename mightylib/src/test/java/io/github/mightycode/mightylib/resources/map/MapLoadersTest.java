package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.resources.ResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// The happy path of the loaders of maps and tilesets (the malformed files are in TextFormatLoadersTest).
// The lines that name a tileset or a texture need a Resources and are left out.
class MapLoadersTest {
    private static final String DEFAULT = TileLayer.DEFAULT_CATEGORY_NAME;

    @TempDir
    Path folder;

    private String write(String name, String content) throws IOException {
        Path file = folder.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toString();
    }

    private TileMap load(String content) throws IOException {
        String path = write("map.tilemap", content);
        TileMap map = new TileMap("map", path);
        new TileMapLoader().initWithFile(map);
        return map;
    }

    @Test
    void tileMapLayersAreReadWithTheIdsMinusOne() throws IOException {
        TileMap map = load("layerSize 3 2\nlayerNumber 1 1\nlayer 0\n1 2 3\n4 5 6\nlayer 1\n0 0 1\n1 0 0\n");

        assertEquals(3, map.mapWidth());
        assertEquals(2, map.mapHeight());
        assertEquals(0, map.getTileType(DEFAULT, 0, 0, 0));
        assertEquals(2, map.getTileType(DEFAULT, 0, 2, 0));
        assertEquals(3, map.getTileType(DEFAULT, 0, 0, 1));
        assertEquals(5, map.getTileType(DEFAULT, 0, 2, 1));
        assertEquals(-1, map.getTileType(DEFAULT, 1, 0, 0), "0 in the file is an empty tile");
        assertEquals(0, map.getTileType(DEFAULT, 1, 2, 0));
        assertEquals(0, map.getTileType(DEFAULT, 1, 0, 1));
    }

    @Test
    void tileMapCategoriesAreReadWithTheirFirstLayer() throws IOException {
        TileMap map = load("layerSize 1 1\nlayerNumber 2 1\nlayerCategory Back 0\nlayerCategory Front 2\n"
                + "layer 0\n1\nlayer 1\n2\nlayer 2\n3\n");

        assertArrayEquals(new String[]{"Back", "Front"}, map.getLayersCategories());
        assertEquals(0, map.getLayerCategoryFrom("Back"));
        assertEquals(1, map.getLayerCategoryTo("Back"));
        assertEquals(2, map.getLayerCategoryFrom("Front"));
        assertEquals(2, map.getTileType("Front", 0, 0, 0));
        assertEquals(1, map.getTileType("Back", 1, 0, 0));
    }

    @Test
    void aCategoryWithoutIndexStartsAtTheFirstLayer() throws IOException {
        TileMap map = load("layerSize 1 1\nlayerNumber 1 0\nlayerCategory Ground\nlayer 0\n1\n");

        assertEquals(0, map.getLayerCategoryFrom("Ground"));
        assertEquals(0, map.getLayerCategoryTo("Ground"));
    }

    @Test
    void windowsLineEndingsAreAccepted() throws IOException {
        TileMap map = load("layerSize 2 1\r\nlayerNumber 1 0\r\nlayer 0\r\n3 4\r\n");

        assertEquals(2, map.getTileType(DEFAULT, 0, 0, 0));
        assertEquals(3, map.getTileType(DEFAULT, 0, 1, 0));
    }

    @Test
    void anEmptyMapFileIsRefused() throws IOException {
        String path = write("empty.tilemap", "");
        ResourceException error = assertThrows(ResourceException.class,
                () -> new TileMapLoader().initWithFile(new TileMap("empty", path)));
        assertTrue(error.getMessage().contains("empty.tilemap"), error.getMessage());
    }

    @Test
    void aNegativeMapSizeIsRefused() throws IOException {
        String path = write("negative.tilemap", "layerSize -1 2\n");
        assertThrows(ResourceException.class, () -> new TileMapLoader().initWithFile(new TileMap("negative", path)));
    }

    @Test
    void aNumberOfLayersThatIsNegativeIsRefused() throws IOException {
        String path = write("negative.tilemap", "layerSize 1 1\nlayerNumber -1 0\n");
        assertThrows(ResourceException.class, () -> new TileMapLoader().initWithFile(new TileMap("negative", path)));
    }

    @Test
    void linesThatAreNotKnownAreIgnored() throws IOException {
        TileMap map = load("# a comment\nlayerSize 1 1\nfoo bar\nlayerNumber 1 0\nlayer 0\n5\n");
        assertEquals(4, map.getTileType(DEFAULT, 0, 0, 0));
    }

    @Test
    void aLayerMissingFromTheFileIsRefused() throws IOException {
        String path = write("missing.tilemap", "layerSize 1 1\nlayerNumber 2 0\nlayer 0\n1\n");
        assertThrows(ResourceException.class, () -> new TileMapLoader().initWithFile(new TileMap("missing", path)));
    }

    @Test
    void tileSetAnimationsAreReadAsATileThenPairsOfFrameAndDuration() throws IOException {
        String path = write("anim.tileset", "animation 2\n3 4 0.5 5 0.25\n7 7 1.5\n");
        TileSet tileSet = new TileSet("anim", path);

        new TileSetLoader().initWithFile(tileSet);

        TileSet.TileAnimation first = tileSet.getAnimation(3);
        assertNotNull(first);
        assertEquals(2, first.getFrameCount());
        assertEquals(4, first.getTileId(0));
        assertEquals(5, first.getTileId(1));
        assertEquals(0.5f, first.getTime(0));
        assertEquals(0.25f, first.getTime(1));

        TileSet.TileAnimation second = tileSet.getAnimation(7);
        assertNotNull(second);
        assertEquals(1, second.getFrameCount());
        assertEquals(1.5f, second.getTime(0));
        assertEquals(2, tileSet.getAnimations().size());
    }

    @Test
    void aTileSetWithoutTextureIsNotCompleted() throws IOException {
        String path = write("empty.tileset", "tilesize 16 16\n");
        TileSet tileSet = new TileSet("empty", path);

        new TileSetLoader().initWithFile(tileSet);

        assertEquals("error", tileSet.texture(), "the texture is still the default one");
    }

    @Test
    void parameterLineNeedsTheTwoFlags() throws IOException {
        String path = write("flags.tileset", "parameter true\n");
        assertThrows(ResourceException.class, () -> new TileSetLoader().initWithFile(new TileSet("flags", path)));
    }

    @Test
    void loadersAcceptTheirOwnFilesOnly() {
        TileMapLoader maps = new TileMapLoader();
        assertEquals("level", maps.filterFile("res/tilemap/level.tilemap"));
        assertNull(maps.filterFile("res/tilemap/level.tileset"));
        assertNull(maps.filterFile("res/tilemap/level"));

        TileSetLoader sets = new TileSetLoader();
        assertEquals("ground", sets.filterFile("res/tileset/ground.tileset"));
        assertNull(sets.filterFile("res/tileset/ground.tilemap"));
    }

    @Test
    void loadersDescribeTheirType() {
        assertEquals(TileMap.class, new TileMapLoader().getType());
        assertEquals("TileMap", new TileMapLoader().getResourceNameType());
        assertEquals(TileSet.class, new TileSetLoader().getType());
        assertEquals("TileSet", new TileSetLoader().getResourceNameType());
    }

    @Test
    void anotherTypeOfDataIsIgnoredByTheLoaders() {
        TileSet notAMap = new TileSet("set", "set.tileset");
        assertDoesNotThrow(() -> new TileMapLoader().initWithFile(notAMap));
        TileMap notASet = new TileMap("map", "map.tilemap");
        assertDoesNotThrow(() -> new TileSetLoader().initWithFile(notASet));
    }
}
