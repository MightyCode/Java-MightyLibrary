package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.DisplaySettings;
import io.github.mightycode.mightylib.main.EWindowMode;
import io.github.mightycode.mightylib.main.WindowConfig;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.main.WindowView;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The display modes, in the window that has the focus: F1 windowed, F2 exclusive fullscreen, F3 borderless,
 * B border and Z resizing (windowed), L a 1280x720 fullscreen resolution instead of the one of the screen.
 * W opens a second window with the same keys: a second exclusive fullscreen on a screen becomes borderless.
 * Escape goes back to the menu (closes the second window in it).
 */
public class TestDisplayModesScene extends Scene {
    private static final int VIRTUAL_WIDTH = 1280;
    private static final int VIRTUAL_HEIGHT = 720;

    private Text help;
    private Text status;
    private WindowContext second;

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.16f, 0.22f);

        help = text(28, new Vector2f(VIRTUAL_WIDTH / 2f, 40));
        help.setText("F1 windowed   F2 fullscreen   F3 borderless   B border   Z resizable\n"
                + "L fullscreen 1280x720 / screen   W second window   ESC menu");

        status = text(32, new Vector2f(VIRTUAL_WIDTH / 2f, 300));
    }

    private static Text text(int size, Vector2f position) {
        Text text = new Text();
        text.setFont("bahnschrift")
                .setFontSize(size)
                .setReference(EDirection.Up)
                .setAlignment(ETextAlignment.Center)
                .setPosition(position);
        return text;
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = mainContext.getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (keyboard.keyPressed(GLFW_KEY_W) && !isOpen(second)) {
            second = openWindow(new WindowConfig("Second window").setSize(640, 360)
                    .setVirtualSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT), new SecondView());
        }

        ChangeDisplay(mainContext);
        status.setText(Describe(mainContext));
    }

    // A closed window is not in the list of the context any more
    private boolean isOpen(WindowContext window) {
        return window != null && mainContext.getContext().getWindows().contains(window);
    }

    // The keys of the scene, read in the given window
    static void ChangeDisplay(WindowContext window) {
        KeyboardManager keyboard = window.getKeyboardManager();

        if (keyboard.keyPressed(GLFW_KEY_F1))
            window.setWindowMode(EWindowMode.WINDOWED);
        if (keyboard.keyPressed(GLFW_KEY_F2))
            window.setWindowMode(EWindowMode.FULLSCREEN);
        if (keyboard.keyPressed(GLFW_KEY_F3))
            window.setWindowMode(EWindowMode.BORDERLESS);

        DisplaySettings display = window.getDisplay();
        if (keyboard.keyPressed(GLFW_KEY_B))
            window.setDisplay(display.setDecorated(!display.isDecorated()));
        if (keyboard.keyPressed(GLFW_KEY_Z))
            window.setDisplay(display.setResizable(!display.isResizable()));
        if (keyboard.keyPressed(GLFW_KEY_L))
            window.setDisplay(display.getResolution() == null ? display.setResolution(1280, 720) : display.clearResolution());
    }

    static String Describe(WindowContext window) {
        Vector2i size = window.getWindow().getInfo().getSizeRef();
        return window.getName() + "\n" + window.getDisplay() + "\n" + size.x + "x" + size.y;
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        help.display();
        status.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        help.unload();
        status.unload();
    }

    // The same keys, for this window
    private static class SecondView extends WindowView {
        private Text status;

        @Override
        protected void launch(String[] args) {
            setClearColor(0.22f, 0.14f, 0.1f);
            status = text(48, new Vector2f(VIRTUAL_WIDTH / 2f, 200));
        }

        @Override
        public void update() {
            super.update();

            if (window().getKeyboardManager().keyPressed(GLFW_KEY_ESCAPE)) {
                close();
                return;
            }

            ChangeDisplay(window());
            status.setText(Describe(window()));
        }

        @Override
        public void display() {
            setVirtualScene();
            clear();
            status.display();
            setAndDisplayRealScene();
        }

        @Override
        public void unload() {
            super.unload();
            status.unload();
        }
    }
}
