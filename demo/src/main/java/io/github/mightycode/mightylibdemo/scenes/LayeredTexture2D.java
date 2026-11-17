package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.resources.LayeredTextureData;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * A data read from several files with its own fallback (LayeredTextureData, demo loader): from left to right
 * the character "hero" made of three layers (resources/layered/hero, body, clothes, hat, drawn over each other),
 * an unknown layered picture (blue stripes: the fallback of LayeredTextureData, not the checkerboard), an unknown
 * texture (checkerboard). Check: "?gl summary" lists the fallback of LayeredTextureData, "?resources LayeredTextureData"
 * the state of each layer; change a layer (python tools/generate_layers.py after editing it) then
 * "reload LayeredTextureData hero".
 */
public class LayeredTexture2D extends Scene {
    private static final String HERO = "hero";
    private static final String UNKNOWN = "missingLayers";
    private static final int SIZE = 256;

    private RectangleRenderer hero, unknownLayers, unknownTexture;
    private final List<Text> labels = new ArrayList<>();

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "layered" };
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.2f, 0.2f, 0.25f);

        hero = square(layeredTexture(HERO), 0, "Layers of hero");
        // Unknown name: the fallback data of the type, which shares the fallback element
        unknownLayers = square(layeredTexture(UNKNOWN), 1, "Unknown layers: stripes");
        unknownTexture = new RectangleRenderer("texture2D");
        unknownTexture.setMainTextureChannel("missingTexture");
        place(unknownTexture, 2, "Unknown texture: checkerboard");
    }

    // The texture of the data (its fallback when the name is unknown), referenced by the renderer that gets it
    private IRenderTextureBindable layeredTexture(String name) {
        GLElement element = glResources().acquire(resources().getResourceOrFallback(LayeredTextureData.class, name), true);
        return (IRenderTextureBindable) element;
    }

    private RectangleRenderer square(IRenderTextureBindable texture, int index, String label) {
        RectangleRenderer renderer = new RectangleRenderer("texture2D");
        renderer.setMainTextureChannel(texture);
        place(renderer, index, label);
        return renderer;
    }

    private void place(RectangleRenderer renderer, int index, String label) {
        renderer.setSizePix(SIZE, SIZE);
        renderer.setPosition(new Vector2f(index * (SIZE + 50) + 40, 80));

        Text text = new Text();
        text.setFontSize(24)
                .setReference(EDirection.LeftUp)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(index * (SIZE + 50) + 40, 80 + SIZE + 20))
                .setText(label);
        labels.add(text);
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

        hero.display();
        unknownLayers.display();
        unknownTexture.display();
        for (Text label : labels)
            label.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (hero != null) {
            hero.unload();
            unknownLayers.unload();
            unknownTexture.unload();
        }
        for (Text label : labels)
            label.unload();
        labels.clear();
    }
}
