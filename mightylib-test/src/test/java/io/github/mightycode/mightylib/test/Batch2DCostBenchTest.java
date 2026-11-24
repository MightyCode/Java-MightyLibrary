package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.Shape2DRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.BiFunction;

import static io.github.mightycode.mightylib.test.BatchPictures.*;

/*
 * Bench of the CPU cost of a draw of the 2D batch with the whole path: Renderer.display, submitToBatch, state,
 * culling and writing, for N renderers of each kind in a view with the active batch. The time of the display() calls
 * and the time of the send of the batch (sort, upload, draw calls, the GL calls of the frame) are measured apart. Two
 * cases per kind: everything in the view, everything out of it (the camera moved away). Only with
 * -Dmightylib.bench=true (python tools/bench.py): it measures, it checks nothing. One JSON line per case: ns per
 * draw, median of the passes after a warm-up.
 */
@EnabledIfSystemProperty(named = "mightylib.bench", matches = "true")
@MightySceneTest(config = SceneRunnerTest.Config.class)
class Batch2DCostBenchTest {
    private static final int DRAWS = 16000;
    private static final int WARM_UP_PASSES = 100;
    private static final int PASSES = 31;
    private static final long SEED = 12345;

    // Makes the renderer of this index at a place of the screen (the camera at rest), the size in pixels
    private interface Maker extends BiFunction<Pattern, Random, Renderer> {
    }

    private static void place(Shape2DRenderer renderer, Random random, Vector2i screen) {
        float size = 8 + random.nextFloat() * 40;
        renderer.setSizePix(size, size * (0.5f + random.nextFloat()));
        renderer.setReferenceDirection(EDirection.None);
        renderer.setPosition(new Vector2f(random.nextFloat() * screen.x, random.nextFloat() * screen.y));
        renderer.setRotationZ(random.nextFloat() * 6.28f);
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    // The batch programs of the shapes load in the background: frames to let them arrive
    private static void drawOnce(SceneRunner runner, Scene scene, List<Renderer> renderers) {
        runner.frames(8);
        try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
            Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
            capture.draw(scene, () -> renderers.forEach(Renderer::display));
        }
        runner.frames(8);
    }

    private void bench(SceneRunner runner, String kind, Maker maker) {
        PatternHolder holder = new PatternHolder();
        inScene(runner, scene -> {
            List<Renderer> renderers = new ArrayList<>();
            Vector2i screen = scene.context().getWindow().getInfo().getVirtualSizeCopy();
            try (Views.Scope ignored = Views.enter(scene)) {
                holder.pattern = new Pattern();
                Random random = new Random(SEED);
                for (int i = 0; i < DRAWS; ++i) {
                    Renderer renderer = maker.apply(holder.pattern, random);
                    if (renderer instanceof Shape2DRenderer)
                        place((Shape2DRenderer) renderer, random, screen);
                    renderers.add(renderer);
                }
            }
            // Text and animations are placed by their maker
            drawOnce(runner, scene, renderers);

            Camera2D camera = scene.camera2D();
            Vector2f position = camera.getCamPosCopy();
            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                DrawBatch2D batch = scene.batch2D();
                for (int view = 0; view < 2; ++view) {
                    camera.setPos(view == 0 ? position : new Vector2f(position).add(1_000_000f, 1_000_000f));
                    measure(scene, batch, renderers, kind + (view == 0 ? " visible" : " hidden"));
                }
            } finally {
                camera.setPos(position);
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                renderers.forEach(Renderer::unload);
                holder.pattern.delete();
            }
        });
    }

    private static final class PatternHolder {
        Pattern pattern;
    }

    private void measure(Scene scene, DrawBatch2D batch, List<Renderer> renderers, String name) {
        double[] displayNanos = new double[PASSES];
        double[] sendNanos = new double[PASSES];
        int drawn = 0, culled = 0;
        for (int pass = -WARM_UP_PASSES; pass < PASSES; ++pass) {
            long start = System.nanoTime();
            for (int i = 0, size = renderers.size(); i < size; ++i)
                renderers.get(i).display();
            long displayed = System.nanoTime();
            batch.endView();
            long sent = System.nanoTime();
            // The GPU catches up outside the measure
            GL11.glFinish();
            if (pass >= 0) {
                displayNanos[pass] = (displayed - start) / (double) renderers.size();
                sendNanos[pass] = (sent - displayed) / (double) renderers.size();
                drawn = batch.lastFrameDraws();
                culled = batch.lastFrameCulled();
            }
        }

        System.out.printf(Locale.ROOT, "{\"bench\":\"display\",\"case\":\"%s\",\"n\":%d,\"nsPerDraw\":%.1f,"
                        + "\"sendNsPerDraw\":%.1f,\"drawn\":%d,\"culled\":%d}%n", name, renderers.size(),
                median(displayNanos), median(sendNanos), drawn, culled);
    }

    @Test
    void benchTexturedRectangles(SceneRunner runner) {
        bench(runner, "textured rectangle", (pattern, random) -> {
            RectangleRenderer rectangle = new RectangleRenderer(EngineResources.TEXTURE_2D);
            rectangle.setMainTextureChannel(pattern);
            float x = random.nextInt(8) / 8f, y = random.nextInt(8) / 8f;
            rectangle.setTexturePosition(new Vector4f(x, y, x + 1f / 8, y + 1f / 8));
            return rectangle;
        });
    }

    // Three textured for one colored, in this order: the runs of the batch alternate (like BatchStress2D)
    @Test
    void benchMixedRectangles(SceneRunner runner) {
        int[] index = {0};
        bench(runner, "mixed rectangle", (pattern, random) -> {
            RectangleRenderer rectangle;
            if (index[0]++ % 4 == 3) {
                rectangle = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
                rectangle.setColorMode(new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f));
            } else {
                rectangle = new RectangleRenderer(EngineResources.TEXTURE_2D);
                rectangle.setMainTextureChannel(pattern);
                float x = random.nextInt(8) / 8f, y = random.nextInt(8) / 8f;
                rectangle.setTexturePosition(new Vector4f(x, y, x + 1f / 8, y + 1f / 8));
            }
            return rectangle;
        });
    }

    @Test
    void benchColoredRectangles(SceneRunner runner) {
        bench(runner, "colored rectangle", (pattern, random) -> {
            RectangleRenderer rectangle = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
            rectangle.setColorMode(new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f));
            return rectangle;
        });
    }

    @Test
    void benchAnimations(SceneRunner runner) {
        bench(runner, "animation", (pattern, random) -> {
            Animation2DRenderer animation = new Animation2DRenderer(EngineResources.TEXTURE_2D);
            animation.setMainTextureChannel(pattern);
            float size = 8 + random.nextFloat() * 40;
            animation.setScale(new Vector3f(size, size * (0.5f + random.nextFloat()), 1));
            animation.setPosition(new Vector2f(random.nextFloat() * 1000, random.nextFloat() * 700));
            animation.setRotationZ(random.nextFloat() * 6.28f);
            return animation;
        });
    }

    @Test
    void benchShapes(SceneRunner runner) {
        bench(runner, "shape", (pattern, random) -> {
            CircleRenderer circle = new CircleRenderer();
            circle.setColorMode(new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f));
            return circle;
        });
    }

    @Test
    void benchShapesWithStroke(SceneRunner runner) {
        bench(runner, "shape with stroke", (pattern, random) -> {
            RoundedRectangleRenderer shape = new RoundedRectangleRenderer().setCornerRadius(4);
            shape.setColorMode(new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f));
            shape.setStrokeWidth(2);
            shape.setStrokeColor(new Color4f(1f, 1f, 1f, 1f));
            return shape;
        });
    }

    @Test
    void benchTexts(SceneRunner runner) {
        bench(runner, "text", (pattern, random) -> {
            Text text = new Text();
            text.setFontSize(12 + random.nextInt(12));
            text.setColor(new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f));
            text.setText("Text " + random.nextInt(1000));
            text.setPosition(new Vector2f(random.nextFloat() * 1000, random.nextFloat() * 700));
            text.setRotationZ(random.nextFloat() * 6.28f);
            return text;
        });
    }
}
