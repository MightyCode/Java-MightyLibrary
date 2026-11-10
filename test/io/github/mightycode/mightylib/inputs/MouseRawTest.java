package io.github.mightycode.mightylib.inputs;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

// The owner of the capture (the console) reads the mouse the game no longer sees, simulation included
class MouseRawTest {
    // A mouse without window: the real cursor and buttons are given
    private static final class FakeMouse extends MouseManager {
        double x = 50;
        double y = 60;
        boolean left;

        @Override
        protected boolean realButton(int buttonId) {
            return buttonId == GLFW_MOUSE_BUTTON_LEFT && left;
        }

        @Override
        protected void realCursor(double[] x, double[] y) {
            x[0] = this.x;
            y[0] = this.y;
        }
    }

    @Test
    void rawReadsIgnoreTheCapture() {
        FakeMouse mouse = new FakeMouse();
        mouse.setCaptured(true);
        mouse.left = true;
        mouse.postUpdate();

        assertFalse(mouse.getButton(GLFW_MOUSE_BUTTON_LEFT));
        assertFalse(mouse.buttonPressed(GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(mouse.posX() < -1000);
        assertTrue(mouse.rawButtonPressed(GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(mouse.getRawButton(GLFW_MOUSE_BUTTON_LEFT));
        assertEquals(new Vector2f(50, 60), mouse.getRawPos());

        mouse.postUpdate();
        assertFalse(mouse.rawButtonPressed(GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(mouse.getRawButton(GLFW_MOUSE_BUTTON_LEFT));

        // The capture ends with the button held: no press for the game
        mouse.setCaptured(false);
        mouse.postUpdate();
        assertTrue(mouse.getButton(GLFW_MOUSE_BUTTON_LEFT));
        assertFalse(mouse.buttonPressed(GLFW_MOUSE_BUTTON_LEFT));

        mouse.setCaptured(true);
        mouse.left = false;
        mouse.postUpdate();
        assertTrue(mouse.rawButtonReleased(GLFW_MOUSE_BUTTON_LEFT));
        assertFalse(mouse.buttonReleased(GLFW_MOUSE_BUTTON_LEFT));
    }

    @Test
    void rawReadsFollowTheSimulation() {
        FakeMouse mouse = new FakeMouse();
        mouse.postUpdate();
        mouse.setCaptured(true);

        mouse.simulateMove(10, 20);
        mouse.simulateButton(GLFW_MOUSE_BUTTON_LEFT, 1);
        mouse.postUpdate();
        assertEquals(new Vector2f(10, 20), mouse.getRawPos());
        assertTrue(mouse.rawButtonPressed(GLFW_MOUSE_BUTTON_LEFT));

        // The real cursor moves: it takes over again
        mouse.x = 70;
        mouse.postUpdate();
        assertEquals(new Vector2f(70, 60), mouse.getRawPos());
    }
}
