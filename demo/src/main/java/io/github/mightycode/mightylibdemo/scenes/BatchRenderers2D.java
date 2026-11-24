package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.debug.GraphRenderer;
import io.github.mightycode.mightylib.graphics.debug.MatrixRenderer;
import io.github.mightycode.mightylib.graphics.debug.NodesRenderer;
import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera2DComponent;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The renderers that go through the 2D batch: a long paragraph and colored texts, a tile map larger than the screen
 * seen by a moving camera (only its visible tiles are drawn), several animations, and the debug renderers (nodes,
 * graph, matrix). M changes the mode of the batch (off, orphan, ring, instanced) to compare the pictures. The camera
 * follows a path until Space stops it or you move it (Z Q S D, arrows, right drag, wheel), Space starts it again. The counters are those of the command stats.
 */
public class BatchRenderers2D extends Scene {
    private static final int ANIMATIONS = 6;

    private LayersTileMapRenderer mapRenderer;
    private final List<Animation2DRenderer> animations = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();
    private NodesRenderer nodes;
    private GraphRenderer graph;
    private MatrixRenderer matrix;
    private Text help, counters;
    private Camera2D hudCamera;

    private float time;
    private boolean cameraMoves = true;
    private FreeCamera2DComponent freeCamera;
    private float counterTimer;
    private final Vector2f cameraPosition = new Vector2f();
    // The mode of the batch of the engine when the scene starts, given back when it ends (M changes it)
    private EBatch2DMode modeAtLaunch;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"anim", "map-assets"};
    }

    @Override
    public void launch(String[] args) {
        setClearColor(52, 189, 235, 1f);
        modeAtLaunch = engine().batch2DMode();
        hudCamera = camera2D().copy();
        freeCamera = DemoCamera.free(this);

        mapRenderer = new LayersTileMapRenderer("texture2D", false);
        mapRenderer.setTileMap(resources().getResource(TileMap.class, "map"));

        for (int i = 0; i < ANIMATIONS; ++i) {
            Animation2DRenderer animation = new Animation2DRenderer("texture2D");
            animation.setMainTextureChannel("slime");
            Animator animator = new Animator();
            animator.addAndInitAnimation("first", resources().getResource(AnimationData.class, "slime"), true);
            animation.init(animator);
            animation.setShiftRotation(EDirection.None);
            animation.setScale(new Vector2f(4 + i));
            animation.setHorizontalFlip(i % 2 != 0);
            animation.setPosition(new Vector2f(150 + i * 170, 520));
            animations.add(animation);
        }

        for (int i = 0; i < 4; ++i) {
            Text text = new Text();
            text.setFontSize(16 + i * 8).setColor(new Color4f(0.2f * i, 0.1f, 0.5f, 1f));
            text.setText("Text " + i + " in the batch : AaBbQq 0123");
            text.setPosition(new Vector2f(30, 150 + i * 36));
            texts.add(text);
        }

        Text paragraph = new Text();
        paragraph.setFontSize(18).setAlignment(ETextAlignment.Center).setMaxWidth(420);
        paragraph.setText("A long paragraph wrapped at the spaces and centered: every character is a quad written "
                + "in the batch, with all the other texts, in the same draw call. Press M to compare the modes of the "
                + "batch, the picture must not change.");
        paragraph.setPosition(new Vector2f(760, 150));
        texts.add(paragraph);

        buildDebugRenderers();

        help = new Text();
        help.setFontSize(18).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 10));
        help.setReferenceCamera(hudCamera);
        help.setText("M : mode of the batch   Space : camera path   ZQSD/arrows, right drag, wheel : move and zoom   ESC : menu");
        counters = new Text();
        counters.setFontSize(16).setReference(EDirection.LeftUp).setPosition(new Vector2f(10, 36));
        counters.setReferenceCamera(hudCamera);
    }

    private void buildDebugRenderers() {
        List<Vector2f> points = new ArrayList<>();
        for (int i = 0; i < 12; ++i)
            points.add(new Vector2f(60 + i * 70, 640 + (i % 3) * 30));

        nodes = new NodesRenderer(camera2D());
        nodes.setNodeSize(10);
        nodes.init(points);
        nodes.updateNodes(points, new Vector4f(), new Vector4f(), 1f);

        SortedMap<Vector2f, Long> ordered = new TreeMap<>((a, b) -> Float.compare(a.x, b.x));
        Map<Vector2f, List<Vector2f>> connections = new HashMap<>();
        for (int i = 0; i < points.size(); ++i) {
            ordered.put(points.get(i), (long) i);
            List<Vector2f> neighbours = new ArrayList<>();
            if (i > 0)
                neighbours.add(points.get(i - 1));
            if (i > 1)
                neighbours.add(points.get(i - 2));
            connections.put(points.get(i), neighbours);
        }
        graph = new GraphRenderer(camera2D());
        graph.setEdgeSize(4);
        graph.init(ordered, connections);
        graph.updateRoads(ordered, connections, new Vector4f(), new Vector4f(), 1f);

        int[][] values = new int[6][8];
        for (int y = 0; y < values.length; ++y) {
            for (int x = 0; x < values[y].length; ++x)
                values[y][x] = x * 7 + y * 3;
        }
        matrix = new MatrixRenderer(camera2D(), 48);
        matrix.updateRenderer(values, 0, 64, new Vector4f(900, 560, 1220, 700));
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = context().getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (keyboard.keyPressed(GLFW_KEY_M)) {
            EBatch2DMode[] modes = EBatch2DMode.values();
            engine().setBatch2DMode(modes[(engine().batch2DMode().ordinal() + 1) % modes.length]);
        }
        if (keyboard.keyPressed(GLFW_KEY_SPACE))
            cameraMoves = !cameraMoves;
        // The player takes the camera from the path
        if (freeCamera.isInteracting())
            cameraMoves = false;

        if (cameraMoves) {
            time += GameTime.DeltaTime();
            camera2D().setPos(cameraPosition.set((float) Math.sin(time * 0.4) * 500, (float) Math.cos(time * 0.3) * 250));
        }

        for (Animation2DRenderer animation : animations)
            animation.update();
        mapRenderer.update();

        counterTimer -= GameTime.DeltaTime();
        if (counterTimer <= 0) {
            counterTimer = 0.5f;
            counters.setText(describeCounters());
        }
    }

    private String describeCounters() {
        JSONObject stats = EngineStats.toJson();
        return "mode " + engine().batch2DMode().optionName() + "   fps " + stats.optInt("fps") + "   draw calls "
                + stats.optDouble("drawCallsPerFrame") + " (batch " + stats.optDouble("batchFlushesPerFrame")
                + " for " + stats.optDouble("batchedDrawsPerFrame") + " draws, barriers "
                + stats.optDouble("batchBarriersPerFrame") + ")";
    }

    @Override
    public void display() {
        clear();

        mapRenderer.display();
        for (Animation2DRenderer animation : animations)
            animation.display();
        for (Text text : texts)
            text.display();
        matrix.display();
        graph.display();
        nodes.display();

        help.display();
        counters.display();

        super.display();
    }

    @Override
    public void unload() {
        super.unload();

        if (modeAtLaunch != null)
            engine().setBatch2DMode(modeAtLaunch);
        mapRenderer.unload();
        for (Animation2DRenderer animation : animations)
            animation.unload();
        animations.clear();
        for (Text text : texts)
            text.unload();
        texts.clear();
        nodes.unload();
        graph.unload();
        matrix.unload();
        help.unload();
        counters.unload();
    }
}
