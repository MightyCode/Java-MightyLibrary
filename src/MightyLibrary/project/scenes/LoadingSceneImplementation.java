package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.scenes.TemplateSceneLoading;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class LoadingSceneImplementation extends TemplateSceneLoading {
    Text text;

    public LoadingSceneImplementation() {
        super();
    }

    @Override
    public void init(String[] args) {
        super.init(args);

        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();

        text = new Text();
        text.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.25f, windowSize.y * 0.4f))
                .setFontSize(40)
                .setText("Test2DScene");

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

        super.setAndDisplayRealScene();
    }

    public void unload() {
        super.unload();
    }
}
