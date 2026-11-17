package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.services.EngineServices;
import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.services.ServiceKey;
import io.github.mightycode.mightylib.sounds.ISoundService;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

/**
 * A service of the demo (DemoPlugin): requires the sound, created after the console when there is one; counts the ticks
 * of the engine and, when asked (EngineServices2D), shows them in a small overlay of layer 0, under the console.
 */
public final class TickCounterService implements IEngineService {
    public static final ServiceKey<TickCounterService> KEY =
            ServiceKey.of(Identifier.of(DemoPlugin.NAMESPACE, "tick_counter"), TickCounterService.class);

    private final Engine engine;
    // Read at the creation: the sound this service requires
    private final ISoundService sound;
    private final boolean afterConsole;
    private long ticks;

    private boolean overlayShown;
    // Created at the first display, over the main window
    private IView overlayView;
    private Text overlay;

    TickCounterService(Engine engine) {
        this.engine = engine;
        sound = engine.service(EngineServices.SOUND);
        afterConsole = engine.hasService(EngineServices.CONSOLE);
    }

    public long getTicks() {
        return ticks;
    }

    // The sound service it was given (OpenAL or silent)
    public ISoundService getSound() {
        return sound;
    }

    // The console existed when this service was created (after(CONSOLE))
    public boolean isAfterConsole() {
        return afterConsole;
    }

    // Hidden: its text is released (it holds the font), created again when shown
    public void setOverlayShown(boolean shown) {
        overlayShown = shown;
        if (!shown)
            releaseOverlay();
    }

    @Override
    public void update() {
        ++ticks;
    }

    @Override
    public void displayOverlay() {
        if (!overlayShown)
            return;

        if (overlayView == null)
            overlayView = new OverlayView(engine, engine.contexts().requireMainWindow());

        try (Views.Scope ignored = Views.enter(overlayView)) {
            if (overlay == null) {
                Vector2f size = new Vector2f(overlayView.context().getWindow().getInfo().getVirtualSizeRef());
                overlay = new Text();
                overlay.setFontSize(24)
                        .setReference(EDirection.RightDown)
                        .setPosition(new Vector2f(size.x - 10, size.y - 10))
                        .setColor(new Color4f(1f, 0.85f, 0.3f, 1f))
                        .setText(" ");
            }

            overlay.setText("tick_counter overlay: " + ticks + " ticks");
            overlay.display();
        }
    }

    @Override
    public void unload() {
        releaseOverlay();
    }

    private void releaseOverlay() {
        if (overlay != null) {
            try (Views.Scope ignored = Views.enter(overlayView)) {
                overlay.unload();
            }
            overlay = null;
        }
    }

    // The overlay is drawn over the main window, in its virtual size
    private static final class OverlayView implements IView {
        private final Engine engine;
        private final WindowContext window;
        private final Camera2D camera;

        private OverlayView(Engine engine, WindowContext window) {
            this.engine = engine;
            this.window = window;
            camera = new Camera2D(window.getWindow().getInfo(), new Vector2f());
        }

        @Override
        public Engine engine() { return engine; }

        @Override
        public WindowContext context() { return window; }

        @Override
        public Camera2D camera2D() { return camera; }

        @Override
        public Camera3D camera3D() {
            throw new IllegalStateException("The overlay of tick_counter has no 3D camera");
        }
    }
}
