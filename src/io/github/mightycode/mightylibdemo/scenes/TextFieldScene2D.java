package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.GUI.TextField;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * TextField and the focus of the window. Click the field to type (Ctrl+A/C/X/V, arrows, Home/End, mouse selection);
 * Escape, Enter, Up, Down or a click elsewhere give the focus back. While the field has it, the scene sees no key:
 * the Enter counter below only counts the presses the scene saw (Enter in the field submits it, it is not one);
 * Escape with the scene holding the focus goes back to the menu.
 */
public class TextFieldScene2D extends Scene {
    private TextField field;
    private Text info;

    private int changes;
    private int submissions;
    private int enterSeenByScene;
    private String lastSubmitted = "";
    private String leftWith = "";

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.12f);
        mainContext.getMouseManager().setCursor(true);

        field = new TextField(mainContext, 340, 200, 600, 56, "bahnschrift", 30);
        field.setHint("Type something...");

        info = new Text();
        info.setFont("bahnschrift")
                .setFontSize(28)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(340, 300));
    }

    @Override
    public void update() {
        super.update();

        field.update();

        if (field.isChanged())
            ++changes;
        if (field.isSubmitted()) {
            ++submissions;
            lastSubmitted = field.getText();
        }
        if (field.getLeftWith() != -1)
            leftWith = KeyName(field.getLeftWith());

        InputManager input = mainContext.getInputManager();
        if (input.inputPressed(ActionId.ENTER))
            ++enterSeenByScene;

        if (input.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
    }

    private static String KeyName(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE:
                return "Escape";
            case GLFW_KEY_UP:
                return "Up";
            case GLFW_KEY_DOWN:
                return "Down";
            default:
                return "Enter";
        }
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        field.display();

        info.setText("Focus : " + (field.hasFocus() ? "the field" : "the scene")
                + "\nChanges : " + changes
                + "\nSubmitted : " + submissions + " (last : \"" + lastSubmitted + "\")"
                + "\nFocus given back by : " + (leftWith.isEmpty() ? "-" : leftWith)
                + "\nEnter seen by the scene : " + enterSeenByScene);
        info.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (field != null)
            field.unload();
        if (info != null)
            info.unload();
    }
}
