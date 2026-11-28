package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.graphics.game.TileMapRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.test.BatchPictures.Capture;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * A map of 3 x 2 tiles of one texel (red, green, blue tiles), drawn x16 from (16, 16), in two categories of layers:
 * Back (5 red tiles, the bottom right one empty) then Front (a blue tile at (1, 0)). The names are those a hash map
 * orders the other way: the categories are built and drawn in the order of the map (a HashMap drew Back over Front). A tile changed in the map is
 * drawn after the update of its renderers, an emptied one disappears.
 */
@MightySceneTest(config = TextureRegionsGLTest.Config.class, sources = "src/test/resources/tile-maps")
class TileMapLayersGLTest {
    private static final int SCALE = 16, LEFT = 16, TOP = 16;
    private static final int RED = 0xFF0000, GREEN = 0x00FF00, BLUE = 0x0000FF, CLEAR = 0x334D66;
    private static final int TILE_INDICES = 6;

    // The map loaded, the renderer of its layers placed, its atlas texture sent by a few frames, then the test; the
    // renderer unloaded after it
    private static void withLayers(SceneRunner runner, Consumer<Fixture> test) {
        inScene(runner, scene -> {
            runner.engine().loadUntilDone();
            LayersTileMapRenderer layers = new LayersTileMapRenderer(EngineResources.TEXTURE_2D, true);
            TileMap map;
            try (Views.Scope ignored = Views.enter(scene)) {
                map = Views.engine().resources().getResource(TileMap.class, "layers");
                layers.setTileMap(map);
                for (String category : layers.getCategories()) {
                    TileMapRenderer renderer = layers.getTileMapRenderer(category);
                    renderer.setScale(new Vector3f(SCALE, SCALE, 1));
                    renderer.setPosition(new Vector3f(LEFT, TOP, 0));
                }
            }
            try {
                runner.frames(5);
                try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                    test.accept(new Fixture(scene, map, layers, capture));
                }
            } finally {
                try (Views.Scope ignored = Views.enter(scene)) {
                    layers.unload();
                }
            }
        });
    }

    private static final class Fixture {
        final Scene scene;
        final TileMap map;
        final LayersTileMapRenderer layers;
        final Capture capture;

        Fixture(Scene scene, TileMap map, LayersTileMapRenderer layers, Capture capture) {
            this.scene = scene;
            this.map = map;
            this.layers = layers;
            this.capture = capture;
        }

        ByteBuffer draw(Runnable drawing) {
            return capture.draw(scene, drawing);
        }

        // The color at the center of the tile (x, y), 0xRRGGBB
        int tile(ByteBuffer picture, int x, int y) {
            int pixelX = LEFT + x * SCALE + SCALE / 2;
            int pixelY = TOP + y * SCALE + SCALE / 2;
            int index = (pixelX + (capture.size.y - 1 - pixelY) * capture.size.x) * 4;
            return ((picture.get(index) & 0xFF) << 16) | ((picture.get(index + 1) & 0xFF) << 8)
                    | (picture.get(index + 2) & 0xFF);
        }

        int indices(String category) {
            return layers.getTileMapRenderer(category).getMesh().getIndexCount();
        }
    }

    private static void assertColor(int expected, int actual, String what) {
        for (int shift = 0; shift <= 16; shift += 8) {
            int difference = Math.abs(((expected >> shift) & 0xFF) - ((actual >> shift) & 0xFF));
            if (difference > 2)
                assertEquals(String.format("%06X", expected), String.format("%06X", actual), what);
        }
    }

    @Test
    void eachCategoryHoldsTheNonEmptyTilesOfItsLayersInTheOrderOfTheMap(SceneRunner runner) {
        withLayers(runner, fixture -> {
            assertEquals(Arrays.asList("Back", "Front"), new ArrayList<>(fixture.layers.getCategories()));
            assertEquals(5 * TILE_INDICES, fixture.indices("Back"));
            assertEquals(TILE_INDICES, fixture.indices("Front"));
            assertEquals(fixture.map, fixture.layers.getTileMapRenderer("Back").getTileMap());
        });
    }

    // Front over Back, in every mode of the batch; one category alone draws only its tiles
    @Test
    void theCategoriesAreDrawnInTheOrderOfTheMap(SceneRunner runner) {
        withLayers(runner, fixture -> {
            for (EBatch2DMode mode : EBatch2DMode.values()) {
                Views.engine().setBatch2DMode(mode);
                ByteBuffer all = fixture.draw(fixture.layers::display);
                assertColor(BLUE, fixture.tile(all, 1, 0), mode + ": Front over Back");
                assertColor(RED, fixture.tile(all, 0, 0), mode + ": Back");
                assertColor(CLEAR, fixture.tile(all, 2, 1), mode + ": empty tile");
            }

            ByteBuffer back = fixture.draw(() -> fixture.layers.display("Back"));
            assertColor(RED, fixture.tile(back, 1, 0), "Back alone");
            ByteBuffer front = fixture.draw(() -> fixture.layers.display("Front"));
            assertColor(BLUE, fixture.tile(front, 1, 0), "Front alone");
            assertColor(CLEAR, fixture.tile(front, 0, 0), "Front alone");
            // An unknown category draws nothing
            ByteBuffer none = fixture.draw(() -> fixture.layers.display("Middle"));
            assertColor(CLEAR, fixture.tile(none, 1, 0), "unknown category");
        });
    }

    // Tile types of the map: 0 the first tile of the tileset (red), -1 empty
    @Test
    void aChangedTileIsDrawnAfterTheUpdate(SceneRunner runner) {
        withLayers(runner, fixture -> {
            TileMap map = fixture.map;
            try {
                map.setTileType(0, 2, 1, 1);
                map.setTileType(0, 0, 0, 2);
                // Not yet: the renderers are built again by their update
                assertEquals(5 * TILE_INDICES, fixture.indices("Back"));
                fixture.layers.update();
                map.clearTileChanges();
                assertEquals(6 * TILE_INDICES, fixture.indices("Back"));
                assertEquals(TILE_INDICES, fixture.indices("Front"));
                ByteBuffer changed = fixture.draw(fixture.layers::display);
                assertColor(GREEN, fixture.tile(changed, 2, 1), "filled tile");
                assertColor(BLUE, fixture.tile(changed, 0, 0), "changed tile");
                assertColor(BLUE, fixture.tile(changed, 1, 0), "Front kept");

                map.setTileType(1, 1, 0, -1);
                fixture.layers.update();
                map.clearTileChanges();
                assertEquals(0, fixture.indices("Front"));
                ByteBuffer emptied = fixture.draw(fixture.layers::display);
                assertColor(RED, fixture.tile(emptied, 1, 0), "Back under the emptied Front tile");
            } finally {
                map.setTileType(0, 2, 1, -1);
                map.setTileType(0, 0, 0, 0);
                map.setTileType(1, 1, 0, 2);
                map.clearTileChanges();
            }
        });
    }

    // A map set again replaces the renderers (the previous ones unloaded: the kit checks the leaks around the test)
    @Test
    void aMapSetAgainReplacesItsRenderers(SceneRunner runner) {
        withLayers(runner, fixture -> {
            TileMapRenderer previous = fixture.layers.getTileMapRenderer("Back");
            fixture.layers.setTileMap(fixture.map);
            TileMapRenderer current = fixture.layers.getTileMapRenderer("Back");
            assertNotSame(previous, current, "a new renderer");
            assertEquals(5 * TILE_INDICES, current.getMesh().getIndexCount());
        });
    }
}
