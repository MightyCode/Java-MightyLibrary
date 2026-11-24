package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeCap;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PointsRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PolylineRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

/*
 * The view culling of the batched draws: rectangles, animations, texts and meshes all over and around the view, the
 * camera moved, zoomed and turned: the picture with the culling is the picture without it, pixel for pixel, and the
 * draws outside are culled.
 */
@MightySceneTest(config = SceneRunnerTest.Config.class)
class ViewCullingGLTest {
    private static final EDirection[] REFERENCES = {EDirection.LeftUp, EDirection.None, EDirection.RightDown,
            EDirection.Up};

    // A grid of renderers of each kind over twice the size of the screen, centered on it
    private static List<Renderer> picture(Pattern pattern, Vector2i screen) {
        List<Renderer> renderers = new ArrayList<>();
        int columns = 9, rows = 7;
        for (int row = 0; row < rows; ++row) {
            for (int column = 0; column < columns; ++column) {
                int i = row * columns + column;
                float x = -screen.x * 0.5f + column * screen.x * 2f / (columns - 1);
                float y = -screen.y * 0.5f + row * screen.y * 2f / (rows - 1);
                float angle = (i % 5) * 0.35f - 0.7f;
                switch (i % 4) {
                    case 0: {
                        RectangleRenderer sprite = new RectangleRenderer(EngineResources.TEXTURE_2D);
                        sprite.setMainTextureChannel(pattern);
                        sprite.setReferenceDirection(REFERENCES[i % REFERENCES.length]);
                        sprite.setSizePix(70 + i % 7 * 9, 50 + i % 5 * 11);
                        sprite.setPosition(new Vector2f(x, y));
                        sprite.setRotationZ(angle);
                        renderers.add(sprite);
                        break;
                    }
                    case 1: {
                        Animation2DRenderer animation = new Animation2DRenderer(EngineResources.TEXTURE_2D);
                        animation.setMainTextureChannel(pattern);
                        animation.setShiftRotation(REFERENCES[i % REFERENCES.length]);
                        animation.setScale(new Vector3f(80 + i % 6 * 10, 60, 1));
                        animation.setPosition(new Vector2f(x, y));
                        animation.setRotationZ(angle);
                        renderers.add(animation);
                        break;
                    }
                    case 2: {
                        Text text = new Text();
                        text.setFontSize(16 + i % 4 * 6);
                        text.setColor(new Color4f(1f, 0.9f - 0.1f * (i % 5), 0.3f, 1f));
                        text.setReference(REFERENCES[i % REFERENCES.length]);
                        text.setText("Culled " + i + " AaQq");
                        text.setPosition(new Vector2f(x, y));
                        text.setRotationZ(angle);
                        renderers.add(text);
                        break;
                    }
                    default: {
                        MeshRenderer quad = new MeshRenderer(EngineResources.TEXTURE_2D, true);
                        Mesh mesh = quad.getMesh();
                        mesh.keepCpuData();
                        mesh.setEbo(new int[]{0, 1, 2, 0, 2, 3});
                        mesh.addVboFloat(new float[]{-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f}, 2,
                                Mesh.STATIC_STORE);
                        mesh.addVboFloat(new float[]{0.1f, 0.2f, 0.1f, 0.9f, 0.8f, 0.9f, 0.8f, 0.2f}, 2,
                                Mesh.STATIC_STORE);
                        quad.setMainTextureChannel(pattern);
                        quad.setScale(new Vector3f(120, 70, 1));
                        quad.setX(x);
                        quad.setY(y);
                        quad.setRotationZ(angle);
                        renderers.add(quad);
                        break;
                    }
                }
            }
        }

        return renderers;
    }

    // Camera views: x, y, zoom, rotation
    private static final float[][] VIEWS = {
            {0, 0, 1, 0},
            {37, -23, 1, 0},
            {200, 150, 2, 0},
            {-180, -90, 0.5f, 0},
            {120, 60, 1.3f, 0.4f},
            {-60, 200, 4, -0.9f},
            {500, 300, 0.25f, 0.2f}
    };

    @Test
    void theCulledPictureIsThePictureWithoutCulling(SceneRunner runner) {
        inScene(runner, scene -> {
            Pattern pattern;
            List<Renderer> renderers;
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                renderers = picture(pattern, scene.context().getWindow().getInfo().getVirtualSizeCopy());
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Engine engine = Views.engine();
                Camera2D camera = scene.camera2D();
                Vector2f position = camera.getCamPosCopy();
                Vector2f zoom = camera.getZoomLevel();
                float rotation = camera.getRotation();
                Runnable drawing = () -> renderers.forEach(Renderer::display);
                DrawBatch2D batch = scene.batch2D();
                try {
                    int culledSomewhere = assertCullingChangesNoPixel(scene, capture, renderers.size(), drawing);
                    assertTrue(culledSomewhere > renderers.size(), "few draws culled: " + culledSomewhere);

                    // A renderer out of the culling is always written
                    camera.setRotation(0);
                    camera.setZoomLevel(1);
                    camera.setPos(new Vector2f());
                    capture.draw(scene, drawing);
                    int culledBefore = batch.lastFrameCulled();
                    assertTrue(culledBefore > 0);
                    renderers.forEach(renderer -> renderer.setViewCulling(false));
                    capture.draw(scene, drawing);
                    assertEquals(0, batch.lastFrameCulled());
                    assertEquals(0, batch.lastFrameCullTested());
                    assertEquals(renderers.size(), batch.lastFrameDraws());
                    renderers.forEach(renderer -> renderer.setViewCulling(true));

                    // A margin of the renderer keeps those near the view
                    // A margin is a number of world units, 0 or more
                    assertThrows(IllegalArgumentException.class, () -> renderers.get(0).setViewCullMargin(-1f));
                    assertThrows(IllegalArgumentException.class,
                            () -> renderers.get(0).setViewCullMargin(Float.NaN));
                    assertEquals(0f, renderers.get(0).getViewCullMargin());
                    renderers.forEach(renderer -> renderer.setViewCullMargin(10000));
                    capture.draw(scene, drawing);
                    assertEquals(0, batch.lastFrameCulled());
                    assertEquals(renderers.size(), batch.lastFrameCullTested());
                } finally {
                    engine.setViewCulling2D(true);
                    camera.setPos(position);
                    camera.setZoomLevel(zoom);
                    camera.setRotation(rotation);
                }
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                renderers.forEach(Renderer::unload);
                pattern.delete();
            }
        });
    }

    /*
     * For each view of VIEWS: the picture with the culling is the picture without it, pixel for pixel, every draw
     * tested (each one with a box), and drawn alone in every mode is the same. Returns the draws culled in all.
     */
    private static int assertCullingChangesNoPixel(Scene scene, Capture capture, int draws, Runnable drawing) {
        Engine engine = Views.engine();
        Camera2D camera = scene.camera2D();
        DrawBatch2D batch = scene.batch2D();
        int culledSomewhere = 0;
        for (float[] view : VIEWS) {
            camera.setRotation(view[3]);
            camera.setZoomLevel(view[2]);
            camera.setPos(new Vector2f(view[0], view[1]));
            String name = "view " + view[0] + ", " + view[1] + " zoom " + view[2] + " rotation " + view[3];

            engine.setViewCulling2D(false);
            ByteBuffer all = capture.draw(scene, drawing);
            assertEquals(draws, batch.lastFrameDraws(), name);
            assertEquals(0, batch.lastFrameCullTested(), name);

            engine.setViewCulling2D(true);
            ByteBuffer culled = capture.draw(scene, drawing);
            assertEquals(0, countDifferent(all, culled), name + ": " + compare(all, culled));
            assertEquals(draws, batch.lastFrameCullTested(), name);
            assertEquals(0, batch.lastFrameCullNoBounds(), name);
            assertEquals(draws, batch.lastFrameDraws() + batch.lastFrameCulled(), name);
            culledSomewhere += batch.lastFrameCulled();
            System.out.println("--" + name + ": " + batch.lastFrameCulled() + " culled of " + draws);

            // Drawn alone (no culling) = batched with the culling, in every mode
            if (drawnPixels(culled) > 200)
                capture.assertEveryModeDrawsTheSame(scene, drawing);
            engine.setBatch2DMode(EBatch2DMode.ORPHAN);
        }

        return culledSomewhere;
    }

    /*
     * Shapes just outside the view of the camera at rest (a world unit is a pixel): some reach into it by their
     * stroke in pixels, their outer stroke, their caps or their anti-aliasing (drawn), the others don't (culled)
     */
    private static List<VectorShape2DRenderer> edgeShapes(Vector2i screen) {
        List<VectorShape2DRenderer> shapes = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            // Outer stroke of 6 pixels and a pixel of fringe: reaches 7 pixels left of the shape
            CircleRenderer circle = new CircleRenderer();
            circle.setReferenceDirection(EDirection.LeftUp);
            circle.setSizePix(40, 40);
            circle.setPosition(new Vector2f(screen.x + (i == 0 ? 3 : 8), screen.y * 0.5f));
            circle.setStrokeWidth(6);
            circle.setStrokeUnit(EStrokeUnit.SCREEN_PIXELS);
            circle.setStrokeAlignment(EStrokeAlignment.OUTER);
            circle.setStrokeColor(new Color4f(1f, 1f, 0.2f, 1f));
            shapes.add(circle);

            // Outer stroke of 5 world units: reaches 6 units right of the rectangle
            RoundedRectangleRenderer rectangle = new RoundedRectangleRenderer().setCornerRadii(2, 2, 2, 2);
            rectangle.setSizePix(40, 30);
            rectangle.setPosition(new Vector2f(i == 0 ? -43 : -47, screen.y * 0.3f));
            rectangle.setStrokeWidth(5);
            rectangle.setStrokeAlignment(EStrokeAlignment.OUTER);
            rectangle.setStrokeColor(new Color4f(0.2f, 1f, 0.4f, 1f));
            shapes.add(rectangle);

            // A line above the view: its half width and its fringe reach down (the box counts its miter: -12 is out)
            List<Vector2f> points = new ArrayList<>();
            points.add(new Vector2f(0, 0));
            points.add(new Vector2f(80, 0));
            points.add(new Vector2f(80, -40));
            PolylineRenderer line = new PolylineRenderer().setPoints(points).setCap(EStrokeCap.SQUARE);
            line.setPosition(new Vector2f(screen.x * 0.4f, i == 0 ? -3 : -12));
            line.setStrokeWidth(8);
            line.setStrokeColor(new Color4f(1f, 0.4f, 1f, 1f));
            shapes.add(line);

            // Points below the view: half their size and their fringe reach up
            PointsRenderer points2 = new PointsRenderer().setPointSize(10);
            points2.addPoint(new Vector2f(0, 0));
            points2.addPoint(new Vector2f(30, 0));
            points2.setPosition(new Vector2f(screen.x * 0.6f, screen.y + (i == 0 ? 4 : 7)));
            points2.setColorMode(new Color4f(0.3f, 0.8f, 1f, 1f));
            shapes.add(points2);
        }
        return shapes;
    }

    // Every vector and geometric shape, and shapes at the edges of the view
    @Test
    void theCulledVectorShapesAreTheShapesWithoutCulling(SceneRunner runner) {
        inScene(runner, scene -> {
            Pattern pattern;
            List<VectorShape2DRenderer> shapes = new ArrayList<>();
            List<VectorShape2DRenderer> edges;
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                shapes.addAll(VectorShapeBatchGLTest.picture(pattern));
                shapes.addAll(VectorShapeBatchGLTest.strokes(pattern));
                edges = edgeShapes(scene.context().getWindow().getInfo().getVirtualSizeCopy());
                shapes.addAll(edges);
            }
            VectorShapeBatchGLTest.requestBatchPrograms(runner, scene, shapes);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Camera2D camera = scene.camera2D();
                Vector2f position = camera.getCamPosCopy();
                Vector2f zoom = camera.getZoomLevel();
                float rotation = camera.getRotation();
                DrawBatch2D batch = scene.batch2D();
                try {
                    int culled = assertCullingChangesNoPixel(scene, capture, shapes.size(),
                            () -> shapes.forEach(VectorShape2DRenderer::display));
                    assertTrue(culled > shapes.size(), "few shapes culled: " + culled);

                    // At rest: those reaching into the view are drawn, the others culled
                    camera.setRotation(0);
                    camera.setZoomLevel(1);
                    camera.setPos(new Vector2f());
                    ByteBuffer reaching = capture.draw(scene, () -> edges.subList(0, 4)
                            .forEach(VectorShape2DRenderer::display));
                    assertEquals(4, batch.lastFrameDraws());
                    assertTrue(drawnPixels(reaching) > 20, "the edges reaching in are drawn");
                    for (VectorShape2DRenderer edge : edges.subList(4, 8)) {
                        ByteBuffer beyond = capture.draw(scene, edge::display);
                        assertEquals(1, batch.lastFrameCulled(), edge.getClass().getSimpleName());
                        assertEquals(0, drawnPixels(beyond));
                    }
                } finally {
                    camera.setPos(position);
                    camera.setZoomLevel(zoom);
                    camera.setRotation(rotation);
                }
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                shapes.forEach(VectorShape2DRenderer::unload);
                pattern.delete();
            }
        });
    }

    // The drawing without renderer culls its quads too
    @Test
    void theQuadsWithoutRendererAreCulled(SceneRunner runner) {
        inScene(runner, scene -> {
            Material material;
            try (Views.Scope ignored = Views.enter(scene)) {
                material = new Material(EngineResources.COLOR_SHAPE_2D);
                material.retain();
            }
            runner.frames(3);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                DrawBatch2D batch = scene.batch2D();
                Vector2i screen = capture.size;
                Runnable drawing = () -> {
                    for (int i = 0; i < 20; ++i) {
                        Vector2f at = new Vector2f(-screen.x + i * screen.x * 3f / 19, screen.y * 0.5f);
                        assertTrue(batch.drawRectangle(material, at, new Vector2f(60, 40),
                                new Color4f(0.9f, 0.3f, 0.2f, 1f)));
                        assertTrue(batch.drawQuad(material, new float[]{at.x, at.y + 100, at.x, at.y + 60,
                                at.x + 50, at.y + 60, at.x + 50, at.y + 100}, new float[8], null, 0, 0f));
                    }
                };
                Engine engine = Views.engine();
                try {
                    engine.setViewCulling2D(false);
                    ByteBuffer all = capture.draw(scene, drawing);
                    assertEquals(40, batch.lastFrameDraws());
                    engine.setViewCulling2D(true);
                    ByteBuffer culled = capture.draw(scene, drawing);
                    assertEquals(0, countDifferent(all, culled), compare(all, culled));
                    assertEquals(40, batch.lastFrameCullTested());
                    assertTrue(batch.lastFrameCulled() > 10, "culled: " + batch.lastFrameCulled());
                    assertEquals(40, batch.lastFrameDraws() + batch.lastFrameCulled());
                } finally {
                    engine.setViewCulling2D(true);
                    material.release();
                }
            }
        });
    }
}
