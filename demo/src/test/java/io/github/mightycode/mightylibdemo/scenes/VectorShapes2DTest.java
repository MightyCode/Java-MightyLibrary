package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.inputs.internal.InputSimulation;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.test.MightySceneTest;
import io.github.mightycode.mightylib.test.SceneRunner;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.DemoTestConfig;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

/**
 * VectorShapes2D with GL (tag gl, hidden window): the renderer named under the mouse, the tooltip at the mouse, the
 * camera moved by a right button drag and zoomed by the wheel; and the box of a moved Text.
 */
@MightySceneTest(config = DemoTestConfig.class)
class VectorShapes2DTest {
    private static final float EPSILON = 1e-3f;

    private static VectorShapes2D start(SceneRunner runner) {
        runner.start("VectorShapes2D").untilLoaded();
        VectorShapes2D scene = assertInstanceOf(VectorShapes2D.class, runner.scene());
        runner.frames(2);
        return scene;
    }

    @Test
    void theTooltipNamesTheRendererAtTheMouse(SceneRunner runner) {
        VectorShapes2D scene = start(runner);
        runner.mouse(200, 150).frames(3);

        // Over the background grid at least
        assertNotNull(scene.hoveredLabel());
        assertTrue(scene.hoveredLabel().endsWith("Renderer") || scene.hoveredLabel().contains("Renderer ("),
                scene.hoveredLabel());

        // The box of the text follows the mouse (Text.setPosition), the background around it
        Vector2f mouse = scene.context().getMouseManager().pos();
        assertEquals(mouse.x + 18f, scene.tooltipTextPosition().x, EPSILON);
        assertEquals(mouse.y + 18f, scene.tooltipTextPosition().y, EPSILON);
        assertEquals(scene.tooltipTextPosition().x - 6f, scene.tooltipBackgroundPosition().x, EPSILON);

        runner.mouse(260, 190).frames(3);
        Vector2f moved = scene.context().getMouseManager().pos();
        assertEquals(moved.x + 18f, scene.tooltipTextPosition().x, EPSILON);
    }

    @Test
    void theRightButtonDragsTheCamera(SceneRunner runner) {
        VectorShapes2D scene = start(runner);
        MouseManager mouse = scene.context().getMouseManager();

        runner.mouse(300, 200).frames(2);
        Vector2f from = mouse.pos();
        Vector2f cameraBefore = scene.camera2D().getCamPosCopy();
        float zoom = scene.camera2D().getZoomLevel().x;

        // Through the action demo:right_click: a simulated button reaches it
        mouse.simulateButton(GLFW_MOUSE_BUTTON_RIGHT, InputSimulation.UNTIL_RELEASE);
        runner.frames(2).mouse(380, 260).frames(2);
        mouse.simulateButtonRelease(GLFW_MOUSE_BUTTON_RIGHT);
        runner.frames(2);

        Vector2f dragged = mouse.pos().sub(from);
        Vector2f expected = new Vector2f(cameraBefore).sub(new Vector2f(dragged).div(zoom));
        assertEquals(expected.x, scene.camera2D().getCamPosCopy().x, EPSILON);
        assertEquals(expected.y, scene.camera2D().getCamPosCopy().y, EPSILON);
    }

    @Test
    void theWheelZooms(SceneRunner runner) {
        VectorShapes2D scene = start(runner);
        float before = scene.camera2D().getZoomLevel().x;

        runner.scroll(0, 2).frames(2);
        assertEquals(before * 1.15f * 1.15f, scene.camera2D().getZoomLevel().x, 1e-4f);
    }

    // A text whose position changes, without changing its content
    static final class MovedTextScene extends Scene {
        Text text;

        @Override
        public void launch(String[] args) {
            text = new Text();
            text.setFontSize(20).setReference(EDirection.LeftUp).setText("Box");
            text.setPosition(new Vector2f(10, 20));
        }

        @Override
        public void unload() {
            super.unload();
            text.unload();
        }
    }

    @Test
    void theBoxOfATextFollowsItsPosition(SceneRunner runner) {
        runner.start(MovedTextScene::new).untilLoaded();
        Text text = assertInstanceOf(MovedTextScene.class, runner.scene()).text;
        assertEquals(new Vector2f(10, 20), text.leftUpPosition());

        text.setPosition(new Vector2f(100, 50));
        assertEquals(new Vector2f(100, 50), text.leftUpPosition());

        // setX and setY of Renderer go through setPosition too
        text.setX(30);
        text.setY(40);
        assertEquals(new Vector2f(30, 40), text.leftUpPosition());

        // Centered: the box moves with the position, half its size before it
        text.setReference(EDirection.None);
        text.setPosition(new Vector2f(200, 200));
        assertEquals(200 - text.size().x / 2, text.leftUpPosition().x, EPSILON);
        assertEquals(200 - text.size().y / 2, text.leftUpPosition().y, EPSILON);
    }
}
