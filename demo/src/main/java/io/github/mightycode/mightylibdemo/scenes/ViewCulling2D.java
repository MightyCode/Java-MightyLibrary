package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.CircleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeAlignment;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.EStrokeUnit;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PointsRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PolygonRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.PolylineRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RoundedRectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylib.utils.math.geometry.Polygon;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The view culling of the 2D draws: a world about twenty screens large filled with every batched renderer (rectangles,
 * animations, vector shapes with strokes in world units and in pixels, polylines, polygons, points, texts, a tile
 * map, meshes), a few of them out of the culling (setViewCulling(false)) or with a margin (setViewCullMargin). The
 * camera is the free camera of the demo (Z Q S D, arrows, right drag, wheel), + and - zoom too, Q and E turn it. C turns the culling of the engine on or off,
 * I reduces its test zone by 200 pixels (what is closer to the edges disappears: nothing may disappear farther),
 * B draws the box of each tested draw (green drawn, red culled), F freezes the boxes of the views (move the camera
 * to see what the frozen box would cull). The counters are those of the command stats and batch.
 */
public class ViewCulling2D extends Scene {
    private static final int INSET = 200;

    private final List<Renderer> items = new ArrayList<>();
    private LayersTileMapRenderer map;
    private Text help, counters;
    private Camera2D hudCamera;

    private float counterTimer;
    private float rotation;
    private boolean culling = true, inset, boxes, frozen;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"anim", "map-assets"};
    }

    @Override
    public void launch(String[] args) {
        setClearColor(40, 44, 60, 1f);
        hudCamera = camera2D().copy();
        culling = engine().isViewCulling2D();

        Vector2i screen = context().getWindow().getInfo().getVirtualSizeCopy();
        // About twenty screens: 5 by 4
        float width = screen.x * 5f, height = screen.y * 4f;
        Random random = new Random(7);

        map = new LayersTileMapRenderer("texture2D", false);
        map.setTileMap(resources().getResource(TileMap.class, "map"));

        for (int i = 0; i < 500; ++i) {
            RectangleRenderer rectangle = new RectangleRenderer(i % 3 == 0 ? "colorShape2D" : "texture2D");
            if (i % 3 == 0)
                rectangle.setColorMode(randomColor(random));
            else
                rectangle.setMainTextureChannel("slime");
            rectangle.setSizePix(30 + random.nextInt(70), 30 + random.nextInt(70));
            place(rectangle, random, width, height);
            items.add(rectangle);
        }

        for (int i = 0; i < 60; ++i) {
            Animation2DRenderer animation = new Animation2DRenderer("texture2D");
            animation.setMainTextureChannel("slime");
            Animator animator = new Animator();
            animator.addAndInitAnimation("first", resources().getResource(AnimationData.class, "slime"), true);
            animation.init(animator);
            animation.setShiftRotation(EDirection.None);
            animation.setScale(new Vector2f(3 + random.nextInt(5)));
            place(animation, random, width, height);
            items.add(animation);
        }

        for (int i = 0; i < 120; ++i) {
            switch (i % 3) {
                case 0: {
                    CircleRenderer circle = new CircleRenderer();
                    circle.setSizePix(40 + random.nextInt(40), 40 + random.nextInt(40));
                    circle.setColorMode(randomColor(random));
                    stroke(circle, random, i);
                    place(circle, random, width, height);
                    items.add(circle);
                    break;
                }
                case 1: {
                    RoundedRectangleRenderer rounded = new RoundedRectangleRenderer().setCornerRadius(10);
                    rounded.setSizePix(50 + random.nextInt(60), 30 + random.nextInt(40));
                    rounded.setColorMode(randomColor(random));
                    stroke(rounded, random, i);
                    place(rounded, random, width, height);
                    items.add(rounded);
                    break;
                }
                default: {
                    Polygon polygon = Polygon.CreateBuilder().addVertex(new Vector2f(0, 0))
                            .addVertex(new Vector2f(1, 0.3f)).addVertex(new Vector2f(0.6f, 1))
                            .addVertex(new Vector2f(0.1f, 0.8f)).build();
                    PolygonRenderer shape = new PolygonRenderer(polygon);
                    shape.setSizePix(60 + random.nextInt(50), 60 + random.nextInt(50));
                    shape.setColorMode(randomColor(random));
                    stroke(shape, random, i);
                    place(shape, random, width, height);
                    items.add(shape);
                    break;
                }
            }
        }

        for (int i = 0; i < 30; ++i) {
            List<Vector2f> points = new ArrayList<>();
            for (int p = 0; p < 5; ++p)
                points.add(new Vector2f(p * 35, (p % 2) * (20 + random.nextInt(30))));
            PolylineRenderer line = new PolylineRenderer().setPoints(points);
            line.setStrokeWidth(3 + random.nextInt(6));
            line.setStrokeUnit(i % 2 == 0 ? EStrokeUnit.WORLD : EStrokeUnit.SCREEN_PIXELS);
            line.setStrokeColor(randomColor(random));
            place(line, random, width, height);
            items.add(line);
        }

        for (int i = 0; i < 10; ++i) {
            PointsRenderer points = new PointsRenderer().setPointSize(10 + random.nextInt(10));
            for (int p = 0; p < 12; ++p)
                points.addPoint(new Vector2f(random.nextInt(200), random.nextInt(120)));
            points.setColorMode(randomColor(random));
            place(points, random, width, height);
            items.add(points);
        }

        for (int i = 0; i < 60; ++i) {
            Text text = new Text();
            text.setFontSize(14 + random.nextInt(14)).setColor(randomColor(random));
            text.setText("Text " + i + " AaBbQq");
            place(text, random, width, height);
            items.add(text);
        }

        for (int i = 0; i < 20; ++i) {
            MeshRenderer quad = new MeshRenderer("texture2D", true);
            Mesh mesh = quad.getMesh();
            mesh.keepCpuData();
            mesh.setEbo(new int[]{0, 1, 2, 0, 2, 3});
            mesh.addVboFloat(new float[]{-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f}, 2, Mesh.STATIC_STORE);
            mesh.addVboFloat(new float[]{0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f}, 2, Mesh.STATIC_STORE);
            quad.setMainTextureChannel("slime");
            quad.setScale(new Vector3f(60 + random.nextInt(60), 40 + random.nextInt(40), 1));
            place(quad, random, width, height);
            items.add(quad);
        }

        // Out of the culling: always written, whatever the view
        for (int i = 0; i < 5; ++i)
            items.get(i).setViewCulling(false);
        // A margin of the renderer: kept while it is within 300 units of the view
        for (int i = 5; i < 10; ++i)
            items.get(i).setViewCullMargin(300);

        camera2D().setPos(new Vector2f(width * 0.5f - screen.x * 0.5f, height * 0.5f - screen.y * 0.5f));
        DemoCamera.free(this);

        help = new Text();
        help.setFontSize(16).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 10));
        help.setReferenceCamera(hudCamera);
        help.setText("ZQSD/arrows, right drag : move   wheel, + - : zoom   Q E : turn   C : culling   I : test zone -" + INSET
                + " px   B : boxes   F : freeze   ESC : menu");
        counters = new Text();
        counters.setFontSize(16).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 34));
        counters.setReferenceCamera(hudCamera);
    }

    private static Color4f randomColor(Random random) {
        return new Color4f(0.3f + random.nextFloat() * 0.7f, 0.3f + random.nextFloat() * 0.7f,
                0.3f + random.nextFloat() * 0.7f, 1f);
    }

    // Half of the strokes in world units, half in pixels, each alignment
    private static void stroke(io.github.mightycode.mightylib.graphics.renderer._2D.shape.VectorShape2DRenderer shape,
                               Random random, int i) {
        shape.setStrokeWidth(2 + random.nextInt(8));
        shape.setStrokeUnit(i % 2 == 0 ? EStrokeUnit.WORLD : EStrokeUnit.SCREEN_PIXELS);
        shape.setStrokeAlignment(EStrokeAlignment.values()[i % EStrokeAlignment.values().length]);
        shape.setStrokeColor(new Color4f(1f, 1f, 1f, 1f));
    }

    private static void place(Renderer renderer, Random random, float width, float height) {
        renderer.setX(random.nextFloat() * width);
        renderer.setY(random.nextFloat() * height);
        renderer.setRotationZ(random.nextFloat() * 6.28f);
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = context().getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        float delta = GameTime.DeltaTime();
        // The free camera (updated with the scene) moves and zooms with the wheel; + and - zoom too
        float zoom = camera2D().getZoomLevel().x;
        if (keyboard.getKeyState(GLFW_KEY_EQUAL) || keyboard.getKeyState(GLFW_KEY_KP_ADD))
            camera2D().setZoomLevel(Math.min(8f, zoom * (1f + delta)));
        if (keyboard.getKeyState(GLFW_KEY_MINUS) || keyboard.getKeyState(GLFW_KEY_KP_SUBTRACT))
            camera2D().setZoomLevel(Math.max(0.1f, zoom / (1f + delta)));
        if (keyboard.getKeyState(GLFW_KEY_Q))
            rotation -= delta;
        if (keyboard.getKeyState(GLFW_KEY_E))
            rotation += delta;

        if (keyboard.keyPressed(GLFW_KEY_C)) {
            culling = !culling;
            engine().setViewCulling2D(culling);
        }
        if (keyboard.keyPressed(GLFW_KEY_I)) {
            inset = !inset;
            engine().commands().execute("batch cull inset " + (inset ? INSET : 0));
        }
        if (keyboard.keyPressed(GLFW_KEY_B)) {
            boxes = !boxes;
            engine().commands().execute("batch cull show " + (boxes ? "on" : "off"));
        }
        if (keyboard.keyPressed(GLFW_KEY_F)) {
            frozen = !frozen;
            engine().commands().execute("batch cull " + (frozen ? "freeze" : "unfreeze"));
        }

        camera2D().setRotation(rotation);

        for (Renderer item : items) {
            if (item instanceof Animation2DRenderer)
                ((Animation2DRenderer) item).update();
        }
        map.update();

        counterTimer -= delta;
        if (counterTimer <= 0) {
            counterTimer = 0.5f;
            counters.setText(describeCounters());
        }
    }

    private String describeCounters() {
        JSONObject stats = EngineStats.toJson();
        return "culling " + (culling ? "on" : "off") + (inset ? " (zone -" + INSET + " px)" : "")
                + (boxes ? " boxes" : "") + (frozen ? " frozen" : "") + "   fps " + stats.optInt("fps")
                + "   tested " + stats.optDouble("viewCullTestedPerFrame") + " culled "
                + stats.optDouble("viewCulledPerFrame") + " no box " + stats.optDouble("viewCullNoBoundsPerFrame")
                + "   draw calls " + stats.optDouble("batchFlushesPerFrame") + " bytes "
                + stats.optLong("batchBytesPerFrame") + "   render " + stats.optDouble("renderCpuMs") + " ms";
    }

    @Override
    public void display() {
        clear();

        map.display();
        for (Renderer item : items)
            item.display();

        help.display();
        counters.display();

        super.display();
    }

    @Override
    public void unload() {
        super.unload();

        // The tools of the verification are the engine's: left as they were found
        if (inset)
            engine().commands().execute("batch cull inset 0");
        if (boxes)
            engine().commands().execute("batch cull show off");
        if (frozen)
            engine().commands().execute("batch cull unfreeze");
        engine().setViewCulling2D(true);

        map.unload();
        for (Renderer item : items)
            item.unload();
        items.clear();
        help.unload();
        counters.unload();
    }
}
