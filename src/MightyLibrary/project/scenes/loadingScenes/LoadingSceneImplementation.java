package MightyLibrary.project.scenes.loadingScenes;

import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.scenes.TemplateSceneLoading;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class LoadingSceneImplementation extends TemplateSceneLoading {
    public LoadingSceneImplementation() {
        super();
    }

    public void init(String[] args) {
        super.init(args, new LoadingSceneImplementation());
    }

    @Override
    public void launch(String[] args) {
        super.launch(args);

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
