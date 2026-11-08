package io.github.mightycode.mightylibdemo.scenes.loadingScenes;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.scenes.MenuScene;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class LoadingSceneImplementation extends TemplateSceneLoading {
    private Text text;
    // Data already loaded when this screen appears: only what this loading adds is counted
    private int doneAtStart = -1;

    public LoadingSceneImplementation() {
        super();
    }

    @Override
    public void launch(String[] args) {
        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();

        text = new Text();
        text.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setColor(ColorList.White())
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.1f))
                .setFontSize(40)
                .setText("LOADING");

        addDisplayable(text);
    }

    @Override
    public void update() {
        super.update();

        // x / n of the data this loading brings (n grows when a data read adds dependencies)
        int[] progress = resources.wantedProgress();
        if (doneAtStart < 0) {
            doneAtStart = progress[0];
        }

        int total = progress[1] - doneAtStart;
        int done = progress[0] - doneAtStart;
        if (total > 0 && text != null)
            text.setText("LOADING " + Math.max(0, done) + " / " + total);

        // The scene being loaded is left (unloaded or released) when the menu is set
        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();
        super.display();

        setClearColor(0.2f, 0.2f, 0.2f, 1.0f);

        super.setAndDisplayRealScene();
    }

    public void unload() {
        super.unload();
    }
}
