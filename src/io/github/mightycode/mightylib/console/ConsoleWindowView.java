package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowView;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

/**
 * The console in a window of its own: black, white text, the whole log scrollable. Its inputs are read by Console.
 */
final class ConsoleWindowView extends WindowView {
    private static final Color4f BACKGROUND = new Color4f(0f, 0f, 0f, 1f);
    private static final Color4f INPUT_BACKGROUND = new Color4f(0.12f, 0.12f, 0.12f, 1f);

    private final Console console;
    private ConsoleView consoleView;
    private ConsolePanel panel;

    ConsoleWindowView(Console console) {
        this.console = console;
    }

    ConsolePanel getPanel() { return panel; }

    @Override
    protected void launch(String[] args) {
        consoleView = new ConsoleView(engine(), window());
        try (Views.Scope ignored = Views.enter(consoleView)) {
            panel = new ConsolePanel(consoleView, console.getLog(), console::getViewerLevel, console.getEditor(),
                    console.getFontName(), BACKGROUND, INPUT_BACKGROUND);
        }
    }

    @Override
    public void display() {
        // Drawn straight on the window, in its real pixels
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        Vector2i size = window().getWindow().getInfo().getSizeRef();
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        boolean focused = glfwGetWindowAttrib(window().getWindow().getInfo().getWindowId(), GLFW_FOCUSED) == 1;
        try (Views.Scope ignored = Views.enter(consoleView)) {
            panel.display(0, 0, size.x, size.y, focused);
        }
    }

    @Override
    public void unload() {
        if (panel != null) {
            try (Views.Scope ignored = Views.enter(consoleView)) {
                panel.unload();
            }
            panel = null;
        }

        super.unload();
        console.onWindowClosed(this);
    }
}
