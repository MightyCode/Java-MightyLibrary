package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Padding of a Text : each blue rectangle is the box of the text (leftUpPosition and size(), padding included), the
 * white text inside is kept away from its edges by the padding. Left : reference LeftUp, no padding then 20 on every
 * side ; middle : reference None, centered, left 60 and up 10 ; right : reference RightDown, right 40 and down 30.
 * Bottom : a long text wrapped at the spaces (setMaxWidth), the longest word is cut.
 */
public class TextPaddingScene extends Scene {
    private static final String CONTENT = "Padding\nof a text";

    private static final String LONG_CONTENT = "A long text wrapped at the spaces, with a very long word: "
            + "supercalifragilisticexpialidocious.\nAn explicit line feed is kept.";

    private Text[] texts;
    private RectangleRenderer[] boxes;

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.12f);

        texts = new Text[]{
                text(EDirection.LeftUp, ETextAlignment.Left, new Vector4f(0, 0, 0, 0), new Vector2f(40, 40)),
                text(EDirection.LeftUp, ETextAlignment.Left, new Vector4f(20, 20, 20, 20), new Vector2f(40, 240)),
                text(EDirection.None, ETextAlignment.Center, new Vector4f(60, 10, 0, 0), new Vector2f(640, 200)),
                text(EDirection.RightDown, ETextAlignment.Right, new Vector4f(0, 0, 40, 30), new Vector2f(1240, 400)),
                text(EDirection.LeftUp, ETextAlignment.Left, new Vector4f(0, 0, 0, 0), new Vector2f(500, 450))
                        .setFontSize(30).setMaxWidth(400).setText(LONG_CONTENT)
        };

        boxes = new RectangleRenderer[texts.length];
        for (int i = 0; i < texts.length; ++i) {
            boxes[i] = new RectangleRenderer("colorShape2D");
            boxes[i].setColorMode(new Color4f(0.15f, 0.3f, 0.7f, 1f));
            boxes[i].setPosition(new Vector2f(texts[i].leftUpPosition()));
            boxes[i].setSizePix(texts[i].size());
        }
    }

    private static Text text(EDirection reference, ETextAlignment alignment, Vector4f padding, Vector2f position) {
        Text text = new Text();
        text.setFont("bahnschrift")
                .setFontSize(40)
                .setReference(reference)
                .setAlignment(alignment)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPadding(padding.x, padding.y, padding.z, padding.w)
                .setPosition(position)
                .setText(CONTENT);
        return text;
    }

    @Override
    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        // The boxes follow the texts : the font may only be usable after the first frames
        for (int i = 0; i < texts.length; ++i) {
            boxes[i].setPosition(new Vector2f(texts[i].leftUpPosition()));
            boxes[i].setSizePix(new Vector2f(texts[i].size()));
            boxes[i].display();
            texts[i].display();
        }

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (texts == null)
            return;

        for (int i = 0; i < texts.length; ++i) {
            boxes[i].unload();
            texts[i].unload();
        }
    }
}
