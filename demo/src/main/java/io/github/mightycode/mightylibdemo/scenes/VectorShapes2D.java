package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
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

import static org.lwjgl.glfw.GLFW.*;

/**
 * The vector shapes: each one filled, stroke only and textured, the three alignments of the stroke, strokes in
 * world units or in pixels of the screen. Z / X zoom, Q / E turn the camera, R resets it, U switches the unit of the
 * strokes (world / screen pixels), A switches the anti-aliasing.
 */
public class VectorShapes2D extends Scene {
    private static final float CELL = 170f;
    private static final Color4f FILL = new Color4f(0.25f, 0.55f, 0.9f, 1f);
    private static final Color4f STROKE = new Color4f(0.95f, 0.85f, 0.3f, 1f);
    private static final Color4f HALF_STROKE = new Color4f(0.95f, 0.3f, 0.3f, 0.6f);
    private static final Color4f NONE = new Color4f(0f, 0f, 0f, 0f);

    private final List<VectorShape2DRenderer> shapes = new ArrayList<>();
    // Width of the stroke of each shape in world units, and in pixels when switched
    private final List<float[]> strokeWidths = new ArrayList<>();
    private VectorShape2DRenderer turning;
    private Text help;
    private Camera2D hudCamera;

    private boolean screenUnits;
    private boolean antialiasing = true;

    @Override
    public void launch(String[] args) {
        setClearColor(30, 30, 36, 1f);

        hudCamera = camera2D().copy();
        resetCamera();

        // Row 0: rounded rectangles
        RoundedRectangleRenderer filled = rounded(0, 0, 24f);
        filled.setColorMode(FILL);

        RoundedRectangleRenderer strokeOnly = rounded(1, 0, 24f);
        strokeOnly.setColorMode(NONE);
        stroke(strokeOnly, 6f, 2f, STROKE);

        RoundedRectangleRenderer both = rounded(2, 0, 24f);
        both.setColorMode(FILL);
        stroke(both, 6f, 2f, STROKE);

        RoundedRectangleRenderer sharp = rounded(3, 0, 0f);
        sharp.setColorMode(FILL);
        stroke(sharp, 4f, 1f, STROKE);

        RoundedRectangleRenderer corners = rounded(4, 0, 0f);
        corners.setCornerRadii(0f, 40f, 10f, 30f);
        corners.setColorMode(FILL);
        stroke(corners, 3f, 1f, STROKE);

        // Row 1: the three alignments, a half transparent stroke over the fill
        EStrokeAlignment[] alignments = EStrokeAlignment.values();
        for (int i = 0; i < alignments.length; ++i) {
            RoundedRectangleRenderer aligned = rounded(i, 1, 16f);
            aligned.setColorMode(FILL);
            stroke(aligned, 16f, 4f, HALF_STROKE);
            aligned.setStrokeAlignment(alignments[i]);
        }

        // Row 2: textured, tinted, turning, thin lines
        RoundedRectangleRenderer textured = rounded(0, 2, 30f);
        textured.setMainTextureChannel("painting");
        stroke(textured, 4f, 2f, STROKE);

        RoundedRectangleRenderer tinted = rounded(1, 2, 30f);
        tinted.setMainTextureChannel("painting");
        tinted.setColorMode(new Color4f(1f, 0.5f, 0.5f, 0.8f), false);

        turning = rounded(2, 2, 12f);
        turning.setColorMode(FILL);
        stroke(turning, 5f, 2f, STROKE);

        RoundedRectangleRenderer thin = rounded(3, 2, 20f);
        thin.setColorMode(NONE);
        stroke(thin, 1f, 1f, STROKE);

        RoundedRectangleRenderer sharpEdges = rounded(4, 2, 30f);
        sharpEdges.setColorMode(FILL);
        sharpEdges.setAntialiasing(false);
        stroke(sharpEdges, 4f, 2f, STROKE);

        help = new Text();
        help.setReferenceCamera(hudCamera);
        help.setFontSize(18)
                .setReference(EDirection.LeftUp)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(10, 10));
        updateHelp();
    }

    // A rounded rectangle in the cell (column, row) of the grid, centered on it
    private RoundedRectangleRenderer rounded(int column, int row, float radius) {
        RoundedRectangleRenderer shape = new RoundedRectangleRenderer();
        shape.setCornerRadius(radius);
        shape.setReferenceDirection(EDirection.None);
        shape.setSizePix(130, 90);
        place(shape, column, row);
        return shape;
    }

    private void place(VectorShape2DRenderer shape, int column, int row) {
        shape.setPosition(new Vector2f((column - 2) * CELL, (row - 1) * CELL * 0.8f));
        shapes.add(shape);
        strokeWidths.add(new float[]{0f, 0f});
    }

    // The stroke of the last shape placed: its width in world units and in pixels of the screen
    private void stroke(VectorShape2DRenderer shape, float worldWidth, float pixelWidth, Color4f color) {
        strokeWidths.set(shapes.indexOf(shape), new float[]{worldWidth, pixelWidth});
        shape.setStrokeColor(color);
        applyStroke(shape, shapes.indexOf(shape));
    }

    private void applyStroke(VectorShape2DRenderer shape, int index) {
        float[] widths = strokeWidths.get(index);
        shape.setStrokeUnit(screenUnits ? EStrokeUnit.SCREEN_PIXELS : EStrokeUnit.WORLD);
        shape.setStrokeWidth(screenUnits ? widths[1] : widths[0]);
    }

    private void resetCamera() {
        Vector2i size = context().getWindow().getInfo().getVirtualSizeRef();
        camera2D().setZoomReference(EDirection.None);
        camera2D().setRotation(0f);
        camera2D().setZoomLevel(1f);
        camera2D().setPos(new Vector2f(-size.x / 2f, -size.y / 2f));
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
                applyStroke(shapes.get(i), i);
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
