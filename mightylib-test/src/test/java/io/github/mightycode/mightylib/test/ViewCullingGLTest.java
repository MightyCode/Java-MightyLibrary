package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
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
                    int culledSomewhere = 0;
                    for (float[] view : VIEWS) {
                        camera.setRotation(view[3]);
                        camera.setZoomLevel(view[2]);
                        camera.setPos(new Vector2f(view[0], view[1]));
                        String name = "view " + view[0] + ", " + view[1] + " zoom " + view[2] + " rotation " + view[3];

                        engine.setViewCulling2D(false);
                        ByteBuffer all = capture.draw(scene, drawing);
                        assertEquals(renderers.size(), batch.lastFrameDraws(), name);
                        assertEquals(0, batch.lastFrameCullTested(), name);

                        engine.setViewCulling2D(true);
                        ByteBuffer culled = capture.draw(scene, drawing);
                        assertEquals(0, countDifferent(all, culled), name + ": " + compare(all, culled));
                        assertEquals(renderers.size(), batch.lastFrameCullTested(), name);
                        assertEquals(0, batch.lastFrameCullNoBounds(), name);
                        assertEquals(renderers.size(), batch.lastFrameDraws() + batch.lastFrameCulled(), name);
                        assertTrue(drawnPixels(culled) > 200, name + ": almost nothing drawn");
                        culledSomewhere += batch.lastFrameCulled();
                        System.out.println("--" + name + ": " + batch.lastFrameCulled() + " culled of "
                                + renderers.size());

                        // Drawn alone (no culling) = batched with the culling, in every mode
                        capture.assertEveryModeDrawsTheSame(scene, drawing);
                        engine.setBatch2DMode(EBatch2DMode.ORPHAN);
                    }
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
