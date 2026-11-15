package io.github.mightycode.mightylib.resources.map;

import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TileLayerTest {
    @Test
    void tilesAreStoredByColumnAndRow() {
        TileLayer layer = new TileLayer(new Vector2i(3, 2));

        layer.setTileType(2, 1, 7);
        layer.setTileType(0, 0, 4);

        assertEquals(7, layer.getTile(2, 1));
        assertEquals(4, layer.getTile(0, 0));
        assertEquals(0, layer.getTile(1, 0), "a new layer is filled with the tile 0");
    }

    @Test
    void sizeIsCopiedAtCreationAndOnReading() {
        Vector2i size = new Vector2i(3, 2);
        TileLayer layer = new TileLayer(size);

        size.set(10, 10);
        layer.mapSize().set(99, 99);

        assertEquals(new Vector2i(3, 2), layer.mapSize());
    }

    @Test
    void outsideTheLayerIsAnError() {
        TileLayer layer = new TileLayer(new Vector2i(3, 2));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> layer.getTile(3, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> layer.getTile(0, 2));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> layer.setTileType(-1, 0, 1));
    }
}
