package io.github.mightycode.mightylib.inputs;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

// A mouse without window: the real cursor and the left button are given
final class FakeMouse extends MouseManager {
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
