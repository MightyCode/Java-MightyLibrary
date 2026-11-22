package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArcRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EPointShape;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EllipseRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.GridRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.HexagonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PointsRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RegularPolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.TriangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.test.BatchPictures.Capture;
import io.github.mightycode.mightylib.test.BatchPictures.Pattern;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.*;

// The vector shapes in the 2D batch (one instance per shape, per point): the picture drawn alone in every mode
@MightySceneTest(config = SceneRunnerTest.Config.class)
class VectorShapeBatchGLTest {
    private static final EStrokeAlignment[] ALIGNMENTS = EStrokeAlignment.values();

    // Every shape of the engine: strokes of each alignment and unit, turned, mirrored, textured, sharp
    private static List<VectorShape2DRenderer> picture(Pattern pattern) {
        List<VectorShape2DRenderer> shapes = new ArrayList<>();
        shapes.add(new CircleRenderer());
        shapes.add(new EllipseRenderer());
        shapes.add(new RoundedRectangleRenderer().setCornerRadii(4, 12, 20, 0));
        shapes.add(new TriangleRenderer().setVertices(new Vector2f(0, 0), new Vector2f(1, 0.3f),
                new Vector2f(0.4f, 1)));
        shapes.add(new ArcRenderer().setInnerRatio(0.5f).setAngles(0.3f, 4f));
        shapes.add(new RegularPolygonRenderer(5).setStartAngle(0.2f));
        shapes.add(new HexagonRenderer());
        shapes.add(new CircleRenderer());
        shapes.add(new EllipseRenderer());

        for (int i = 0; i < shapes.size(); ++i) {
            VectorShape2DRenderer shape = shapes.get(i);
            // A negative size mirrors the shape
            shape.setSizePix(i == 3 ? -110 : 110 + i * 6, i == 5 ? -80 : 80 + i * 4);
            shape.setPosition(new Vector2f(90 + (i % 5) * 170, 110 + (i / 5) * 200));
            shape.setRotationZ(i * 0.35f - 0.7f);
            shape.setColorMode(new Color4f(0.1f * i, 0.9f - 0.08f * i, 0.4f, i % 2 == 0 ? 1f : 0.6f));
            shape.setStrokeWidth(i % 3 == 0 ? 0 : 3 + i);
            shape.setStrokeColor(new Color4f(1f, 0.3f, 0.1f * i, 1f));
            shape.setStrokeAlignment(ALIGNMENTS[i % ALIGNMENTS.length]);
            shape.setStrokeUnit(i % 2 == 0 ? EStrokeUnit.WORLD : EStrokeUnit.SCREEN_PIXELS);
        }

        // Textured (tinted), a part of the texture; sharp edges
        shapes.get(7).setMainTextureChannel(pattern);
        shapes.get(7).setTexturePosition(new Vector4f(0.1f, 0.8f, 0.2f, 0.9f));
        shapes.get(8).setAntialiasing(false);

        GridRenderer grid = new GridRenderer().setSpacing(17, 23).setGridOrigin(new Vector2f(3, 5));
        grid.setSizePix(300, 160);
        grid.setPosition(new Vector2f(700, 480));
        grid.setRotationZ(0.2f);
        shapes.add(grid);

        PointsRenderer points = new PointsRenderer().setPointSize(14).setPointShape(EPointShape.SQUARE);
        for (int i = 0; i < 12; ++i)
            points.addPoint(new Vector2f(i * 22, (i % 4) * 15));
        points.setPosition(new Vector2f(150, 480));
        points.setRotationZ(-0.3f);
        points.setStrokeWidth(2);
        points.setColorMode(new Color4f(0.2f, 0.5f, 1f, 1f));
        shapes.add(points);

        PointsRenderer round = new PointsRenderer().setPointSize(10);
        for (int i = 0; i < 8; ++i)
            round.addPoint(new Vector2f(i * 18, i * 6));
        round.setPosition(new Vector2f(420, 560));
        round.setStrokeWidth(1.5f);
        round.setStrokeUnit(EStrokeUnit.SCREEN_PIXELS);
        round.setStrokeAlignment(EStrokeAlignment.OUTER);
        shapes.add(round);
        return shapes;
    }

    /*
     * The batch program of a material is asked at its first batched draw, then loaded with the time of the frames
     * (drawn alone meanwhile): one batched display, then frames
     */
    private static void requestBatchPrograms(SceneRunner runner, io.github.mightycode.mightylib.scenes.Scene scene,
                                             List<? extends VectorShape2DRenderer> shapes) {
        runner.frames(8);
        try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
            Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
            capture.draw(scene, () -> shapes.forEach(VectorShape2DRenderer::display));
        }
        runner.frames(8);
    }

    @Test
    void eachShapeIsDrawnAsAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            List<VectorShape2DRenderer> shapes;
            Pattern pattern;
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                shapes = picture(pattern);
            }
            requestBatchPrograms(runner, scene, shapes);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Runnable drawing = () -> shapes.forEach(VectorShape2DRenderer::display);
                capture.assertEveryModeDrawsTheSame(scene, drawing);

                // Every shape batched: each a draw of instances (the shapes of one state together)
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                capture.draw(scene, drawing);
                assertEquals(shapes.size(), scene.batch2D().lastFrameDraws());
                assertEquals(0, scene.batch2D().lastFrameBarriers());
                assertEquals(scene.batch2D().lastFrameDrawCalls(), scene.batch2D().lastFrameInstancedDrawCalls());
                assertTrue(scene.batch2D().lastFrameFormatCalls().containsKey("shape"));
                assertTrue(scene.batch2D().lastFrameFormatCalls().containsKey("point"));
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                shapes.forEach(VectorShape2DRenderer::unload);
                pattern.delete();
            }
        });
    }

    // Many circles of one look: one draw call
    @Test
    void equalShapesAreOneDrawCall(SceneRunner runner) {
        inScene(runner, scene -> {
            List<CircleRenderer> circles = new ArrayList<>();
            try (Views.Scope ignored = Views.enter(scene)) {
                for (int i = 0; i < 40; ++i) {
                    CircleRenderer circle = new CircleRenderer();
                    circle.setSizePix(12, 12);
                    circle.setPosition(new Vector2f(10 + i * 15, 40));
                    circle.setColorMode(new Color4f(i / 40f, 0.5f, 0.5f, 1f));
                    circle.setStrokeWidth(i % 4);
                    circles.add(circle);
                }
            }
            requestBatchPrograms(runner, scene, circles);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                capture.draw(scene, () -> circles.forEach(CircleRenderer::display));
                assertEquals(40, scene.batch2D().lastFrameDraws());
                assertEquals(1, scene.batch2D().lastFrameDrawCalls());
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                circles.forEach(CircleRenderer::unload);
            }
        });
    }
}
