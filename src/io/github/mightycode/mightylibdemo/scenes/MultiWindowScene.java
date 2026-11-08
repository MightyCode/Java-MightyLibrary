package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.WindowConfig;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.main.WindowView;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Several windows: a dialog that blocks the main window (D), a tool window that shows the same renderer as the
 * main window (T), and a second context with its own scene (C). Escape closes the window it is pressed in;
 * in the main window it goes back to the menu, which closes the windows this scene opened.
 */
public class MultiWindowScene extends Scene {
    private static final int VIRTUAL_WIDTH = 1280;
    private static final int VIRTUAL_HEIGHT = 720;

    private Text help;
    private Text status;
    // Created for this scene's window, drawn in the tool window as well: its vertex array is made for each window
    private RectangleRenderer square;

    private WindowContext dialog;
    private WindowContext tools;
    private int contexts;

    @Override
    public void launch(String[] args) {
        setClearColor(0.12f, 0.14f, 0.2f);

        help = text(32, new Vector2f(VIRTUAL_WIDTH / 2f, 40));
        help.setText("D : modal dialog   T : tool window   C : second context   ESC : menu");

        status = text(28, new Vector2f(VIRTUAL_WIDTH / 2f, 100));

        square = new RectangleRenderer("texture2D");
        square.setMainTextureChannel("painting");
        square.setSizePix(360, 360);
        square.setPosition(new Vector2f(VIRTUAL_WIDTH / 2f - 180, 240));
    }

    private Text text(int size, Vector2f position) {
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
        status.setText(mainContext.getInputManager().isBlocked() ? "This window waits for the dialog" : "This window is free");

        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (keyboard.keyPressed(GLFW_KEY_D) && !isOpen(dialog)) {
            dialog = openWindow(new WindowConfig("Dialog").setSize(640, 360).setVirtualSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
                    .setModal(true), new MessageView("Modal dialog", new Vector2f(0.35f, 0.12f)));
            System.out.println("--MultiWindowScene: dialog opened");
        }

        if (keyboard.keyPressed(GLFW_KEY_T) && !isOpen(tools)) {
            tools = openWindow(new WindowConfig("Tools").setSize(480, 270).setVirtualSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT),
                    new SharedSquareView(square));
            System.out.println("--MultiWindowScene: tool window opened");
        }

        if (keyboard.keyPressed(GLFW_KEY_C)) {
            engine().openContext("second-" + (++contexts),
                    new WindowConfig("Second context").setSize(640, 360).setVirtualSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT),
                    Scene2D::new);
            System.out.println("--MultiWindowScene: context opened");
        }
    }

    // A closed window is not in the list of the context any more
    private boolean isOpen(WindowContext window) {
        return window != null && mainContext.getContext().getWindows().contains(window);
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        help.display();
        status.display();
        square.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        help.unload();
        status.unload();
        square.unload();
    }

    // ---- The windows ----

    // A message and a closing key
    private static class MessageView extends WindowView {
        private final String message;
        private final Vector2f color;
        private Text text;

        MessageView(String message, Vector2f color) {
            this.message = message;
            this.color = color;
        }

        @Override
        protected void launch(String[] args) {
            setClearColor(color.x, color.y, 0.3f);

            text = new Text();
            text.setFont("bahnschrift")
                    .setFontSize(48)
                    .setReference(EDirection.None)
                    .setAlignment(ETextAlignment.Center)
                    .setPosition(new Vector2f(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f))
                    .setText(message + "\nESC closes");
        }

        @Override
        public void update() {
            super.update();

            if (window().getKeyboardManager().keyPressed(GLFW_KEY_ESCAPE)) {
                System.out.println("--MultiWindowScene: " + message + " closed by its key");
                close();
            }
        }

        @Override
        public void display() {
            setVirtualScene();
            clear();
            text.display();
            setAndDisplayRealScene();
        }

        @Override
        public void unload() {
            super.unload();
            text.unload();
        }
    }

    // The renderer of the main window, drawn here too
    private static class SharedSquareView extends WindowView {
        private final RectangleRenderer shared;

        SharedSquareView(RectangleRenderer shared) {
            this.shared = shared;
        }

        @Override
        protected void launch(String[] args) {
            setClearColor(0.2f, 0.3f, 0.15f);
        }

        @Override
        public void update() {
            super.update();

            if (window().getKeyboardManager().keyPressed(GLFW_KEY_ESCAPE))
                close();
        }

        @Override
        public void display() {
            setVirtualScene();
            clear();
            shared.display();
            setAndDisplayRealScene();
        }
    }
}
