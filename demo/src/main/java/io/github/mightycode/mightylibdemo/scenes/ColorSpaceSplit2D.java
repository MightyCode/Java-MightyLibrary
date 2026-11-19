package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

/**
 * The same 2D picture drawn twice, linear on the left half and gamma on the right half (the view changes its mode
 * between the two draws, the scissor keeps one half each): a control ramp, alpha blends, a filtered texture and text.
 * Look at the seam in the middle: the ramp, the blends and the edges of the text differ between the two modes.
 */
public class ColorSpaceSplit2D extends Scene {
    private static final int RAMP_STEPS = 16;

    private final List<RectangleRenderer> shapes = new ArrayList<>();
    private RectangleRenderer texture;
    private Text label;
    private Text sample;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "litcrates3d" };
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.5f);

        Vector2i size = context().getWindow().getInfo().getVirtualSizeRef();
        float width = size.x, height = size.y;

        // Control: a ramp of colors given in sRGB looks the same in both modes (the colors are converted); what differs
        // is what is mixed (blends, filtering, antialiasing)
        float stepWidth = width / RAMP_STEPS;
        for (int i = 0; i < RAMP_STEPS; ++i) {
            float grey = i / (float) (RAMP_STEPS - 1);
            shapes.add(rectangle(new Color4f(grey, grey, grey, 1f), i * stepWidth, 0, stepWidth, height * 0.2f));
        }

        // Alpha blends over a bright and a dark band, across the middle seam
        shapes.add(rectangle(ColorList.White(), 0, height * 0.25f, width, height * 0.1f));
        shapes.add(rectangle(ColorList.Black(), 0, height * 0.35f, width, height * 0.1f));
        for (int i = 0; i < 4; ++i) {
            float alpha = 0.2f + 0.2f * i;
            shapes.add(rectangle(new Color4f(1f, 0f, 0f, alpha), width * 0.30f + i * width * 0.1f, height * 0.25f,
                    width * 0.09f, height * 0.2f));
        }

        // A filtered texture, scaled down: the filtering is done on linear values for an sRGB texture
        texture = new RectangleRenderer("texture2D");
        texture.setMainTextureChannel("container2");
        texture.setSizePix(height * 0.3f, height * 0.3f);
        texture.setPosition(new Vector2f(width * 0.5f - height * 0.15f, height * 0.5f));

        label = new Text();
        label.setFontSize(36).setReference(EDirection.None).setAlignment(ETextAlignment.Center)
                .setColor(ColorList.White()).setPosition(new Vector2f(width * 0.5f, height * 0.95f))
                .setText("left: linear | right: gamma");

        sample = new Text();
        sample.setFontSize(48).setReference(EDirection.None).setAlignment(ETextAlignment.Center)
                .setColor(ColorList.Black()).setPosition(new Vector2f(width * 0.5f, height * 0.88f))
                .setText("Anti-aliased text on grey");
    }

    private RectangleRenderer rectangle(Color4f color, float x, float y, float width, float height) {
        RectangleRenderer rectangle = new RectangleRenderer("colorShape2D");
        rectangle.setColorMode(color);
        rectangle.setSizePix(width, height);
        rectangle.setPosition(new Vector2f(x, y));
        return rectangle;
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        Vector2i size = context().getWindow().getInfo().getVirtualSizeRef();
        int half = size.x / 2;

        setScissor(0, 0, half, size.y);
        drawAll(ERenderColorSpace.LINEAR);
        setScissor(half, 0, size.x - half, size.y);
        drawAll(ERenderColorSpace.GAMMA);
        clearScissor();

        setAndDisplayRealScene();
    }

    // The view takes the mode of the half
    private void drawAll(ERenderColorSpace mode) {
        setColorSpace(mode);

        for (RectangleRenderer shape : shapes)
            shape.display();
        texture.display();
        sample.display();
        label.display();
    }

    @Override
    public void unload() {
        super.unload();
        for (RectangleRenderer shape : shapes)
            shape.unload();
        shapes.clear();
        texture.unload();
        label.unload();
        sample.unload();
    }
}
