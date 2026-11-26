package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawSortKeys;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.ArcRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EllipseRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.GridRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RegularPolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.Shape2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.TriangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
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
 * colored one, of several sizes and turning; or, after F, N vector shapes of the engine mixed (rounded rectangle,
 * circle, ellipse, triangle, arc, regular polygon, grid: another kind at each draw, in the order of the calls), of
 * random colors, a third of them with a stroke (widths, alignments and units varied), without texture. Page Up
 * doubles N, Page Down halves it, M changes the mode of the 2D batch (off, orphan, ring, instanced), O sorts the
 * draws by state (DrawSortKeys.layerThenState: the draws of the same state together). Z Q S D or the arrows move the
 * camera, the right button drags it, the wheel zooms (the texts stay on screen). The counters are those of the
 * command stats (renderCpuMs: CPU time of the display of the windows, without the buffer swap).
 */
public class BatchStress2D extends Scene {
    private static final int START_COUNT = 1000;
    private static final int MAX_COUNT = 1 << 17;
    // Tiles per side of the tile set used as an atlas
    private static final int TILES = 8;

    // Kinds of vector shapes of the shapes mode
    private static final int SHAPE_KINDS = 7;
    private static final long SEED = 42;

    private final List<Shape2DRenderer> renderers = new ArrayList<>();
    private final List<Float> speeds = new ArrayList<>();
    // Same seed at each launch: the same picture for the same N
    private final Random random = new Random(SEED);
    private Vector2i area;
    // Vector shapes instead of rectangles (F)
    private boolean shapes;

    private Material spriteMaterial;
    private Material colorMaterial;

    private Text help;
    // Grey rounded rectangle under the texts, to read them over the moving shapes
    private RoundedRectangleRenderer textBackground;
    private Text counters;
    private float counterTimer;
    private float time;

    // The atlas of the mix (tile set and frames of the slime, tools/generate_stress_atlas.py) and its animation
    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"stress"};
    }

    @Override
    public void launch(String[] args) {
        setClearColor(30, 30, 40, 1f);
        area = context().getWindow().getInfo().getVirtualSizeCopy();
        Camera2D hud = camera2D().copy();
        DemoCamera.free(this);

        spriteMaterial = new Material("texture2D").setMainTexture("tileset");
        colorMaterial = new Material("colorShape2D");
        // Kept by the scene: N may drop every renderer of one of them, then grow again
        spriteMaterial.retain();
        colorMaterial.retain();
        resize(START_COUNT);

        help = new Text();
        help.setFontSize(20).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 10));
        help.setText("PgUp : N x2   PgDn : N / 2   F : rectangles / shapes   M : mode of the batch   O : sort by state   ZQSD/arrows : move   wheel : zoom   ESC : menu");
        help.setReferenceCamera(hud);
        counters = new Text();
        counters.setReferenceCamera(hud);
        counters.setFontSize(18).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 40));

        textBackground = new RoundedRectangleRenderer();
        textBackground.setCornerRadius(8f);
        textBackground.setColorMode(new Color4f(0.25f, 0.25f, 0.27f, 0.88f));
        textBackground.setPosition(new Vector2f(4, 4));
        textBackground.setReferenceCamera(hud);
        fitBackground();
    }

    private void resize(int count) {
        while (renderers.size() > count) {
            renderers.remove(renderers.size() - 1).unload();
            speeds.remove(speeds.size() - 1);
        }

        while (renderers.size() < count)
            renderers.add(create(renderers.size()));
    }

    // The same picture for the same N in each mode: the renderers made again from the seed
    private void toggleShapes() {
        int count = renderers.size();
        resize(0);
        shapes = !shapes;
        random.setSeed(SEED);
        resize(count);
    }

    private Shape2DRenderer create(int index) {
        return shapes ? createShape(index) : createRectangle(index);
    }

    private Shape2DRenderer createShape(int index) {
        float size = 8 + random.nextFloat() * 40;
        VectorShape2DRenderer shape;
        switch (index % SHAPE_KINDS) {
            case 0:
                shape = new RoundedRectangleRenderer().setCornerRadius(size * 0.2f);
                break;
            case 1:
                shape = new CircleRenderer();
                break;
            case 2:
                shape = new EllipseRenderer();
                break;
            case 3:
                shape = new TriangleRenderer();
                break;
            case 4:
                shape = new ArcRenderer().setInnerRatio(0.4f)
                        .setAngles(random.nextFloat() * 6.28f, 1.5f + random.nextFloat() * 4f);
                break;
            case 5:
                shape = new RegularPolygonRenderer(3 + random.nextInt(6));
                break;
            default:
                shape = new GridRenderer().setSpacing(size / 4, size / 4);
                break;
        }

        shape.setColorMode(randomColor());
        if (index % 3 == 0) {
            EStrokeAlignment[] alignments = EStrokeAlignment.values();
            EStrokeUnit[] units = EStrokeUnit.values();
            shape.setStrokeWidth(1 + random.nextFloat() * 3)
                    .setStrokeAlignment(alignments[random.nextInt(alignments.length)])
                    .setStrokeUnit(units[random.nextInt(units.length)])
                    .setStrokeColor(randomColor());
        }

        place(shape, size);
        return shape;
    }

    private Color4f randomColor() {
        return new Color4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1f);
    }

    private Shape2DRenderer createRectangle(int index) {
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

        place(rectangle, 8 + random.nextFloat() * 40);
        return rectangle;
    }

    // Size, place and turning speed, the same for the rectangles and the shapes
    private void place(Shape2DRenderer renderer, float size) {
        renderer.setSizePix(size, size * (0.5f + random.nextFloat()));
        renderer.setReferenceDirection(EDirection.None);
        renderer.setPosition(new Vector2f(random.nextFloat() * area.x, random.nextFloat() * area.y));
        speeds.add((random.nextFloat() - 0.5f) * 4f);
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = context().getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (keyboard.keyPressed(GLFW_KEY_PAGE_UP))
            resize(Math.min(MAX_COUNT, renderers.size() * 2));
        if (keyboard.keyPressed(GLFW_KEY_PAGE_DOWN))
            resize(Math.max(1, renderers.size() / 2));
        if (keyboard.keyPressed(GLFW_KEY_F))
            toggleShapes();
        if (keyboard.keyPressed(GLFW_KEY_O))
            setDrawSortKey(batch2D().getDrawSortKey() == null ? DrawSortKeys.layerThenState() : null);
        if (keyboard.keyPressed(GLFW_KEY_M)) {
            EBatch2DMode[] modes = EBatch2DMode.values();
            engine().setBatch2DMode(modes[(engine().batch2DMode().ordinal() + 1) % modes.length]);
        }

        time += GameTime.DeltaTime();
        for (int i = 0; i < renderers.size(); ++i)
            renderers.get(i).setRotationZ(time * speeds.get(i));

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
        return (shapes ? "shapes " : "rectangles ") + renderers.size() + "   mode " + engine().batch2DMode().optionName()
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

        for (Shape2DRenderer renderer : renderers)
            renderer.display();

        textBackground.display();
        help.display();
        counters.display();

        super.display();
    }

    @Override
    public void unload() {
        super.unload();

        for (Shape2DRenderer renderer : renderers)
            renderer.unload();
        renderers.clear();
        speeds.clear();
        textBackground.unload();
        help.unload();
        counters.unload();
        // Freed now: their renderers are gone
        spriteMaterial.release();
        colorMaterial.release();
    }
}
