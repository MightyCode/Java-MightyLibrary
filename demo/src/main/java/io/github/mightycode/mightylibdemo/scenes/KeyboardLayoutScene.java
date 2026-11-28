package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

/**
 * The keys held, each with its physical position (GLFW_KEY_..., what the actions read) and its label in the layout
 * of the system (KeyboardManager.keyLabel: "Z" on the key GLFW_KEY_W of an AZERTY keyboard), and the text typed.
 * ESCAPE returns to the menu. An input test screen: no driven camera.
 */
public class KeyboardLayoutScene extends Scene {
    // Typed text kept on the screen
    private static final int TYPED_LENGTH = 40;

    // Names of the GLFW_KEY_... constants by key, read once
    private static final Map<Integer, String> KEY_CONSTANTS = KeyConstants();

    private Text info;
    private String shownInfo;
    private final StringBuilder typed = new StringBuilder();

    private static Map<Integer, String> KeyConstants() {
        Map<Integer, String> names = new TreeMap<>();
        for (Field field : GLFW.class.getFields()) {
            String name = field.getName();
            if (!name.startsWith("GLFW_KEY_") || field.getType() != int.class || !Modifier.isStatic(field.getModifiers())
                    || "GLFW_KEY_LAST".equals(name) || "GLFW_KEY_UNKNOWN".equals(name))
                continue;
            try {
                names.putIfAbsent(field.getInt(null), name);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Public constant of GLFW not readable: " + name, e);
            }
        }
        return names;
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.15f, 0.15f, 0.2f);

        info = new Text();
        info.setFontSize(22)
                .setReference(EDirection.LeftUp)
                .setColor(ColorList.White())
                .setPosition(new Vector2f(20, 20));
        shownInfo = null;
        typed.setLength(0);
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        KeyboardManager keyboard = context().getInputManager().getKeyboardManager();
        typed.append(keyboard.getTypedText());
        if (typed.length() > TYPED_LENGTH)
            typed.delete(0, typed.length() - TYPED_LENGTH);

        String text = describe(keyboard);
        if (!text.equals(shownInfo)) {
            info.setText(text);
            shownInfo = text;
        }
    }

    private String describe(KeyboardManager keyboard) {
        StringBuilder builder = new StringBuilder("ESC: menu\nHold keys: position -> label in the layout of the system\n\n");
        boolean any = false;
        for (Map.Entry<Integer, String> key : KEY_CONSTANTS.entrySet()) {
            if (!keyboard.getKeyState(key.getKey()))
                continue;
            any = true;
            builder.append(key.getValue()).append(" -> ").append(keyboard.keyLabel(key.getKey())).append("\n");
        }
        if (!any)
            builder.append("No key held\n");

        builder.append("\nTyped: ").append(typed);
        return builder.toString();
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        info.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (info != null)
            info.unload();
    }
}
