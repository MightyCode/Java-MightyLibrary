package io.github.mightycode.mightylib.resources.map;

import io.github.mightycode.mightylib.algorithms.OutputCapture;
import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.resources.DataTypeTestAccess;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.ILogListener;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TileMapTest {
    private static final String DEFAULT = TileLayer.DEFAULT_CATEGORY_NAME;

    private final TileMap map = new TileMap("map", "map.tilemap");

    // Layers of the given size filled with -1 (empty tiles)
    private static TileLayer[] emptyLayers(int count, int width, int height) {
        TileLayer[] layers = new TileLayer[count];
        for (int i = 0; i < count; ++i) {
            layers[i] = new TileLayer(new Vector2i(width, height));
            for (int y = 0; y < height; ++y)
                for (int x = 0; x < width; ++x)
                    layers[i].setTileType(x, y, -1);
        }
        return layers;
    }

    @Test
    void aNewMapIsEmptyAndNotLoadable() {
        assertEquals(0, map.mapWidth());
        assertEquals(0, map.mapHeight());
        assertEquals(new Vector2i(0, 0), map.getMapSize());
        assertFalse(map.internLoad());
        assertEquals(0, map.tilesVersion());
    }

    @Test
    void informationGivesTheSizeAndOneDefaultCategory() {
        map.setInformation(new Vector2i(5, 3), emptyLayers(2, 5, 3), null, null);

        assertEquals(5, map.mapWidth());
        assertEquals(3, map.mapHeight());
        assertArrayEquals(new String[]{DEFAULT}, map.getLayersCategories());
        assertEquals(0, map.getLayerCategoryFrom(DEFAULT));
        assertEquals(1, map.getLayerCategoryTo(DEFAULT));
    }

    @Test
    void emptyCategoryListIsTheDefaultCategoryToo() {
        map.setInformation(new Vector2i(1, 1), emptyLayers(3, 1, 1), new String[0], new int[0]);

        assertArrayEquals(new String[]{DEFAULT}, map.getLayersCategories());
        assertEquals(2, map.getLayerCategoryTo(DEFAULT));
    }

    @Test
    void categoriesCoverTheLayersFromTheirFirstOneToTheNextCategory() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(5, 2, 2), new String[]{"Back", "Front"}, new int[]{0, 3});

        assertEquals(0, map.getLayerCategoryFrom("Back"));
        assertEquals(2, map.getLayerCategoryTo("Back"));
        assertEquals(3, map.getLayerCategoryFrom("Front"));
        assertEquals(4, map.getLayerCategoryTo("Front"), "the last category goes to the last layer");
    }

    @Test
    void tileTypeIsReadInTheLayersOfTheCategory() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(4, 2, 2), new String[]{"Back", "Front"}, new int[]{0, 2});
        map.setTileType(0, 1, 0, 10);
        map.setTileType(2, 0, 1, 20);
        map.setTileType(3, 1, 1, 30);

        assertEquals(10, map.getTileType("Back", 0, 1, 0));
        assertEquals(-1, map.getTileType("Back", 1, 1, 0));
        assertEquals(20, map.getTileType("Front", 0, 0, 1), "the first layer of Front is the layer 2");
        assertEquals(30, map.getTileType("Front", 1, 1, 1));
    }

    @Test
    void aLayerBeyondTheCategoryGivesAnEmptyTile() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(4, 2, 2), new String[]{"Back", "Front"}, new int[]{0, 2});

        assertEquals(-1, map.getTileType("Back", 2, 0, 0));
        assertEquals(-1, map.getTileType("Front", 5, 0, 0));
    }

    @Test
    void layersArrayIsCopiedButTheLayersAreShared() {
        TileLayer[] layers = emptyLayers(2, 2, 2);
        map.setInformation(new Vector2i(2, 2), layers, null, null);

        layers[0] = null;
        layers[1].setTileType(0, 0, 7);

        assertEquals(-1, map.getTileType(DEFAULT, 0, 0, 0));
        assertEquals(7, map.getTileType(DEFAULT, 1, 0, 0));
    }

    @Test
    void nonEmptyTilesAreCountedInTheCategory() {
        map.setInformation(new Vector2i(3, 2), emptyLayers(3, 3, 2), new String[]{"Back", "Front"}, new int[]{0, 2});
        assertEquals(0, map.numberOfNonEmptyTile("Back"));

        map.setTileType(0, 0, 0, 1);
        map.setTileType(0, 2, 1, 0);
        map.setTileType(1, 1, 1, 5);
        map.setTileType(2, 0, 0, 9);

        assertEquals(3, map.numberOfNonEmptyTile("Back"));
        assertEquals(1, map.numberOfNonEmptyTile("Front"));
    }

    @Test
    void mapSizeIsACopy() {
        map.setInformation(new Vector2i(5, 3), emptyLayers(1, 5, 3), null, null);
        map.getMapSize().set(99, 99);
        assertEquals(5, map.mapWidth());
    }

    @Test
    void changesOfAMapNotLoadedYetAreNotCounted() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(1, 2, 2), null, null);

        map.setTileType(0, 1, 1, 4);

        assertEquals(4, map.getTileType(DEFAULT, 0, 1, 1));
        assertEquals(0, map.tilesVersion());
        assertEquals(0, map.changedTiles());
    }

    @Test
    void theVersionIncreasesAtEachTileChanged() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(2, 2, 2), null, null);
        DataTypeTestAccess.markLoaded(map);
        int version = map.tilesVersion();

        map.setTileType(0, 1, 0, 4);
        assertEquals(version + 1, map.tilesVersion());
        map.setTileType(1, 0, 1, 8);
        map.setTileType(0, 1, 0, 6);

        assertEquals(version + 3, map.tilesVersion());
        assertEquals(3, map.changedTiles());
    }

    @Test
    void settingTheSameTileIsNotAChange() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(1, 2, 2), null, null);
        DataTypeTestAccess.markLoaded(map);
        int version = map.tilesVersion();

        map.setTileType(0, 0, 0, -1);

        assertEquals(version, map.tilesVersion());
        assertEquals(0, map.changedTiles());
    }

    @Test
    void reloadCopyIsAMapOfTheSameSource() {
        TileMap copy = (TileMap) map.createReloadCopy();

        assertNotSame(map, copy);
        assertEquals("map", copy.getDataName());
        assertEquals("map.tilemap", copy.path());
    }

    // Problems of the map sent during the action
    private static List<LogEntry> problemsOf(Runnable action) {
        List<LogEntry> entries = new CopyOnWriteArrayList<>();
        ILogListener listener = entries::add;
        Logger.AddListener(listener);
        try {
            OutputCapture.errorsOf(action);
        } finally {
            Logger.RemoveListener(listener);
        }
        return entries.stream().filter(entry -> entry.message().startsWith("map reloaded")).collect(Collectors.toList());
    }

    @Test
    void reloadExchangesTheContentAndReportsTheChangesMadeByTheCode() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(1, 2, 2), null, null);
        DataTypeTestAccess.markLoaded(map);
        map.setTileType(0, 0, 0, 3);
        map.setTileType(0, 1, 0, 3);
        int version = map.tilesVersion();
        TileMap newer = new TileMap("map", "map.tilemap");
        newer.setInformation(new Vector2i(4, 5), emptyLayers(2, 4, 5), new String[]{"Back", "Front"}, new int[]{0, 1});

        List<LogEntry> warnings = problemsOf(() -> map.swapContent(newer));

        assertEquals(4, map.mapWidth());
        assertEquals(5, map.mapHeight());
        assertArrayEquals(new String[]{"Back", "Front"}, map.getLayersCategories());
        assertEquals(1, map.getLayerCategoryFrom("Front"));
        assertEquals(-1, map.getTileType("Back", 0, 0, 0), "the file wins");
        assertEquals(2, newer.mapWidth(), "the copy gets the previous version");
        assertTrue(map.tilesVersion() > version, "a reload is a new version of the tiles");
        assertEquals(0, map.changedTiles());

        assertEquals(1, warnings.size(), warnings.toString());
        assertEquals(ELogLevel.WARNING, warnings.get(0).level());
        assertEquals(Logger.CATEGORY_RESOURCES, warnings.get(0).category());
        assertEquals("map reloaded: 2 tiles changed by the code are lost", warnings.get(0).message());
    }

    @Test
    void aReloadWithoutChangeReportsNothing() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(1, 2, 2), null, null);
        DataTypeTestAccess.markLoaded(map);
        int version = map.tilesVersion();
        TileMap newer = new TileMap("map", "map.tilemap");
        newer.setInformation(new Vector2i(2, 2), emptyLayers(1, 2, 2), null, null);

        assertTrue(problemsOf(() -> map.swapContent(newer)).isEmpty());
        assertTrue(map.tilesVersion() > version);
    }

    @Test
    void unloadEmptiesTheMap() {
        map.setInformation(new Vector2i(2, 2), emptyLayers(1, 2, 2), null, null);

        map.internUnload();

        assertEquals(0, map.mapWidth());
        assertEquals(0, map.mapHeight());
    }

    @Test
    void theAtlasTextureIsTheGLElementOfTheMap() {
        Class<? extends GLElement> type = map.glElementType();
        assertEquals(Texture.class, type);
        assertEquals("tilemap:map", map.glElementName());
    }
}
