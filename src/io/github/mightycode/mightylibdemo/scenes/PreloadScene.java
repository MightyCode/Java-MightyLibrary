package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.json.JSONObject;
import org.joml.Vector2f;
import org.joml.Vector2i;

/**
 * Preload of the next scene: Scene2D is loaded in advance while this scene runs, ENTER sets it (its resources
 * are already loaded: the loading screen is short), ESCAPE goes back to the menu.
 * Check with: scene PreloadScene, resources, resources batch anim, input ENTER, resources.
 */
public class PreloadScene extends Scene {
    private static final int REFRESH_TICKS = 20;

    private Scene2D next;
    private Text text;
    private int ticks;

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.1f);

        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();
        text = new Text();
        text.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setColor(ColorList.White())
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.5f))
                .setFontSize(30)
                .setText("Preloading");
        addDisplayable(text);

        // The scene is only built: its batches are loaded, not its objects
        next = new Scene2D();
        sceneManagerInterface.preloadScene(next);
    }

    @Override
    public void update() {
        super.update();

        if (++ticks % REFRESH_TICKS == 0)
            text.setText(describePreload() + " | ENTER: Scene2D, ESCAPE: menu");

        if (mainContext.getInputManager().inputPressed(ActionId.ENTER))
            sceneManagerInterface.setNewScene(next, new String[]{});

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
    }

    // Loaded data / data of the batches of the next scene
    private String describePreload() {
        int loaded = 0, total = 0;
        long cpu = 0, gpu = 0;
        for (String batch : next.getInvolvedBatch()) {
            JSONObject preview = resources.describeBatch(batch);
            if (preview.has("error"))
                continue;

            loaded += preview.getInt("loaded");
            total += preview.getInt("withDependencies");
            cpu += preview.getLong("cpuBytes");
            gpu += preview.getLong("gpuBytes");
        }

        return "Scene2D preloaded: " + loaded + "/" + total + " data, " + (cpu >> 10) + " KiB CPU, "
                + (gpu >> 10) + " KiB GPU";
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();
        super.display();
        setAndDisplayRealScene();
    }
}
