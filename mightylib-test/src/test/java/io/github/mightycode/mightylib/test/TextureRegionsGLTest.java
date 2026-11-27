package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.game.TileMapRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.resources.animation.FrameData;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.geometry.EFlip;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

/*
 * The regions of a texture limited in the shader: the center frame of a sheet whose frames touch each other (3 x 5
 * texels, a sheet of 9 x 15: not a power of two), red border and green inside, every other frame blue, drawn x8 with
 * its edges on centers of pixels (where a fragment on the edge read the texel of the neighbor frame) by each way that
 * sets a region (Animation2DRenderer, TileMapRenderer, RectangleRenderer.setTextureRegion, DrawBatch2D.drawRegion),
 * flipped or not, in every mode of the batch: no blue (never a texel of a neighbor frame), 24 x 40 pixels drawn,
 * each row and column of the frame 8 pixels wide (+-1: the boundaries between its texels fall on centers of pixels
 * too, rounded to either side; the half texel inset of before drew the outer ones 4 pixels wide), every mode the
 * picture of the mode OFF.
 */
@MightySceneTest(config = TextureRegionsGLTest.Config.class, sources = "src/test/resources/texture-regions")
class TextureRegionsGLTest {
    // The engine of the kit tests with the loaders of the tile maps
    static final class Config implements Supplier<EngineConfig> {
        @Override
        public EngineConfig get() {
            return new SceneRunnerTest.Config().get().addPlugin(new IPlugin() {
                @Override
                public String namespace() {
                    return "textureregions";
                }

                @Override
                public void register(EngineRegistry registry) {
                    registry.resources().add(new TileSetLoader()).add(new TileMapLoader());
                }
            });
        }
    }

    private static final int SCALE = 8;
    private static final int FRAME_WIDTH = 3, FRAME_HEIGHT = 5;
    private static final int SHEET_WIDTH = 9, SHEET_HEIGHT = 15;
    // The center frame of the sheet: left, right, top, bottom in texture coordinates
    private static final Vector4f FRAME = new Vector4f(3f / SHEET_WIDTH, 6f / SHEET_WIDTH, 5f / SHEET_HEIGHT,
            10f / SHEET_HEIGHT);
    private static final int DRAWS = 6;
    // Top left corner of the draw i: on the center of a pixel
    private static final float FIRST_X = 20.5f, STEP_X = 40f, TOP = 30.5f;

    @Test
    void theFramesOfAPixelArtSheetAreReadWhole(SceneRunner runner) {
        check(runner, "regionsPixel", false);
    }

    @Test
    void theFramesOfALinearSheetAreReadWhole(SceneRunner runner) {
        check(runner, "regionsLinear", true);
    }

    private static void check(SceneRunner runner, String name, boolean linear) {
        inScene(runner, scene -> {
            runner.engine().loadUntilDone();
            AnimationData data = centerFrame(name);
            try (Views.Scope ignored = Views.enter(scene)) {
                Views.engine().resources().addPreLoadedResource(data);
            }
            runner.engine().loadUntilDone();

            List<Renderer> renderers = new ArrayList<>();
            Material material;
            try (Views.Scope ignored = Views.enter(scene)) {
                for (int flip = 0; flip < 2; ++flip) {
                    Animator animator = new Animator();
                    animator.addAndInitAnimation("frame", data, true);
                    Animation2DRenderer animation = new Animation2DRenderer(EngineResources.TEXTURE_2D);
                    animation.init(animator);
                    animation.setHorizontalFlip(flip == 1).setVerticalFlip(flip == 1);
                    animation.setScale(new Vector2f(SCALE, SCALE));
                    // The hot point (the top left corner) is mirrored by the flips: the bottom right corner
                    animation.setPosition(new Vector2f(left(flip) + flip * FRAME_WIDTH * SCALE,
                            TOP + flip * FRAME_HEIGHT * SCALE));
                    renderers.add(animation);
                }

                TileMapRenderer tiles = new TileMapRenderer(EngineResources.TEXTURE_2D, false, "Background");
                tiles.setTilemap(Views.engine().resources().getResource(TileMap.class, name));
                tiles.setScale(new Vector3f(SCALE, SCALE, 1));
                tiles.setPosition(new Vector3f(left(2), TOP, 0));
                renderers.add(tiles);

                for (int flip = 0; flip < 2; ++flip) {
                    RectangleRenderer rectangle = new RectangleRenderer(EngineResources.TEXTURE_2D);
                    rectangle.setMainTextureChannel(name);
                    rectangle.setTextureRegion(FRAME);
                    rectangle.setTextureFlip(flip == 1 ? EFlip.HorizontalVertical : EFlip.None);
                    rectangle.setSizePix(FRAME_WIDTH * SCALE, FRAME_HEIGHT * SCALE);
                    rectangle.setPosition(new Vector2f(left(3 + flip), TOP));
                    renderers.add(rectangle);
                }
                material = ((RectangleRenderer) renderers.get(renderers.size() - 1)).material();
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                try {
                    Runnable drawing = () -> {
                        renderers.forEach(Renderer::display);
                        // drawRegion takes (u0, v0, u1, v1)
                        assertTrue(scene.batch2D().drawRegion(material, new Vector2f(left(5), TOP),
                                new Vector2f(FRAME_WIDTH * SCALE, FRAME_HEIGHT * SCALE), null, 0f,
                                new Vector4f(FRAME.x, FRAME.z, FRAME.y, FRAME.w), EFlip.Horizontal, null));
                    };
                    checkEveryMode(scene, capture, drawing, linear);
                } finally {
                    renderers.forEach(Renderer::unload);
                    Views.engine().resources().deleteResource(data);
                }
            }
        });
    }

    private static float left(int draw) {
        return FIRST_X + draw * STEP_X;
    }

    // An animation of the center frame of the sheet, its hot point at its top left corner
    private static AnimationData centerFrame(String texture) {
        FrameData frame = new FrameData();
        frame.init(FRAME_WIDTH + "," + FRAME_HEIGHT + "," + FRAME_WIDTH + "," + FRAME_HEIGHT + ",0,0,1", "test", 1);
        AnimationData data = new AnimationData("regionFrame-" + texture, null).setTexture(texture)
                .setFramesData(new FrameData[]{frame});
        data.setPreloaded();
        return data;
    }

    // Each mode: the frames whole, the picture of the mode OFF
    private static void checkEveryMode(Scene scene, Capture capture, Runnable drawing, boolean linear) {
        Engine engine = Views.engine();
        engine.setBatch2DMode(EBatch2DMode.OFF);
        ByteBuffer alone = capture.draw(scene, drawing);
        checkFrames(capture, alone, linear, EBatch2DMode.OFF);
        for (EBatch2DMode mode : EBatch2DMode.values()) {
            if (mode == EBatch2DMode.OFF)
                continue;

            engine.setBatch2DMode(mode);
            ByteBuffer batched = capture.draw(scene, drawing);
            assertTrue(scene.batch2D().lastFrameDraws() > 0, mode + ": nothing batched");
            checkFrames(capture, batched, linear, mode);
            capture.assertSame(alone, batched, BATCHED, mode.toString());
        }
    }

    private static void checkFrames(Capture capture, ByteBuffer picture, boolean linear, EBatch2DMode mode) {
        CapturedImage image = capture.image(picture);
        for (int draw = 0; draw < DRAWS; ++draw)
            checkFrame(image, draw, linear, mode + ", draw " + draw);
    }

    // The frame of the draw: only its colors around it, 24 x 40 pixels drawn, rows and columns of 8 pixels
    private static void checkFrame(CapturedImage image, int draw, boolean linear, String what) {
        int left = (int) left(draw), top = (int) TOP;
        int width = FRAME_WIDTH * SCALE, height = FRAME_HEIGHT * SCALE;
        // A pixel more on each side: the fragments on the edges are drawn or not (rule of the rasterizer)
        for (int y = top - 1; y <= top + height + 1; ++y) {
            for (int x = left - 1; x <= left + width + 1; ++x) {
                int rgb = image.rgb(x, y);
                assertTrue(isBackground(rgb) || isFrame(rgb, linear),
                        what + ": pixel " + x + ", " + y + " is " + Integer.toHexString(rgb)
                                + ", not a color of the frame (a texel of a neighbor frame)");
            }
        }

        // The middle column crosses the rows (red border, green inside), the middle row the columns
        int[] rows = runs(image, left + width / 2, top - 1, 0, 1, height + 3);
        int[] columns = runs(image, left - 1, top + height / 2, 1, 0, width + 3);
        assertRuns(new int[]{SCALE, 3 * SCALE, SCALE}, rows, what + ", rows");
        assertRuns(new int[]{SCALE, SCALE, SCALE}, columns, what + ", columns");
    }

    // Lengths of the runs of red then green then red pixels along a line (the background skipped)
    private static int[] runs(CapturedImage image, int x, int y, int dx, int dy, int count) {
        List<Integer> lengths = new ArrayList<>();
        int previous = -1;
        for (int i = 0; i < count; ++i) {
            int rgb = image.rgb(x + i * dx, y + i * dy);
            if (isBackground(rgb))
                continue;

            int kind = red(rgb) > green(rgb) ? 0 : 1;
            if (kind == previous)
                lengths.set(lengths.size() - 1, lengths.get(lengths.size() - 1) + 1);
            else
                lengths.add(1);
            previous = kind;
        }

        return lengths.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void assertRuns(int[] expected, int[] actual, String what) {
        assertEquals(expected.length, actual.length, what + ": " + java.util.Arrays.toString(actual));
        int total = 0, expectedTotal = 0;
        for (int i = 0; i < expected.length; ++i) {
            // The boundaries between texels on centers of pixels: either side
            assertTrue(Math.abs(expected[i] - actual[i]) <= 1,
                    what + ": " + java.util.Arrays.toString(actual) + " instead of "
                            + java.util.Arrays.toString(expected));
            total += actual[i];
            expectedTotal += expected[i];
        }
        assertEquals(expectedTotal, total, what + ": pixels drawn");
    }

    private static boolean isBackground(int rgb) {
        return Math.abs(red(rgb) - 51) <= 2 && Math.abs(green(rgb) - 77) <= 2 && Math.abs(blue(rgb) - 102) <= 2;
    }

    // Pixel art: red or green; linear: a mix of them, never blue
    private static boolean isFrame(int rgb, boolean linear) {
        if (blue(rgb) > 2)
            return false;
        if (linear)
            return true;

        return (red(rgb) >= 253 && green(rgb) <= 2) || (green(rgb) >= 253 && red(rgb) <= 2);
    }

    private static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    private static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    private static int blue(int rgb) {
        return rgb & 0xFF;
    }
}
