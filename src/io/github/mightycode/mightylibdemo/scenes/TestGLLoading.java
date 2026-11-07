package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.scenes.Scene;
import org.joml.Vector2f;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Loads several big generated textures, sent by strips with the time left in each frame.
 * Check with: stats reset, scene TestGLLoading, wait-loaded, stats (maxFrameIntervalMsSinceReset).
 */
public class TestGLLoading extends Scene {
    private static final int TEXTURE_COUNT = 6;
    private static final int TEXTURE_SIZE = 2048;

    private final RectangleRenderer[] renderers = new RectangleRenderer[TEXTURE_COUNT];

    private static String textureName(int index) {
        return "testGLLoading" + index;
    }

    @Override
    public void init(String[] args) {
        for (int i = 0; i < TEXTURE_COUNT; ++i) {
            // Filled by Java2D (fast), converted to pixels by the resource workers
            BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setPaint(new GradientPaint(0, 0, Color.getHSBColor(i / (float) TEXTURE_COUNT, 0.8f, 1f),
                    TEXTURE_SIZE, TEXTURE_SIZE, Color.DARK_GRAY));
            graphics.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.dispose();

            TextureData data = new TextureData(textureName(i), "generated");
            data.setBufferedImage(image);
            data.setPreloaded();
            resources.addPreLoadedResource(data);
        }
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.1f);

        Vector2f windowSize = new Vector2f(mainContext.getWindow().getInfo().getVirtualSizeRef());
        float cellSize = windowSize.x / 3f;

        for (int i = 0; i < TEXTURE_COUNT; ++i) {
            renderers[i] = new RectangleRenderer("texture2D");
            renderers[i].setMainTextureChannel(textureName(i));
            renderers[i].setSizePix(cellSize - 10, cellSize - 10);
            renderers[i].setPosition(new Vector2f((i % 3) * cellSize + 5, (i / 3) * cellSize + 5));
        }
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        for (RectangleRenderer renderer : renderers)
            renderer.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        for (RectangleRenderer renderer : renderers) {
            if (renderer != null)
                renderer.unload();
        }

        for (int i = 0; i < TEXTURE_COUNT; ++i)
            resources.deleteResource(TextureData.class, textureName(i));
    }
}
