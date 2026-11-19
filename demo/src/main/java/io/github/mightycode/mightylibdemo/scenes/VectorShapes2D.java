package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArcRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EllipseRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.HexagonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RegularPolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.TriangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
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
 * texture. Z / X zoom, Q / E turn the camera, R resets it, U switches the unit of the strokes (world / screen
 * pixels), A switches the anti-aliasing.
 */
public class VectorShapes2D extends Scene {
    private static final float CELL_WIDTH = 150f;
    private static final float CELL_HEIGHT = 110f;
    private static final float TOP = -480f;
    private static final float START_ZOOM = 0.65f;

    private static final Color4f FILL = new Color4f(0.25f, 0.55f, 0.9f, 1f);
    private static final Color4f STROKE = new Color4f(0.95f, 0.85f, 0.3f, 1f);
    private static final Color4f HALF_STROKE = new Color4f(0.95f, 0.3f, 0.3f, 0.6f);
    private static final Color4f NONE = new Color4f(0f, 0f, 0f, 0f);

    private final List<VectorShape2DRenderer> shapes = new ArrayList<>();
    // Width of the stroke of each shape: in world units, in pixels of the screen
    private final List<float[]> strokeWidths = new ArrayList<>();
    private VectorShape2DRenderer turning;
    private Text help;

    private boolean screenUnits;
    private boolean antialiasing = true;

    @Override
    public void launch(String[] args) {
        setClearColor(30, 30, 36, 1f);

        Camera2D hudCamera = camera2D().copy();
        resetCamera();

        addShapeGrid();
        addStrokeExamples();

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

    // The shape centered in the cell (column, row) of the grid
    private VectorShape2DRenderer add(VectorShape2DRenderer shape, int column, int row) {
        shape.setReferenceDirection(EDirection.None);
        if (shape instanceof HexagonRenderer)
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
        help.setText("Z / X zoom, Q / E turn the camera, R reset\n"
                + "U stroke unit: " + (screenUnits ? "screen pixels" : "world") + "\n"
                + "A anti-aliasing: " + (antialiasing ? "on" : "off"));
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});

        KeyboardManager keyboard = context().getKeyboardManager();
        float delta = GameTime.DeltaTime();

        if (keyboard.getKeyState(GLFW_KEY_Z))
            camera2D().setZoomLevel(camera2D().getZoomLevel().x * (1f + delta));
        if (keyboard.getKeyState(GLFW_KEY_X))
            camera2D().setZoomLevel(camera2D().getZoomLevel().x / (1f + delta));
        if (keyboard.getKeyState(GLFW_KEY_Q))
            camera2D().setRotation(camera2D().getRotation() - delta * 0.5f);
        if (keyboard.getKeyState(GLFW_KEY_E))
            camera2D().setRotation(camera2D().getRotation() + delta * 0.5f);
        if (keyboard.keyPressed(GLFW_KEY_R))
            resetCamera();

        if (keyboard.keyPressed(GLFW_KEY_U)) {
            screenUnits = !screenUnits;
            for (int i = 0; i < shapes.size(); ++i)
                applyStroke(i);
            updateHelp();
        }

        if (keyboard.keyPressed(GLFW_KEY_A)) {
            antialiasing = !antialiasing;
            for (VectorShape2DRenderer shape : shapes)
                shape.setAntialiasing(antialiasing);
            updateHelp();
        }

        turning.setRotationZ(turning.getRotationAngle() + delta * 0.6f);
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        for (VectorShape2DRenderer shape : shapes)
            shape.display();

        help.display();

        super.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        for (VectorShape2DRenderer shape : shapes)
            shape.unload();
        help.unload();
    }
}
