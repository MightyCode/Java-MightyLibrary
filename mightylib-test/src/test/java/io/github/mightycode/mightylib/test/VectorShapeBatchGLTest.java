package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArcRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArrowRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EPointShape;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeCap;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeJoin;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EllipseRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.GridRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.HexagonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PointsRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PolylineRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RegularPolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.TriangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.test.BatchPictures.Capture;
import io.github.mightycode.mightylib.test.BatchPictures.Pattern;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.Polygon;
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
    static List<VectorShape2DRenderer> picture(Pattern pattern) {
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
    static void requestBatchPrograms(SceneRunner runner, io.github.mightycode.mightylib.scenes.Scene scene,
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

    // Polylines of each join and cap, closed, an arrow, polygons filled and stroked (one textured): per vertex values
    static List<VectorShape2DRenderer> strokes(Pattern pattern) {
        List<VectorShape2DRenderer> shapes = new ArrayList<>();
        EStrokeJoin[] joins = EStrokeJoin.values();
        EStrokeCap[] caps = EStrokeCap.values();
        for (int i = 0; i < 6; ++i) {
            List<Vector2f> points = new ArrayList<>();
            for (int p = 0; p < 5; ++p)
                points.add(new Vector2f(p * 30, (p % 2) * (25 + i * 6)));
            PolylineRenderer line = new PolylineRenderer().setPoints(points).setCap(caps[i % caps.length])
                    .setClosed(i == 5);
            line.setJoin(joins[i % joins.length]);
            line.setPosition(new Vector2f(60 + (i % 3) * 230, 60 + (i / 3) * 140));
            line.setRotationZ(0.2f * i);
            line.setStrokeWidth(4 + i * 2);
            line.setStrokeUnit(i % 2 == 0 ? EStrokeUnit.WORLD : EStrokeUnit.SCREEN_PIXELS);
            line.setStrokeColor(new Color4f(0.15f * i, 0.8f, 1f - 0.1f * i, i == 2 ? 0.5f : 1f));
            line.setAntialiasing(i != 4);
            shapes.add(line);
        }

        ArrowRenderer arrow = new ArrowRenderer().setLine(new Vector2f(0, 0), new Vector2f(160, 40))
                .setHead(30, 24, true);
        arrow.setPosition(new Vector2f(700, 100));
        arrow.setStrokeWidth(5);
        arrow.setStrokeColor(new Color4f(1f, 0.9f, 0.2f, 1f));
        shapes.add(arrow);

        Polygon concave = Polygon.CreateBuilder()
                .addVertex(new Vector2f(0, 0)).addVertex(new Vector2f(1, 0.4f)).addVertex(new Vector2f(2, 0))
                .addVertex(new Vector2f(1.6f, 1)).addVertex(new Vector2f(2, 2)).addVertex(new Vector2f(0.2f, 1.6f))
                .build();
        for (int i = 0; i < 2; ++i) {
            PolygonRenderer polygon = new PolygonRenderer(concave);
            polygon.setSizePix(i == 0 ? 120 : -90, 100);
            polygon.setPosition(new Vector2f(300 + i * 260, 380));
            polygon.setRotationZ(0.3f - 0.5f * i);
            polygon.setColorMode(new Color4f(0.3f, 0.6f * i, 0.9f, 0.8f));
            polygon.setStrokeWidth(3 + 3 * i);
            polygon.setStrokeColor(new Color4f(0.9f, 0.2f, 0.4f, 1f));
            if (i == 1)
                polygon.setMainTextureChannel(pattern);
            shapes.add(polygon);
        }
        return shapes;
    }

    @Test
    void geometricShapesAreDrawnAsAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            List<VectorShape2DRenderer> shapes;
            Pattern pattern;
            try (Views.Scope ignored = Views.enter(scene)) {
                pattern = new Pattern();
                shapes = strokes(pattern);
            }
            requestBatchPrograms(runner, scene, shapes);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Runnable drawing = () -> shapes.forEach(VectorShape2DRenderer::display);
                capture.assertEveryModeDrawsTheSame(scene, drawing);

                // Zoomed in: the widths in screen pixels stay, the others grow
                Camera2D camera = scene.camera2D();
                camera.setZoomLevel(1.7f);
                capture.assertEveryModeDrawsTheSame(scene, drawing);
                camera.setZoomLevel(1f);

                // Every shape in one draw call: the widths and colors are values of the vertices
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                capture.draw(scene, drawing);
                assertEquals(shapes.size(), scene.batch2D().lastFrameDraws());
                assertEquals(0, scene.batch2D().lastFrameBarriers());
                assertTrue(scene.batch2D().lastFrameFormatCalls().get("stroke") <= 2,
                        scene.batch2D().lastFrameFormatCalls().toString());
            }

            try (Views.Scope ignored = Views.enter(scene)) {
                shapes.forEach(VectorShape2DRenderer::unload);
                pattern.delete();
            }
        });
    }
}
