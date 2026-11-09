package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import org.joml.Vector2f;

/**
 * View of the console in a window: its renderers are created and drawn under it (Views.enter), with a camera in
 * real pixels of the window (origin at the top left), whatever the virtual size of the scene.
 */
final class ConsoleView implements IView {
    /** Projection on the real size of the window, followed when it changes (refresh). */
    static final class PixelCamera extends Camera2D {
        private int width = -1;
        private int height = -1;

        PixelCamera(WindowInfo info) {
            super(info, new Vector2f(0, 0));
        }

        @Override
        public void updateProjection() {
            projection.setOrtho(0, Math.max(1, windowInfo.getSizeRef().x), Math.max(1, windowInfo.getSizeRef().y), 0, -10, 10);
        }

        // The projection follows the size of the window
        void refresh() {
            int newWidth = windowInfo.getSizeRef().x;
            int newHeight = windowInfo.getSizeRef().y;
            if (newWidth != width || newHeight != height) {
                width = newWidth;
                height = newHeight;
                updateProjection();
            }
        }
    }

    private final Engine engine;
    private final WindowContext window;
    private final PixelCamera camera;

    ConsoleView(Engine engine, WindowContext window) {
        this.engine = engine;
        this.window = window;
        this.camera = new PixelCamera(window.getWindow().getInfo());
    }

    PixelCamera pixelCamera() { return camera; }

    @Override
    public Engine engine() { return engine; }

    @Override
    public WindowContext context() { return window; }

    @Override
    public Camera2D camera2D() { return camera; }

    @Override
    public Camera3D camera3D() {
        throw new IllegalStateException("The console has no 3D camera");
    }
}
