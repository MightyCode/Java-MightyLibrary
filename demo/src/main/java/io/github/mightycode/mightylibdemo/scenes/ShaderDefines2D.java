package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.shader.IShaderService;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;

/**
 * The defines of the shaders. Two entries of resources/shaders/shaders.json on the same files: definesDemo2D without
 * define (left), definesDemoTinted2D with "defines": {"TINT": ...} (right, tinted blue). Key D sets or removes the
 * global define DEMO_INVERT (shaders().setDefine): every shader is read again, the colors of both squares are inverted.
 * The define is removed when the scene is left. ESCAPE goes back to the menu.
 * Check: python tools/scenario.py "scene ShaderDefines2D" wait-loaded "input key D" sleep:1 "?shaders"
 */
public class ShaderDefines2D extends Scene {
    private static final String INVERT = "DEMO_INVERT";

    private RectangleRenderer plain;
    private RectangleRenderer tinted;
    private Text help;

    @Override
    public void launch(String[] args) {
        setClearColor(0.12f, 0.12f, 0.16f);
        Vector2f size = new Vector2f(context().getWindow().getInfo().getVirtualSizeRef());

        plain = square("definesDemo2D", new Vector2f(size.x / 2 - 200, size.y / 2 + 40));
        tinted = square("definesDemoTinted2D", new Vector2f(size.x / 2 + 200, size.y / 2 + 40));

        help = new Text();
        help.setFontSize(26)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setPosition(new Vector2f(40, 40))
                .setColor(new Color4f(1f))
                .setText(helpText());
    }

    private static RectangleRenderer square(String shader, Vector2f center) {
        RectangleRenderer square = new RectangleRenderer(shader);
        square.setColorMode(new Color4f(1f, 0.85f, 0.5f, 1f));
        square.setReferenceDirection(EDirection.None);
        square.setSizePix(300, 300);
        square.setPosition(center);
        return square;
    }

    private String helpText() {
        boolean inverted = engine().shaders().defines().containsKey(INVERT);
        return "Same GLSL files, two entries of shaders.json.\n"
                + "Left: definesDemo2D, no define. Right: definesDemoTinted2D, define TINT of its entry.\n"
                + "D: global define " + INVERT + " (" + (inverted ? "set" : "not set") + "), every shader is read again.\n"
                + "ESCAPE: menu";
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});

        if (context().getKeyboardManager().keyPressed(GLFW_KEY_D)) {
            IShaderService shaders = engine().shaders();
            if (shaders.defines().containsKey(INVERT))
                shaders.removeDefine(INVERT);
            else
                shaders.setDefine(INVERT, "");

            help.setText(helpText());
        }
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        plain.display();
        tinted.display();
        help.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        // A global define stays until removed: the other scenes find the shaders as they were
        engine().shaders().removeDefine(INVERT);
        plain.unload();
        tinted.unload();
        help.unload();
    }
}
