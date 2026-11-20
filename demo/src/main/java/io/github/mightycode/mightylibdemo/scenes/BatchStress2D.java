package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawSortKeys;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Measure of the 2D drawing: N rectangles, three textured ones (regions of one tile set, like an atlas) for one
 * colored one, of several sizes and turning. Up doubles N, Down halves it, M changes the mode of the 2D batch (off,
 * orphan, ring, instanced), S sorts the draws by state (DrawSortKeys.layerThenState: the sprites together, the
 * colored rectangles together). The counters are those of the command stats (renderCpuMs: CPU time of the display of the
 * windows, without the buffer swap).
 */
public class BatchStress2D extends Scene {
    private static final int START_COUNT = 1000;
    private static final int MAX_COUNT = 1 << 17;
    // Tiles per side of the tile set used as an atlas
    private static final int TILES = 8;

    private final List<RectangleRenderer> rectangles = new ArrayList<>();
    private final List<Float> speeds = new ArrayList<>();
    // Same seed at each launch: the same picture for the same N
    private final Random random = new Random(42);
    private Vector2i area;

    private Material spriteMaterial;
    private Material colorMaterial;

    private Text help;
    // Grey rounded rectangle under the texts, to read them over the moving rectangles
    private RoundedRectangleRenderer textBackground;
    private Text counters;
    private float counterTimer;
    private float time;

    @Override
    public void launch(String[] args) {
        setClearColor(30, 30, 40, 1f);
        area = context().getWindow().getInfo().getVirtualSizeCopy();

        spriteMaterial = new Material("texture2D").setMainTexture("tileset");
        colorMaterial = new Material("colorShape2D");
        resize(START_COUNT);

        help = new Text();
        help.setFontSize(20).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 10));
        help.setText("Up : N x2   Down : N / 2   M : mode of the batch   S : sort by state   ESC : menu");
        counters = new Text();
        counters.setFontSize(18).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 40));

        textBackground = new RoundedRectangleRenderer();
        textBackground.setCornerRadius(8f);
        textBackground.setColorMode(new Color4f(0.25f, 0.25f, 0.27f, 0.88f));
        textBackground.setPosition(new Vector2f(4, 4));
        fitBackground();
    }

    private void resize(int count) {
        while (rectangles.size() > count) {
            rectangles.remove(rectangles.size() - 1).unload();
            speeds.remove(speeds.size() - 1);
        }

        while (rectangles.size() < count)
            rectangles.add(create(rectangles.size()));
    }

    private RectangleRenderer create(int index) {
        RectangleRenderer rectangle;
        if (index % 4 == 3) {
            rectangle = new RectangleRenderer(colorMaterial);
            rectangle.setColorMode(new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f));
        } else {
            rectangle = new RectangleRenderer(spriteMaterial);
            float x = random.nextInt(TILES) / (float) TILES;
            float y = random.nextInt(TILES) / (float) TILES;
            rectangle.setTexturePosition(new Vector4f(x, y, x + 1f / TILES, y + 1f / TILES));
        }

        float size = 8 + random.nextFloat() * 40;
        rectangle.setSizePix(size, size * (0.5f + random.nextFloat()));
        rectangle.setReferenceDirection(EDirection.None);
        rectangle.setPosition(new Vector2f(random.nextFloat() * area.x, random.nextFloat() * area.y));
        speeds.add((random.nextFloat() - 0.5f) * 4f);
        return rectangle;
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = context().getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (keyboard.keyPressed(GLFW_KEY_UP))
            resize(Math.min(MAX_COUNT, rectangles.size() * 2));
        if (keyboard.keyPressed(GLFW_KEY_DOWN))
            resize(Math.max(1, rectangles.size() / 2));
        if (keyboard.keyPressed(GLFW_KEY_S))
            setDrawSortKey(batch2D().getDrawSortKey() == null ? DrawSortKeys.layerThenState() : null);
        if (keyboard.keyPressed(GLFW_KEY_M)) {
            EBatch2DMode[] modes = EBatch2DMode.values();
            engine().setBatch2DMode(modes[(engine().batch2DMode().ordinal() + 1) % modes.length]);
        }

        time += GameTime.DeltaTime();
        for (int i = 0; i < rectangles.size(); ++i)
            rectangles.get(i).setRotationZ(time * speeds.get(i));

        counterTimer -= GameTime.DeltaTime();
        if (counterTimer <= 0) {
            counterTimer = 0.5f;
            counters.setText(describeCounters());
            fitBackground();
        }
    }

    // Around the two texts, with a margin
    private void fitBackground() {
        float right = Math.max(help.leftUpPosition().x + help.size().x, counters.leftUpPosition().x + counters.size().x);
        float bottom = counters.leftUpPosition().y + counters.size().y;
        textBackground.setSizePix(right + 8 - 4, bottom + 14 - 4);
    }

    private String describeCounters() {
        JSONObject stats = EngineStats.toJson();
        return "N " + rectangles.size() + "   mode " + engine().batch2DMode().optionName()
                + (batch2D().getDrawSortKey() == null ? "" : " sorted by state") + "   fps "
                + stats.optInt("fps") + "   frame " + stats.optDouble("frameMs") + " ms   render CPU "
                + stats.optDouble("renderCpuMs") + " ms"
                + "\ndraw calls " + stats.optDouble("drawCallsPerFrame") + " (batch "
                + stats.optDouble("batchFlushesPerFrame") + " for " + stats.optDouble("batchedDrawsPerFrame")
                + " draws, barriers " + stats.optDouble("batchBarriersPerFrame") + ")   program binds "
                + stats.optDouble("programBindsPerFrame") + "   texture binds " + stats.optDouble("textureBindsPerFrame")
                + "   uniforms " + stats.optDouble("uniformUploadsPerFrame");
    }

    @Override
    public void display() {
        clear();

        for (RectangleRenderer rectangle : rectangles)
            rectangle.display();

        textBackground.display();
        help.display();
        counters.display();

        super.display();
    }

    @Override
    public void unload() {
        super.unload();

        for (RectangleRenderer rectangle : rectangles)
            rectangle.unload();
        rectangles.clear();
        speeds.clear();
        textBackground.unload();
        help.unload();
        counters.unload();
        // The materials are freed with their last renderer
    }
}
