package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The sources of the resources: the demo declares the classpath source "assets/demo/" (DemoPlugin, files in
 * demo/src/main/resources/assets/demo), above the folder of the game, as a mod would be. From left to right: a texture
 * of the game folder replaced by the demo (same path textures/sources/replaced.png: green stripes, the red ones of the
 * game folder are hidden), a texture only in the demo (blue stripes), added to textures.json by the textures.json of
 * the demo (merged by entry). The batch of the scene is in the demo source too. Below: the sources that have each file,
 * the highest serving it. Check: "resources sources textures/sources/replaced.png".
 */
public class ResourceSourcesScene2D extends Scene {
    private static final int SIZE = 256;
    private static final String[] FILES = {
            "textures/sources/replaced.png", "textures/sources/onlyDemo.png", "textures/textures.json",
            "batchs/resourceSources.batch"
    };

    private final List<RectangleRenderer> squares = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "resourceSources" };
    }

    @Override
    public void launch(String[] args) {
        DemoCamera.free(this);
        setClearColor(0.2f, 0.2f, 0.25f);

        square("sourcesReplaced", 0, "Replaced by the demo: green");
        square("sourcesOnlyDemo", 1, "Only in the demo: blue");

        ResourceSources sources = resources().sources();
        float y = 80 + SIZE + 80;
        for (String file : FILES) {
            text(40, y, 20, file + " : " + layers(sources, file));
            y += 32;
        }
    }

    // The sources that have the file, from the lowest to the highest (the last one serves it)
    private static String layers(ResourceSources sources, String file) {
        List<String> names = new ArrayList<>();
        for (IResourceSource source : sources.layers(file))
            names.add(source.name());

        Optional<IResourceSource> served = sources.resolve(file);
        return names.isEmpty() ? "in no source"
                : String.join(" < ", names) + (served.isPresent() ? ", served by " + served.get().name() : "");
    }

    private void square(String texture, int index, String label) {
        RectangleRenderer renderer = new RectangleRenderer("texture2D");
        renderer.setMainTextureChannel(texture);
        renderer.setSizePix(SIZE, SIZE);
        renderer.setPosition(new Vector2f(index * (SIZE + 50) + 40, 80));
        squares.add(renderer);

        text(index * (SIZE + 50) + 40, 80 + SIZE + 20, 24, label);
    }

    private void text(float x, float y, float size, String content) {
        Text text = new Text();
        text.setFontSize(size)
                .setReference(EDirection.LeftUp)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(x, y))
                .setText(content);
        texts.add(text);
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

        for (RectangleRenderer square : squares)
            square.display();
        for (Text text : texts)
            text.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        for (RectangleRenderer square : squares)
            square.unload();
        squares.clear();
        for (Text text : texts)
            text.unload();
        texts.clear();
    }
}
