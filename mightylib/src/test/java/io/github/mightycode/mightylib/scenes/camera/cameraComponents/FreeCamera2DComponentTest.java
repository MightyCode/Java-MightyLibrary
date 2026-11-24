package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.main.utils.GameTimeControl;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// The free 2D camera without window: moves at the same speed on the screen whatever the zoom, drag, wheel
class FreeCamera2DComponentTest {
    private static final InputAction LEFT = InputAction.of("test:left");
    private static final InputAction RIGHT = InputAction.of("test:right");
    private static final InputAction UP = InputAction.of("test:up");
    private static final InputAction DOWN = InputAction.of("test:down");
    private static final InputAction QUICK = InputAction.of("test:quick");
    private static final InputAction DRAG = InputAction.of("test:drag");

    // A mouse without window whose cursor is given
    private static final class TestMouse extends MouseManager {
        double x = 500, y = 300;

        @Override
        protected boolean realButton(int buttonId) {
            return false;
        }

        @Override
        protected void realCursor(double[] x, double[] y) {
            x[0] = this.x;
            y[0] = this.y;
        }
    }

    private final Set<InputAction> held = new HashSet<>();
    private final Set<InputAction> pressed = new HashSet<>();
    private final Set<InputAction> released = new HashSet<>();
    private final TestMouse mouse = new TestMouse();
    private Camera2D camera;
    private FreeCamera2DComponent component;

    // Only the reads the component makes
    private final IInputManager inputs = (IInputManager) Proxy.newProxyInstance(
            IInputManager.class.getClassLoader(), new Class<?>[]{IInputManager.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getValue":
                        return held.contains((InputAction) args[0]) ? 1f : 0f;
                    case "getState":
                        return held.contains((InputAction) args[0]);
                    case "inputPressed":
                        return pressed.contains((InputAction) args[0]);
                    case "inputReleased":
                        return released.contains((InputAction) args[0]);
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });

    // A window that was never created (virtual size 0): enough for the camera, no GL
    private static WindowInfo windowInfo() throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @BeforeEach
    void create() throws ReflectiveOperationException {
        // A tick of a tenth of a second
        GameTimeControl.setDeltaTime(0.1f);
        camera = new Camera2D(windowInfo(), new Vector2f());
        component = new FreeCamera2DComponent(inputs, mouse, camera, new Vector2i(1280, 720), QUICK);
        mouse.postUpdate();
    }

    @AfterEach
    void reset() {
        GameTimeControl.reset();
    }

    private void tick() {
        mouse.postUpdate();
        component.update();
        pressed.clear();
        released.clear();
    }

    @Test
    void withoutActionsNothingMovesNorInteracts() {
        held.add(RIGHT);
        mouse.simulateScroll(0, 0);
        tick();

        assertEquals(new Vector2f(), camera.getCamPosCopy());
        assertEquals(new Vector2f(1f), camera.getZoomLevel());
        assertFalse(component.isInteracting());
    }

    @Test
    void theMovesFollowTheActionsAtTheSpeedOfTheZoom() {
        component.setMove(LEFT, RIGHT, UP, DOWN);
        held.add(RIGHT);
        tick();
        assertEquals(60f, camera.getCamPosCopy().x, 1e-3f);
        assertEquals(0f, camera.getCamPosCopy().y, 1e-3f);
        assertTrue(component.isInteracting());

        held.clear();
        held.add(UP);
        tick();
        assertEquals(-60f, camera.getCamPosCopy().y, 1e-3f);

        // The same speed on the screen at zoom 2: half the distance of the world
        held.clear();
        camera.setZoomLevel(2f);
        camera.setPos(new Vector2f());
        held.add(LEFT);
        tick();
        assertEquals(-30f, camera.getCamPosCopy().x, 1e-3f);

        held.clear();
        tick();
        assertFalse(component.isInteracting());
    }

    @Test
    void aDiagonalIsNotFasterAndTheQuickActionDoublesTheSpeed() {
        component.setMove(LEFT, RIGHT, UP, DOWN);
        held.add(RIGHT);
        held.add(DOWN);
        tick();
        assertEquals(60f, camera.getCamPosCopy().length(), 1e-2f);

        camera.setPos(new Vector2f());
        held.clear();
        held.add(RIGHT);
        held.add(QUICK);
        tick();
        assertEquals(120f, camera.getCamPosCopy().x, 1e-3f);
    }

    // The world follows the mouse while the action is held: 200 pixels to the left, the camera 200 / zoom to the right
    @Test
    void theDragMovesTheCameraByTheMouseOverTheZoom() {
        component.setDrag(DRAG);
        pressed.add(DRAG);
        held.add(DRAG);
        tick();
        // The press only starts the drag
        assertEquals(new Vector2f(), camera.getCamPosCopy());

        mouse.x = 300;
        tick();
        assertEquals(200f, camera.getCamPosCopy().x, 1e-3f);
        assertTrue(component.isInteracting());

        camera.setZoomLevel(2f);
        camera.setPos(new Vector2f());
        mouse.x = 200;
        tick();
        assertEquals(50f, camera.getCamPosCopy().x, 1e-3f);

        held.clear();
        released.add(DRAG);
        tick();
        mouse.x = 0;
        tick();
        assertEquals(50f, camera.getCamPosCopy().x, 1e-3f);
        assertFalse(component.isInteracting());
    }

    @Test
    void theWheelZoomsUnlessItIsOff() {
        mouse.simulateScroll(0, 1);
        tick();
        assertEquals(1.1f, camera.getZoomLevel().x, 1e-4f);
        assertEquals(1.1f, camera.getZoomLevel().y, 1e-4f);
        assertTrue(component.isInteracting());

        tick();
        assertFalse(component.isInteracting());

        component.setWheelZoom(false);
        mouse.simulateScroll(0, 1);
        tick();
        assertEquals(1.1f, camera.getZoomLevel().x, 1e-4f);
        assertFalse(component.isInteracting());
    }

    @Test
    void theActionsAreRequired() {
        assertThrows(NullPointerException.class, () -> component.setMove(LEFT, RIGHT, UP, null));
        assertThrows(NullPointerException.class, () -> component.setDrag(null));
        assertThrows(NullPointerException.class, () -> new FreeCamera2DComponent(inputs, mouse, camera,
                new Vector2i(1280, 720), null));
    }
}
