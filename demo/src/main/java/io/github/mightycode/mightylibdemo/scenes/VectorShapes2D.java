package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArcRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArrowRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeCap;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeJoin;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EPointShape;
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
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.geometry.Polygon;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The vector shapes. Top: each shape filled with a stroke, stroke only, textured. Below: the three alignments of a
 * half transparent stroke, corners of different radii, a turning shape, a stroke of one pixel, sharp edges, a tinted
 * texture. Then points (a wave moved at each frame); polylines with each join (miter, round, bevel: one row each)
 * and cap (butt, round, square), a closed half transparent line, a line of one screen pixel at every zoom, arrows,
 * a concave polygon with a stroke and the polygon of Scene2D; over a background grid of lines one pixel wide.
 * Controls (letters of a French AZERTY keyboard, read by their position like the other bindings of the demo): the wheel
 * zooms, Ctrl held zooms in, Shift held zooms out, Alt + wheel turns the camera, Z Q S D move it, R resets it,
 * X switches the unit of the strokes (world / screen pixels), W switches the anti-aliasing.
 */
public class VectorShapes2D extends Scene {
    private static final float CELL_WIDTH = 150f;
    private static final float CELL_HEIGHT = 110f;
    private static final float TOP = -400f;
    private static final float START_ZOOM = 0.65f;

    private static final Color4f FILL = new Color4f(0.25f, 0.55f, 0.9f, 1f);
    private static final Color4f STROKE = new Color4f(0.95f, 0.85f, 0.3f, 1f);
    private static final Color4f HALF_STROKE = new Color4f(0.95f, 0.3f, 0.3f, 0.6f);
    private static final Color4f NONE = new Color4f(0f, 0f, 0f, 0f);

    private final List<VectorShape2DRenderer> shapes = new ArrayList<>();
    // Width of the stroke of each shape: in world units, in pixels of the screen
    private final List<float[]> strokeWidths = new ArrayList<>();
    private VectorShape2DRenderer turning;
    private GridRenderer grid;
    private PointsRenderer wave;
    private PolylineRenderer hairline;
    private float time;
    private Text help;

    // Physical keys (GLFW names the positions of a QWERTY keyboard): the letters W and X of an AZERTY keyboard
    private static final int ANTIALIASING_KEY = GLFW_KEY_Z;
    private static final int STROKE_UNIT_KEY = GLFW_KEY_X;
    private static final int RESET_KEY = GLFW_KEY_R;
    // World units per second at zoom 1, zoom factor per second (keys) and per notch of the wheel, radians per notch
    private static final float MOVE_SPEED = 600f;
    private static final float KEY_ZOOM_SPEED = 1.5f;
    private static final float WHEEL_ZOOM_STEP = 1.15f;
    private static final float WHEEL_ROTATION_STEP = (float) Math.toRadians(5);

    private boolean screenUnits;
    private boolean antialiasing = true;

    @Override
    public void launch(String[] args) {
        setClearColor(30, 30, 36, 1f);

        Camera2D hudCamera = camera2D().copy();
        resetCamera();

        grid = new GridRenderer();
        grid.setSizePix(2400, 1600);
        grid.setStrokeColor(new Color4f(1f, 1f, 1f, 0.12f));
        grid.setPosition(new Vector2f(0, 0));

        addShapeGrid();
        addStrokeExamples();
        addPoints();
        addLines();

        help = new Text();
        help.setReferenceCamera(hudCamera);
        help.setFontSize(18)
                .setReference(EDirection.LeftUp)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(10, 10));
        updateHelp();
    }

    // One column per shape: filled with a stroke, stroke only, textured with a stroke
    private void addShapeGrid() {
        List<Supplier<VectorShape2DRenderer>> factories = List.of(
                () -> new RoundedRectangleRenderer().setCornerRadius(20f),
                CircleRenderer::new,
                EllipseRenderer::new,
                TriangleRenderer::new,
                () -> new ArcRenderer().setInnerRatio(0.5f).setAngles((float) Math.PI, (float) (Math.PI * 1.5)),
                () -> new ArcRenderer().setAngles((float) (-Math.PI * 0.25), (float) (Math.PI * 1.5)),
                () -> new RegularPolygonRenderer(5),
                HexagonRenderer::new);

        for (int column = 0; column < factories.size(); ++column) {
            VectorShape2DRenderer filled = add(factories.get(column).get(), column, 0);
            filled.setColorMode(FILL);
            stroke(filled, 5f, 2f, STROKE);

            VectorShape2DRenderer strokeOnly = add(factories.get(column).get(), column, 1);
            strokeOnly.setColorMode(NONE);
            stroke(strokeOnly, 5f, 2f, STROKE);

            VectorShape2DRenderer textured = add(factories.get(column).get(), column, 2);
            textured.setMainTextureChannel("painting");
            stroke(textured, 3f, 1f, STROKE);
        }
    }

    private void addStrokeExamples() {
        EStrokeAlignment[] alignments = EStrokeAlignment.values();
        for (int i = 0; i < alignments.length; ++i) {
            VectorShape2DRenderer aligned = add(new RoundedRectangleRenderer().setCornerRadius(14f), i, 3);
            aligned.setColorMode(FILL);
            stroke(aligned, 16f, 4f, HALF_STROKE);
            aligned.setStrokeAlignment(alignments[i]);
        }

        VectorShape2DRenderer corners = add(new RoundedRectangleRenderer().setCornerRadii(0f, 40f, 10f, 30f), 3, 3);
        corners.setColorMode(FILL);
        stroke(corners, 3f, 1f, STROKE);

        turning = add(new RoundedRectangleRenderer().setCornerRadius(10f), 4, 3);
        turning.setColorMode(FILL);
        stroke(turning, 5f, 2f, STROKE);

        VectorShape2DRenderer thin = add(new CircleRenderer(), 5, 3);
        thin.setColorMode(NONE);
        stroke(thin, 1f, 1f, STROKE);

        VectorShape2DRenderer sharpEdges = add(new HexagonRenderer(), 6, 3);
        sharpEdges.setColorMode(FILL);
        sharpEdges.setAntialiasing(false);
        stroke(sharpEdges, 4f, 2f, STROKE);

        VectorShape2DRenderer tinted = add(new EllipseRenderer(), 7, 3);
        tinted.setMainTextureChannel("painting");
        tinted.setColorMode(new Color4f(1f, 0.5f, 0.5f, 0.8f), false);
    }

    // Row 4: round and square points of growing size, a wave of points moved at each frame
    private void addPoints() {
        PointsRenderer round = new PointsRenderer();
        PointsRenderer square = new PointsRenderer();
        square.setPointShape(EPointShape.SQUARE);
        for (int i = 0; i < 8; ++i) {
            round.addPoint(new Vector2f(i * 30f, 0f));
            square.addPoint(new Vector2f(i * 30f, 40f));
        }
        round.setPointSize(14f);
        round.setColorMode(FILL);
        square.setPointSize(14f);
        square.setColorMode(NONE);

        for (PointsRenderer points : List.of(round, square)) {
            points.setPosition(new Vector2f(-3.5f * CELL_WIDTH, TOP + 4 * CELL_HEIGHT - 20f));
            shapes.add(points);
            strokeWidths.add(new float[]{0f, 0f});
        }
        stroke(square, 2f, 1f, STROKE);

        wave = new PointsRenderer();
        for (int i = 0; i < 60; ++i)
            wave.addPoint(new Vector2f(i * 12f, 0f));
        wave.setPointSize(7f);
        wave.setColorMode(STROKE);
        wave.setPosition(new Vector2f(-0.5f * CELL_WIDTH, TOP + 4 * CELL_HEIGHT));
        shapes.add(wave);
        strokeWidths.add(new float[]{0f, 0f});
    }

    // Rows 5 to 7: a zigzag per join (row) and cap (column), then closed and thin lines, arrows, polygons
    private void addLines() {
        EStrokeJoin[] joins = {EStrokeJoin.MITER, EStrokeJoin.ROUND, EStrokeJoin.BEVEL};
        EStrokeCap[] caps = EStrokeCap.values();
        for (int row = 0; row < joins.length; ++row) {
            for (int column = 0; column < caps.length; ++column) {
                PolylineRenderer zigzag = new PolylineRenderer();
                zigzag.setPoints(List.of(new Vector2f(-45, 25), new Vector2f(-15, -25), new Vector2f(10, 25),
                        new Vector2f(45, -10)));
                zigzag.setJoin(joins[row]);
                zigzag.setCap(caps[column]);
                add(zigzag, column, 5 + row);
                stroke(zigzag, 12f, 5f, STROKE);
            }
        }

        // A closed star, half transparent: no overlap at its joins
        PolylineRenderer star = new PolylineRenderer();
        List<Vector2f> starPoints = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            float radius = i % 2 == 0 ? 45f : 20f;
            starPoints.add(new Vector2f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius));
        }
        star.setPoints(starPoints).setClosed(true);
        star.setJoin(EStrokeJoin.ROUND);
        add(star, 3, 5);
        stroke(star, 8f, 3f, HALF_STROKE);

        // One pixel of the screen at every zoom
        hairline = new PolylineRenderer();
        hairline.setPoints(List.of(new Vector2f(-50, 30), new Vector2f(0, -30), new Vector2f(50, 30)));
        hairline.setStrokeUnit(EStrokeUnit.SCREEN_PIXELS);
        hairline.setStrokeWidth(1f);
        hairline.setStrokeColor(STROKE);
        hairline.setPosition(new Vector2f(0.5f * CELL_WIDTH, TOP + 5 * CELL_HEIGHT));

        ArrowRenderer filled = new ArrowRenderer();
        filled.setLine(new Vector2f(-50, 20), new Vector2f(50, -20));
        add(filled, 5, 5);
        stroke(filled, 5f, 2f, STROKE);

        ArrowRenderer open = new ArrowRenderer();
        open.setPoints(List.of(new Vector2f(-50, 25), new Vector2f(-10, -25), new Vector2f(20, 20),
                new Vector2f(50, -20)));
        open.setHead(18f, 20f, false);
        open.setJoin(EStrokeJoin.ROUND);
        open.setCap(EStrokeCap.ROUND);
        add(open, 6, 5);
        stroke(open, 5f, 2f, STROKE);

        Polygon concave = Polygon.CreateBuilder()
                .addVertex(new Vector2f(0, 0)).addVertex(new Vector2f(1, 0.4f)).addVertex(new Vector2f(2, 0))
                .addVertex(new Vector2f(1.6f, 1)).addVertex(new Vector2f(2, 2)).addVertex(new Vector2f(0.2f, 1.6f))
                .build();
        VectorShape2DRenderer concaveRenderer = add(new PolygonRenderer(concave), 7, 5);
        concaveRenderer.setColorMode(FILL);
        stroke(concaveRenderer, 5f, 2f, STROKE);

        // The polygon of Scene2D, textured
        Polygon fromScene2D = Polygon.CreateBuilder()
                .addVertex(new Vector2f(0, 0)).addVertex(new Vector2f(0.5f, -0.5f))
                .addVertex(new Vector2f(0.5f, 0.5f)).addVertex(new Vector2f(1.5f, 0.5f))
                .addVertex(new Vector2f(1f, 1f)).addVertex(new Vector2f(0f, 1f))
                .build();
        VectorShape2DRenderer textured = add(new PolygonRenderer(fromScene2D), 7, 6);
        textured.setMainTextureChannel("painting");
        stroke(textured, 3f, 1f, STROKE);
    }

    // The shape centered in the cell (column, row) of the grid
    private VectorShape2DRenderer add(VectorShape2DRenderer shape, int column, int row) {
        shape.setReferenceDirection(EDirection.None);
        if (shape instanceof PolylineRenderer || shape instanceof ArrowRenderer)
            shape.setSizePix(1, 1);
        else if (shape instanceof HexagonRenderer)
            shape.setSizePix(100, 100 * HexagonRenderer.STRETCH_RATIO);
        else if (shape instanceof CircleRenderer || shape instanceof ArcRenderer
                || shape instanceof RegularPolygonRenderer)
            shape.setSizePix(85, 85);
        else
            shape.setSizePix(120, 85);
        shape.setPosition(new Vector2f((column - 3.5f) * CELL_WIDTH, TOP + row * CELL_HEIGHT));

        shapes.add(shape);
        strokeWidths.add(new float[]{0f, 0f});
        return shape;
    }

    private void stroke(VectorShape2DRenderer shape, float worldWidth, float pixelWidth, Color4f color) {
        int index = shapes.indexOf(shape);
        strokeWidths.set(index, new float[]{worldWidth, pixelWidth});
        shape.setStrokeColor(color);
        applyStroke(index);
    }

    private void applyStroke(int index) {
        float[] widths = strokeWidths.get(index);
        VectorShape2DRenderer shape = shapes.get(index);
        shape.setStrokeUnit(screenUnits ? EStrokeUnit.SCREEN_PIXELS : EStrokeUnit.WORLD);
        shape.setStrokeWidth(screenUnits ? widths[1] : widths[0]);
    }

    // The origin of the world at the center of the view
    private void resetCamera() {
        Vector2i size = context().getWindow().getInfo().getVirtualSizeRef();
        camera2D().setZoomReference(EDirection.None);
        camera2D().setRotation(0f);
        camera2D().setZoomLevel(1f);
        camera2D().setPos(new Vector2f(-size.x / 2f, -size.y / 2f));
        camera2D().setZoomLevel(START_ZOOM);
    }

    private void updateHelp() {
        help.setText("Wheel / Ctrl / Shift zoom, Alt + wheel turn, Z Q S D move, R reset\n"
                + "X stroke unit: " + (screenUnits ? "screen pixels" : "world") + "\n"
                + "W anti-aliasing: " + (antialiasing ? "on" : "off"));
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});

        KeyboardManager keyboard = context().getKeyboardManager();
        float delta = GameTime.DeltaTime();

        moveCamera(delta);
        zoomAndTurnCamera(keyboard, delta);
        if (keyboard.keyPressed(RESET_KEY))
            resetCamera();

        if (keyboard.keyPressed(STROKE_UNIT_KEY)) {
            screenUnits = !screenUnits;
            for (int i = 0; i < shapes.size(); ++i)
                applyStroke(i);
            updateHelp();
        }

        if (keyboard.keyPressed(ANTIALIASING_KEY)) {
            antialiasing = !antialiasing;
            for (VectorShape2DRenderer shape : shapes)
                shape.setAntialiasing(antialiasing);
            updateHelp();
        }

        turning.setRotationZ(turning.getRotationAngle() + delta * 0.6f);

        time += delta;
        for (int i = 0; i < wave.pointCount(); ++i)
            wave.setPoint(i, new Vector2f(i * 12f, (float) Math.sin(time * 2 + i * 0.25) * 25f));
    }

    // Z Q S D (the move actions of the demo, arrows too): along the axes of the screen, at the same speed on the screen
    // whatever the zoom (the position of the camera is in the turned world, scaled by the zoom)
    private void moveCamera(float delta) {
        IInputManager inputs = context().getInputManager();
        Vector2f direction = new Vector2f(
                inputs.getValue(ActionId.MOVE_RIGHT_2D) - inputs.getValue(ActionId.MOVE_LEFT_2D),
                inputs.getValue(ActionId.MOVE_DOWN_2D) - inputs.getValue(ActionId.MOVE_UP_2D));
        if (direction.lengthSquared() == 0f)
            return;

        if (direction.lengthSquared() > 1f)
            direction.normalize();
        Vector2f zoom = camera2D().getZoomLevel();
        direction.mul(MOVE_SPEED * delta).div(zoom);
        camera2D().setPos(camera2D().getCamPosCopy().add(direction));
    }

    // The wheel zooms (turns with Alt), Ctrl held zooms in, Shift held zooms out, around the center of the view
    private void zoomAndTurnCamera(KeyboardManager keyboard, float delta) {
        float wheel = context().getMouseManager().getMouseScroll().y;
        boolean alt = keyboard.getKeyState(GLFW_KEY_LEFT_ALT) || keyboard.getKeyState(GLFW_KEY_RIGHT_ALT);

        float factor = 1f;
        if (wheel != 0f) {
            if (alt)
                camera2D().setRotation(camera2D().getRotation() + wheel * WHEEL_ROTATION_STEP);
            else
                factor *= (float) Math.pow(WHEEL_ZOOM_STEP, wheel);
        }
        if (keyboard.getKeyState(GLFW_KEY_LEFT_CONTROL) || keyboard.getKeyState(GLFW_KEY_RIGHT_CONTROL))
            factor *= (float) Math.pow(KEY_ZOOM_SPEED, delta);
        if (keyboard.getKeyState(GLFW_KEY_LEFT_SHIFT) || keyboard.getKeyState(GLFW_KEY_RIGHT_SHIFT))
            factor /= (float) Math.pow(KEY_ZOOM_SPEED, delta);

        if (factor != 1f)
            camera2D().setZoomLevel(camera2D().getZoomLevel().x * factor);
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        grid.display();
        for (VectorShape2DRenderer shape : shapes)
            shape.display();
        hairline.display();

        help.display();

        super.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        grid.unload();
        hairline.unload();
        for (VectorShape2DRenderer shape : shapes)
            shape.unload();
        help.unload();
    }
}
