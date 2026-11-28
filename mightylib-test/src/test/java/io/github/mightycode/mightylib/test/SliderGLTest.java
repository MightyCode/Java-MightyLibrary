package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.gui.HorizontalSlider;
import io.github.mightycode.mightylib.graphics.gui.Slider;
import io.github.mightycode.mightylib.graphics.gui.VerticalSlider;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The value of a slider dragged by the mouse: its button is pressed where it is (centered on the start of the bar at
 * the minimum), then follows the mouse along the bar, the value the place of its center on the bar, kept between the
 * minimum and the maximum, rounded to its precision; the mouse is read in the world of the reference camera.
 * Inputs given by hand (an action pressed, released, a cursor placed), the renderers of the scene of the kit.
 */
@MightySceneTest(config = SceneRunnerTest.Config.class)
class SliderGLTest {
    private static final InputAction CLICK = InputAction.of("test:click");
    private static final float EPSILON = 1e-4f;
    // The bar of the horizontal slider: 200 pixels from (100, 100), 50 high
    private static final Vector2f POSITION = new Vector2f(100f, 100f);

    // A mouse without window whose cursor is given
    private static final class TestMouse extends MouseManager {
        double x, y;

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

    private static final class Fixture {
        final Set<InputAction> pressed = new HashSet<>();
        final Set<InputAction> released = new HashSet<>();
        final TestMouse mouse = new TestMouse();
        final IInputManager inputs = (IInputManager) Proxy.newProxyInstance(
                IInputManager.class.getClassLoader(), new Class<?>[]{IInputManager.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "inputPressed":
                            return pressed.contains((InputAction) args[0]);
                        case "inputReleased":
                            return released.contains((InputAction) args[0]);
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        Slider slider;

        // The mouse placed (screen pixels), the slider updated, the presses and releases dropped
        void mouse(double x, double y) {
            mouse.x = x;
            mouse.y = y;
            mouse.postUpdate();
            slider.update();
            pressed.clear();
            released.clear();
        }

        void press(double x, double y) {
            pressed.add(CLICK);
            mouse(x, y);
        }

        void release(double x, double y) {
            released.add(CLICK);
            mouse(x, y);
        }
    }

    private static void withSlider(SceneRunner runner, boolean horizontal, Vector2f cameraPosition,
                                   Consumer<Fixture> test) {
        inScene(runner, scene -> {
            try (Views.Scope ignored = Views.enter(scene)) {
                Fixture fixture = new Fixture();
                Camera2D camera = new Camera2D(scene.context().getWindow().getInfo(), cameraPosition);
                fixture.slider = horizontal
                        ? new HorizontalSlider(camera, fixture.inputs, fixture.mouse, new Vector2f(POSITION),
                        new Vector2f(200f, 50f), 0f, 10f)
                        : new VerticalSlider(camera, fixture.inputs, fixture.mouse, new Vector2f(POSITION),
                        new Vector2f(50f, 200f), 0f, 10f);
                fixture.slider.init(CLICK);
                try {
                    test.accept(fixture);
                } finally {
                    fixture.slider.unload();
                }
            }
        });
    }

    @Test
    void aHorizontalSliderFollowsTheMouseBetweenItsBounds(SceneRunner runner) {
        withSlider(runner, true, new Vector2f(), fixture -> {
            assertEquals(0.0, fixture.slider.getCurrentValue(), EPSILON);

            // Beside the button: nothing
            fixture.press(200, 125);
            assertFalse(fixture.slider.isGettingDragged());
            fixture.release(200, 125);

            fixture.press(100, 125);
            assertTrue(fixture.slider.isGettingDragged());
            fixture.mouse(200, 125);
            assertEquals(5.0, fixture.slider.getCurrentValue(), EPSILON);
            fixture.mouse(150, 400);
            assertEquals(2.5, fixture.slider.getCurrentValue(), EPSILON);
            fixture.mouse(900, 125);
            assertEquals(10.0, fixture.slider.getCurrentValue(), EPSILON);
            fixture.mouse(-50, 125);
            assertEquals(0.0, fixture.slider.getCurrentValue(), EPSILON);

            fixture.mouse(160, 125);
            fixture.release(160, 125);
            assertFalse(fixture.slider.isGettingDragged());
            fixture.mouse(280, 125);
            assertEquals(3.0, fixture.slider.getCurrentValue(), EPSILON);
        });
    }

    @Test
    void aVerticalSliderFollowsTheMouseDownItsBar(SceneRunner runner) {
        withSlider(runner, false, new Vector2f(), fixture -> {
            fixture.press(125, 100);
            assertTrue(fixture.slider.isGettingDragged());
            fixture.mouse(125, 250);
            assertEquals(7.5, fixture.slider.getCurrentValue(), EPSILON);
            fixture.mouse(400, 120);
            assertEquals(1.0, fixture.slider.getCurrentValue(), EPSILON);
        });
    }

    @Test
    void theDraggedValueIsRoundedToThePrecision(SceneRunner runner) {
        withSlider(runner, true, new Vector2f(), fixture -> {
            fixture.slider.setMaxPrecision(0);
            assertEquals(0, fixture.slider.getMaxPrecision());
            fixture.press(100, 125);
            fixture.mouse(174, 125);
            assertEquals(4.0, fixture.slider.getCurrentValue(), EPSILON);
            fixture.slider.setMaxPrecision(1);
            fixture.mouse(174, 125);
            assertEquals(3.7, fixture.slider.getCurrentValue(), EPSILON);
        });
    }

    // setValue is kept between the bounds and moves the button: it is pressed at its new place
    @Test
    void aValueSetMovesTheButton(SceneRunner runner) {
        withSlider(runner, true, new Vector2f(), fixture -> {
            fixture.slider.setValue(42);
            assertEquals(10.0, fixture.slider.getCurrentValue(), EPSILON);
            fixture.slider.setValue(-3);
            assertEquals(0.0, fixture.slider.getCurrentValue(), EPSILON);

            fixture.slider.setValue(2.5);
            fixture.press(150, 125);
            assertTrue(fixture.slider.isGettingDragged(), "the button at a quarter of the bar");
            fixture.release(150, 125);

            // Inverted: the maximum at the start of the bar, the value kept, the button at its mirrored place
            fixture.slider.invertMinMax();
            assertEquals(10.0, fixture.slider.getMinValue(), EPSILON);
            assertEquals(0.0, fixture.slider.getMaxValue(), EPSILON);
            assertEquals(2.5, fixture.slider.getCurrentValue(), EPSILON);
            fixture.press(250, 125);
            assertTrue(fixture.slider.isGettingDragged(), "the button at three quarters of the bar");
            fixture.mouse(200, 125);
            assertEquals(5.0, fixture.slider.getCurrentValue(), EPSILON);
        });
    }

    // A reference camera moved by 50 pixels: the bar is 50 pixels further left on the screen, pressed and dragged
    // there (the drag read the mouse on the screen, the press in the world)
    @Test
    void theMouseIsReadInTheWorldOfTheReferenceCamera(SceneRunner runner) {
        withSlider(runner, true, new Vector2f(50f, 0f), fixture -> {
            fixture.press(50, 125);
            assertTrue(fixture.slider.isGettingDragged());
            fixture.mouse(150, 125);
            assertEquals(5.0, fixture.slider.getCurrentValue(), EPSILON);
        });
    }
}
