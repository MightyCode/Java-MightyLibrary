package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.project.main.ActionId;
import MightyLibrary.project.scenes.loadingScenes.LoadingSceneImplementation;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class TestGUI extends Scene {

    private Text text;

    public void init(String[] args) {
        super.init(args, new LoadingSceneImplementation());
    }

    public void launch(String[] args) {
        super.launch(args);
        setClearColor(52, 189, 235, 1f);


        text = new Text();

        text.setFont("bahnschrift")
                .setFontSize(20)
                .setReference(EDirection.LeftUp)
                .setPadding(5, 5, 120, 20)
                .setAlignment(ETextAlignment.Left)
                .setPosition(new Vector2f(0, 0))
                .setColor(new Color4f(0.5f, 0.4f, 0.3f, 1))
                .setText("Test positioning of text padding(5, 5, 120, 20)\nHey i'm testing things, \ndfdfgdfgdfg");

        addDisplayable(text);
    }

    public void update() {
        super.update();
        Vector2i size = mainContext.getWindow().getInfo().getSizeRef();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        if (mainContext.getInputManager().inputPressed(ActionId.ENTER)) {
            switch (text.reference()) {
                case LeftUp:
                    text.setReference(EDirection.RightUp);
                    text.setAlignment(ETextAlignment.Right);
                    text.setPosition(new Vector2f(size.x, 0));
                    break;
                case RightUp:
                    text.setReference(EDirection.RightDown);
                    text.setPosition(new Vector2f(size.x, size.y));
                    break;
                case RightDown:
                    text.setReference(EDirection.LeftDown);
                    text.setAlignment(ETextAlignment.Left);
                    text.setPosition(new Vector2f(0, size.y));
                    break;
                case LeftDown:
                    text.setReference(EDirection.LeftUp);
                    text.setPosition(new Vector2f(0, 0));
                    break;
            }
        }
    }

    public void display() {
        super.setVirtualScene();
        clear();


        super.display();
        super.setAndDisplayRealScene();
    }


    @Override
    public void unload() {
        super.unload();

    }
}
