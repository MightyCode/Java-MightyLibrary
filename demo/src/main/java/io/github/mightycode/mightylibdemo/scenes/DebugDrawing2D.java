package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.debug.DebugDraw2D;
import io.github.mightycode.mightylib.graphics.debug.DebugDrawHandle;
import io.github.mightycode.mightylib.graphics.debug.DebugLifetimes;
import io.github.mightycode.mightylib.graphics.debug.EDebugSpace;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The debug drawings (IView.debugDraw), each lifetime: a point that moves, its cross and its arrow redrawn at each
 * frame (one frame), its trail kept 3 seconds of game time, a line to it while Space is held (while true), a circle
 * for 120 frames (C), a rectangle until removed (R again), texts on the screen. The camera moves and zooms (Z Q S D, arrows, right drag, wheel):
 * the world drawings follow it, the screen ones stay. The command debugdraw switches them off and counts them.
 */
public class DebugDrawing2D extends Scene {
    private static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);
    private static final Color4f YELLOW = new Color4f(1f, 0.85f, 0.1f, 1f);
    private static final Color4f RED = new Color4f(1f, 0.25f, 0.2f, 1f);
    private static final Color4f GREEN = new Color4f(0.3f, 1f, 0.4f, 1f);
    private static final Color4f BLUE = new Color4f(0.4f, 0.6f, 1f, 1f);
    private static final List<Vector2f> STAR = List.of(new Vector2f(0, -60), new Vector2f(18, -18),
            new Vector2f(60, -10), new Vector2f(25, 15), new Vector2f(35, 60), new Vector2f(0, 35),
            new Vector2f(-35, 60), new Vector2f(-25, 15), new Vector2f(-60, -10), new Vector2f(-18, -18));

    private float time;
    private float trailTimer;
    private final Vector2f point = new Vector2f();
    private final Vector2f previous = new Vector2f();
    private DebugDrawHandle rectangle = DebugDrawHandle.NONE;

    @Override
    public void launch(String[] args) {
        setClearColor(30, 34, 44, 1f);
        DemoCamera.free(this);
        computePoint(0f, previous);
    }

    // The point on its curve around the center of the screen
    private void computePoint(float at, Vector2f into) {
        into.set(640 + (float) Math.sin(at * 1.3) * 380, 360 + (float) Math.sin(at * 2.1) * 220);
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = context().getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        time += GameTime.DeltaTime();
        computePoint(time, point);
        DebugDraw2D debug = debugDraw();

        // One frame (the default): asked again at each frame, it follows the point
        debug.cross(point.x, point.y, 24, WHITE);
        debug.arrow(EDebugSpace.WORLD, 640, 360, point.x, point.y, YELLOW, 2, DebugLifetimes.oneFrame());

        // The trail: a segment every 50 ms, kept 3 seconds of game time
        trailTimer -= GameTime.DeltaTime();
        if (trailTimer <= 0) {
            trailTimer = 0.05f;
            debug.line(EDebugSpace.WORLD, previous.x, previous.y, point.x, point.y, BLUE, 3,
                    DebugLifetimes.seconds(3f));
            previous.set(point);
        }

        // While Space is held
        if (keyboard.keyPressed(GLFW_KEY_SPACE))
            debug.line(EDebugSpace.WORLD, 640, 360, point.x, point.y, GREEN, 4,
                    DebugLifetimes.whileTrue(() -> keyboard.getKeyState(GLFW_KEY_SPACE)));

        // 120 frames
        if (keyboard.keyPressed(GLFW_KEY_C))
            debug.circle(EDebugSpace.WORLD, point.x, point.y, 40, RED, 2, DebugLifetimes.frames(120));

        // Until removed: R puts it, R again removes it
        if (keyboard.keyPressed(GLFW_KEY_R)) {
            if (rectangle == DebugDrawHandle.NONE || rectangle.isRemoved()) {
                rectangle = debug.rect(EDebugSpace.WORLD, point.x - 50, point.y - 30, 100, 60, GREEN, 2,
                        DebugLifetimes.untilRemoved());
            } else {
                rectangle.remove();
                rectangle = DebugDrawHandle.NONE;
            }
        }

        // A star at the center of the world, closed polyline
        debug.polyline(EDebugSpace.WORLD, translated(STAR), true, YELLOW, 1, DebugLifetimes.oneFrame());

        // On the screen: they stay where they are whatever the camera
        debug.text(EDebugSpace.SCREEN, 10, 10, "Space: line while held   C: circle 120 frames   R: rectangle until"
                + " removed\nZQSD/arrows: move   wheel: zoom   trail: 3 seconds   command debugdraw off|on|list   ESC: menu", WHITE,
                18, DebugLifetimes.oneFrame());
        debug.text(EDebugSpace.SCREEN, 10, 60, debug.size() + " drawings alive", GREEN, 18,
                DebugLifetimes.oneFrame());
        debug.rect(EDebugSpace.SCREEN, 4, 4, 1272, 712, BLUE, 2, DebugLifetimes.oneFrame());
    }

    // The star around the center (copied by the drawing: the same points reused each frame)
    private final List<Vector2f> star = List.of(new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f(),
            new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f());

    private List<Vector2f> translated(List<Vector2f> points) {
        for (int i = 0; i < points.size(); ++i)
            star.get(i).set(points.get(i)).add(640, 360);
        return star;
    }

    @Override
    public void display() {
        clear();
        super.display();
    }
}
