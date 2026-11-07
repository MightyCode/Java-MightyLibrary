package io.github.mightycode.mightylibdemo.scenes.loadingScenes;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class LoadingSceneImplementation extends TemplateSceneLoading {
    public LoadingSceneImplementation() {
        super();
    }

    @Override
    public void launch(String[] args) {
        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();

        Text text = new Text();
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
