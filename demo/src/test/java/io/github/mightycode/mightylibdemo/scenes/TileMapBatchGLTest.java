package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer.RenderState;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylibdemo.main.DemoTestConfig;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;

/**
 * The tile map of Scene2D in the 2D batch (only its visible tiles written): the same picture as drawn alone, whatever
 * the place, zoom and rotation of the camera.
 */
@MightySceneTest(config = DemoTestConfig.class)
class TileMapBatchGLTest {
    // The picture of the drawing in a framebuffer of the size of the camera of the scene (also BatchStress2DGLTest)
    static ByteBuffer capture(Scene scene, Runnable drawing) {
        Vector2i size = scene.context().getWindow().getInfo().getVirtualSizeCopy();
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, size.x, size.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        GLContext.setViewport(0, 0, size.x, size.y);
        RenderState.invalidateContextState();

        glClearColor(0.2f, 0.3f, 0.4f, 1f);
        GLContext.ClearColorAndDepth();
        drawing.run();
        scene.batch2D().endView();
        ByteBuffer pixels = BufferUtils.createByteBuffer(size.x * size.y * 4);
        glReadPixels(0, 0, size.x, size.y, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(framebuffer);
        GLContext.forgetTexture(texture);
        glDeleteTextures(texture);
        GLContext.setViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        RenderState.invalidateContextState();
        return pixels;
    }

    // Channels differing by more than 1 (the rounding of the vertices may differ between the two ways)
    static int countDifferent(ByteBuffer expected, ByteBuffer actual) {
        int different = 0;
        for (int i = 0; i < expected.capacity(); ++i) {
            if (Math.abs((expected.get(i) & 0xFF) - (actual.get(i) & 0xFF)) > 1)
                ++different;
        }
        return different;
    }

    // Pixels not of the clear color of capture
    static int drawnPixels(ByteBuffer picture) {
        int drawn = 0;
        for (int i = 0; i < picture.capacity(); i += 4) {
            if (Math.abs((picture.get(i) & 0xFF) - 51) > 2 || Math.abs((picture.get(i + 1) & 0xFF) - 77) > 2
                    || Math.abs((picture.get(i + 2) & 0xFF) - 102) > 2)
                ++drawn;
        }
        return drawn;
    }

    @Test
    void theVisibleTilesAreTheMapDrawnAlone(SceneRunner runner) {
        runner.start("Scene2D").untilLoaded();
        runner.frames(5);
        Scene scene = runner.scene();

        try (Views.Scope ignored = Views.enter(scene)) {
            TileMap tileMap = Views.engine().resources().getResource(TileMap.class, "map");
            LayersTileMapRenderer map = new LayersTileMapRenderer("texture2D", false);
            map.setTileMap(tileMap);
            Vector2i tile = tileMap.tileSetAtlas().getTileSize();
            Vector2f mapSize = new Vector2f(tileMap.mapWidth() * tile.x, tileMap.mapHeight() * tile.y);
            // Tiles of the map drawn or left out: the same in every view and mode (set by the first sample)
            double[] tileCount = {-1};
            Vector2i screen = scene.context().getWindow().getInfo().getVirtualSizeCopy();
            Camera2D camera = scene.camera2D();
            Vector2f position = camera.getCamPosCopy();
            Vector2f zoom = camera.getZoomLevel();
            float rotation = camera.getRotation();
            try {
                // The point of the map (in parts of its size) at the center of the screen, zoom, rotation: the whole
                // map, a part of it zoomed in (most of its tiles out of the view), turned, its corner, then out of it
                float[][] views = {{0.5f, 0.5f, 0.5f, 0}, {0.5f, 0.5f, 16f, 0}, {0.3f, 0.6f, 10f, 0.6f},
                        {0f, 1f, 8f, -0.3f}, {-30f, -30f, 1, 0}};
                for (float[] view : views) {
                    // screen = zoom * (rotation * world - position): the point at the center of the screen
                    camera.setRotation(view[3]);
                    camera.setZoomLevel(view[2]);
                    float x = view[0] * mapSize.x, y = view[1] * mapSize.y;
                    float cos = (float) Math.cos(view[3]), sin = (float) Math.sin(view[3]);
                    camera.setPos(new Vector2f(cos * x - sin * y - screen.x * 0.5f / view[2],
                            sin * x + cos * y - screen.y * 0.5f / view[2]));

                    Views.engine().setBatch2DMode(EBatch2DMode.OFF);
                    ByteBuffer alone = capture(scene, map::display);
                    // Tiles on the screen, none out of the map
                    int drawn = drawnPixels(alone);
                    System.out.println("--Tile map at " + view[0] + ", " + view[1] + " zoom " + camera.getZoomLevel().x + ": "
                            + drawn + " pixels drawn");
                    if (view[0] < -1)
                        assertEquals(0, drawn);
                    else
                        assertTrue(drawn > 1000, drawn + " pixels drawn");

                    for (EBatch2DMode mode : new EBatch2DMode[]{EBatch2DMode.ORPHAN, EBatch2DMode.RING,
                            EBatch2DMode.INSTANCED}) {
                        Views.engine().setBatch2DMode(mode);
                        EngineStats.endSecond(1, 1, 0, 0);
                        ByteBuffer batched = capture(scene, map::display);
                        // The tiles written and those left out are the tiles of the map
                        EngineStats.endSecond(1, 1, 0, 0);
                        JSONObject stats = EngineStats.toJson();
                        double total = stats.getDouble("tilesCulledPerFrame") + stats.getDouble("tilesDrawnPerFrame");
                        if (tileCount[0] < 0)
                            tileCount[0] = total;
                        assertTrue(total > 0, mode + ": " + total + " tiles");
                        assertEquals(tileCount[0], total, 1.0, mode.toString());
                        // Out of the map: every tile left out; in view: some written
                        if (view[0] < -1)
                            assertEquals(0, stats.getDouble("tilesDrawnPerFrame"), mode.toString());
                        else
                            assertTrue(stats.getDouble("tilesDrawnPerFrame") > 0, mode.toString());
                        int different = countDifferent(alone, batched);
                        assertTrue(different <= alone.capacity() / 2000,
                                mode + " at " + view[0] + ", " + view[1] + " zoom " + view[2] + " rotation " + view[3]
                                        + ": " + different + " channels differ");
                    }
                }
            } finally {
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                camera.setPos(position);
                camera.setZoomLevel(zoom);
                camera.setRotation(rotation);
                map.unload();
            }
        }
    }
}
