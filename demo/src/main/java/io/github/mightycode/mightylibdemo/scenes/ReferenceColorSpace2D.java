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
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference scene of the image tests (ReferenceImagesGLTest): the same picture in linear (left half) and gamma
 * (right half): a ramp of greys, alpha blends, a filtered texture and anti-aliased text. The tests also capture it
 * under a picture correction (gamma, brightness, contrast). Still. Do not change it without generating its
 * references again (tools/references.py --update).
 */
public class ReferenceColorSpace2D extends Scene {
    private static final int RAMP_STEPS = 16;

    private final List<RectangleRenderer> shapes = new ArrayList<>();
    private RectangleRenderer texture;
    private Text sample;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"litcrates3d"};
    }

    @Override
    public void launch(String[] args) {
        DemoCamera.free(this);
        setClearColor(0.5f);

        Vector2i size = context().getWindow().getInfo().getVirtualSizeRef();
        float width = size.x, height = size.y;

        float stepWidth = width / RAMP_STEPS;
        for (int i = 0; i < RAMP_STEPS; ++i) {
            float grey = i / (float) (RAMP_STEPS - 1);
            shapes.add(rectangle(new Color4f(grey, grey, grey, 1f), i * stepWidth, 0, stepWidth, height * 0.2f));
        }

        shapes.add(rectangle(ColorList.White(), 0, height * 0.25f, width, height * 0.1f));
        shapes.add(rectangle(ColorList.Black(), 0, height * 0.35f, width, height * 0.1f));
        for (int i = 0; i < 4; ++i) {
            float alpha = 0.2f + 0.2f * i;
            shapes.add(rectangle(new Color4f(1f, 0f, 0f, alpha), width * 0.30f + i * width * 0.1f, height * 0.25f,
                    width * 0.09f, height * 0.2f));
        }

        texture = new RectangleRenderer("texture2D");
        texture.setMainTextureChannel("container2");
        texture.setSizePix(height * 0.3f, height * 0.3f);
        texture.setPosition(new Vector2f(width * 0.5f - height * 0.15f, height * 0.5f));

        sample = new Text();
        sample.setFontSize(32).setReference(EDirection.None).setAlignment(ETextAlignment.Center)
                .setColor(ColorList.Black()).setPosition(new Vector2f(width * 0.5f, height * 0.9f))
                .setText("Anti-aliased text on grey");
    }

    private static RectangleRenderer rectangle(Color4f color, float x, float y, float width, float height) {
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

        // Each half forces its mode, the mode of the view comes back after
        ERenderColorSpace viewMode = colorSpace();
        setScissor(0, 0, half, size.y);
        drawAll(ERenderColorSpace.LINEAR);
        setScissor(half, 0, size.x - half, size.y);
        drawAll(ERenderColorSpace.GAMMA);
        clearScissor();
        setColorSpace(viewMode);

        setAndDisplayRealScene();
    }

    private void drawAll(ERenderColorSpace mode) {
        setColorSpace(mode);

        for (RectangleRenderer shape : shapes)
            shape.display();
        texture.display();
        sample.display();
    }

    @Override
    public void unload() {
        super.unload();
        for (RectangleRenderer shape : shapes)
            shape.unload();
        shapes.clear();
        texture.unload();
        sample.unload();
    }
}
