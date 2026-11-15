package io.github.mightycode.mightylib.resources.map;

import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

// Which tileset a tile id belongs to (ids are given by the map: the first id of each tileset is its start id)
class TileSetAtlasTest {
    private static TileSet tileSet(String name, int tilesX, int tilesY) throws ReflectiveOperationException {
        TileSet tileSet = new TileSet(name, name + ".tileset");
        Field field = TileSet.class.getDeclaredField("tileNumber");
        field.setAccessible(true);
        ((Vector2i) field.get(tileSet)).set(tilesX, tilesY);
        return tileSet;
    }

    private final TileSetAtlas atlas = new TileSetAtlas("map");

    @Test
    void emptyAtlasKnowsNoTile() {
        assertEquals(0, atlas.getNumberTileset());
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(0));
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(-1));
    }

    @Test
    void eachTileIdIsRelatedToTheTileSetThatOwnsIt() throws ReflectiveOperationException {
        atlas.addTileSet(tileSet("a", 2, 2), 0);   // ids 0 to 3
        atlas.addTileSet(tileSet("b", 3, 1), 4);   // ids 4 to 6

        assertEquals(2, atlas.getNumberTileset());
        for (int id = 0; id <= 3; ++id)
            assertEquals(0, atlas.getTileSetIndexRelatedTo(id), "id " + id);
        for (int id = 4; id <= 6; ++id)
            assertEquals(1, atlas.getTileSetIndexRelatedTo(id), "id " + id);
    }

    @Test
    void anIdOutsideEveryTileSetIsNotRelatedToAny() throws ReflectiveOperationException {
        atlas.addTileSet(tileSet("a", 2, 2), 0);
        atlas.addTileSet(tileSet("b", 3, 1), 4);

        assertEquals(-1, atlas.getTileSetIndexRelatedTo(-1), "empty tile");
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(7), "after the last tileset");
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(1000));
    }

    @Test
    void aGapBetweenTwoTileSetsBelongsToNone() throws ReflectiveOperationException {
        atlas.addTileSet(tileSet("a", 2, 2), 0);   // ids 0 to 3
        atlas.addTileSet(tileSet("b", 3, 1), 10);  // ids 10 to 12

        assertEquals(0, atlas.getTileSetIndexRelatedTo(3));
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(5));
        assertEquals(1, atlas.getTileSetIndexRelatedTo(10));
        assertEquals(1, atlas.getTileSetIndexRelatedTo(12));
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(13));
    }

    @Test
    void rotationsAndFlipsTakeIdsOfTheTileSet() throws ReflectiveOperationException {
        TileSet rotated = tileSet("a", 2, 1);
        rotated.setTileParameters(true, false);
        atlas.addTileSet(rotated, 0);            // 2 tiles * 4 rotations: ids 0 to 7
        atlas.addTileSet(tileSet("b", 1, 1), 8);

        assertEquals(0, atlas.getTileSetIndexRelatedTo(7));
        assertEquals(1, atlas.getTileSetIndexRelatedTo(8));
    }

    @Test
    void accessorsByIndex() throws ReflectiveOperationException {
        TileSet first = tileSet("a", 2, 3), second = tileSet("b", 4, 5);
        atlas.addTileSet(first, 0);
        atlas.addTileSet(second, 6);

        assertSame(first, atlas.getTileSet(0));
        assertSame(second, atlas.getTileSet(1));
        assertEquals(0, atlas.getStartId(0));
        assertEquals(6, atlas.getStartId(1));
        assertEquals(new Vector2i(4, 5), atlas.getTileSetSize(1));
    }

    @Test
    void clearForgetsTheTileSets() throws ReflectiveOperationException {
        atlas.addTileSet(tileSet("a", 2, 2), 0);

        atlas.clear();

        assertEquals(0, atlas.getNumberTileset());
        assertEquals(-1, atlas.getTileSetIndexRelatedTo(0));
    }

    @Test
    void theTextureOfTheAtlasIsNamedAfterTheMap() {
        assertEquals("tilemap:map", atlas.getTextureAtlas().getDataName());
    }
}
