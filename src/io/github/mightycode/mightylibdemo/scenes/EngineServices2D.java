package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.services.RunningService;
import io.github.mightycode.mightylib.sounds.SilentSoundService;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.TickCounterService;
import org.joml.Vector2f;

/**
 * The services of the engine in their order of creation, with the plugin of each one, the sound in use (OpenAL, or
 * silent with -Dmightylib.sound=silent or without device) and the ticks counted by the demo service tick_counter,
 * whose overlay (bottom right) stays under the console. ESCAPE goes back to the menu.
 * Check: python tools/scenario.py "scene EngineServices2D" wait-loaded "services", then with -D mightylib.sound=silent
 */
public class EngineServices2D extends Scene {
    private Text services;
    private Text counter;
    private TickCounterService tickCounter;
    private long shownTicks;

    @Override
    public void launch(String[] args) {
        setClearColor(0.12f, 0.16f, 0.2f);
        Vector2f windowSize = new Vector2f(context().getWindow().getInfo().getVirtualSizeRef());

        tickCounter = engine().service(TickCounterService.KEY);
        tickCounter.setOverlayShown(true);

        services = text(new Vector2f(40, 40), 26).setText(describe());
        counter = text(new Vector2f(40, windowSize.y - 120), 30);
        // Written at once: the same texts are shown at every visit (leakcheck compares their references)
        showTicks(tickCounter.getTicks());
    }

    private Text text(Vector2f position, float size) {
        Text text = new Text();
        text.setFont("bahnschrift")
                .setFontSize(size)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setPosition(position)
                .setColor(new Color4f(1f))
                .setText(" ");
        return text;
    }

    private String describe() {
        StringBuilder text = new StringBuilder("Services of the engine (command services for the details):");
        int order = 0;
        for (RunningService running : engine().services()) {
            text.append("\n").append(++order).append(". ").append(running.key()).append(" : ")
                    .append(running.service().getClass().getSimpleName())
                    .append(" (plugin ").append(running.declaration().plugin()).append(")")
                    .append(running.isFallback() ? " - fallback" : "");
        }

        boolean silent = engine().sound() instanceof SilentSoundService;
        text.append("\n\nSound: ").append(silent ? "silent, nothing is heard" : "OpenAL")
                .append("\nConsole: ").append(engine().console() != null ? "service present (key left of 1)" : "none")
                .append("\ntick_counter created after the console: ").append(tickCounter.isAfterConsole());
        return text.toString();
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        // Every 10 ticks: the text is built again at each change
        long ticks = tickCounter.getTicks();
        if (ticks / 10 != shownTicks / 10)
            showTicks(ticks);
    }

    private void showTicks(long ticks) {
        shownTicks = ticks;
        counter.setText("Ticks counted by tick_counter: " + ticks + "\nESCAPE: menu");
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        services.display();
        counter.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        tickCounter.setOverlayShown(false);
        services.unload();
        counter.unload();
    }
}
