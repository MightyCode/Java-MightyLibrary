package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.gui.EnumSelector;
import io.github.mightycode.mightylib.graphics.gui.StrSelector;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;
import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static io.github.mightycode.mightylib.test.BatchPictures.inScene;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

/**
 * A selector over values: the index kept in the values (looping or at the ends), the value and its name, a value
 * chosen by its name, the arrows clicked by the mouse (the click action of the selector), updated until the display.
 */
@MightySceneTest(config = ValueSelectorGLTest.Config.class)
class ValueSelectorGLTest {
    private static final InputAction CLICK = InputAction.of("selector:click");

    // The engine of the kit tests with a click action on the left button of the mouse
    static final class Config implements Supplier<EngineConfig> {
        @Override
        public EngineConfig get() {
            return new SceneRunnerTest.Config().get().addPlugin(new IPlugin() {
                @Override
                public String namespace() {
                    return "selector";
                }

                @Override
                public void register(EngineRegistry registry) {
                    registry.inputs().action(CLICK, new InputSimple(GLFW_MOUSE_BUTTON_1, EInputType.Mouse));
                }
            });
        }
    }

    @Test
    void theIndexLoopsOrStaysAtTheEnds(SceneRunner runner) {
        inScene(runner, scene -> {
            try (Views.Scope ignored = Views.enter(scene)) {
                StrSelector selector = new StrSelector(new String[]{"low", "medium", "high"}, scene.context());
                try {
                    assertEquals(3, selector.size());
                    assertEquals("low", selector.getSelected());
                    selector.setIndex(-1);
                    assertEquals("high", selector.getSelected(), "looping: before the first, the last");
                    selector.setIndex(3);
                    assertEquals("low", selector.getSelected(), "looping: after the last, the first");

                    // The selection is kept (not the first value, the default one)
                    selector.setIndex(1);
                    selector.setCanLoop(false);
                    assertEquals("medium", selector.getSelected(), "kept by setCanLoop");
                    selector.setIndex(-1);
                    assertEquals("low", selector.getSelected(), "kept at the first");
                    selector.setIndex(7);
                    assertEquals("high", selector.getSelected(), "kept at the last");

                    assertTrue(selector.setIfExists("medium"));
                    assertEquals("medium", selector.getSelectedName());
                    assertFalse(selector.setIfExists("ultra"));
                    assertEquals("medium", selector.getSelected());
                } finally {
                    selector.unload();
                }
            }
        });
    }

    @Test
    void anEnumIsShownByTheNamesOfItsValues(SceneRunner runner) {
        inScene(runner, scene -> {
            try (Views.Scope ignored = Views.enter(scene)) {
                EnumSelector<EDirection> selector = new EnumSelector<>(
                        new EDirection[]{EDirection.Left, EDirection.Right}, scene.context());
                try {
                    selector.setIndex(1);
                    assertEquals(EDirection.Right, selector.getSelected());
                    assertEquals("Right", selector.getSelectedName());
                    assertArrayEquals(new EDirection[]{EDirection.Left, EDirection.Right}, selector.getValues());
                } finally {
                    selector.unload();
                }
            }
        });
    }

    // The selector from (100, 100), 400 wide: its arrows are centered at 21 % and 82 % of its width, a little below
    // its top (an eighth of its height, 8 % of the window)
    @Test
    void theArrowsChooseTheNeighbourValueUntilTheDisplay(SceneRunner runner) {
        inScene(runner, scene -> {
            StrSelector selector;
            float arrowY;
            try (Views.Scope ignored = Views.enter(scene)) {
                selector = new StrSelector(new String[]{"a", "b", "c"}, scene.context(), new Vector2f(100f, 100f),
                        400f);
                selector.init(CLICK);
                arrowY = 100f + scene.context().getWindow().getInfo().getVirtualSizeCopy().y * 0.08f * 0.125f + 12f;
            }
            try {
                click(runner, scene, selector, 428f, arrowY);
                assertEquals("b", selector.getSelected(), "right arrow");
                assertTrue(selector.isUpdated());
                try (Views.Scope ignored = Views.enter(scene)) {
                    selector.display();
                }
                assertFalse(selector.isUpdated(), "seen by the display");

                click(runner, scene, selector, 184f, arrowY);
                click(runner, scene, selector, 184f, arrowY);
                assertEquals("c", selector.getSelected(), "left arrow twice, looping");

                // Between the arrows: nothing
                click(runner, scene, selector, 306f, arrowY);
                assertEquals("c", selector.getSelected());
            } finally {
                try (Views.Scope ignored = Views.enter(scene)) {
                    selector.unload();
                }
            }
        });
    }

    // A click of the left button during one tick at this place of the window, the selector updated in that tick
    private static void click(SceneRunner runner, Scene scene, StrSelector selector, float x, float y) {
        runner.click(GLFW_MOUSE_BUTTON_1, x, y).frames(1);
        try (Views.Scope ignored = Views.enter(scene)) {
            selector.update();
        }
        runner.frames(1);
    }
}
