package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylibdemo.main.DemoCamera;
import io.github.mightycode.mightylib.graphics.renderer.EBlendMode;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A Material shared by renderers: a grid of sprites drawing with one material (T changes its texture, B its blend
 * mode: all the sprites follow), colored rectangles sharing one material with a color of their own each (a value of
 * the renderer wins over the one of the material), two renderers on one Mesh, and the five blend modes over a
 * colored background. The counters are those of the command stats; gl materials lists what is shared.
 */
public class SharedMaterial2D extends Scene {
    private static final String[] TEXTURES = {"painting", "hexagon", "slime"};
    private static final EBlendMode[] BLENDS = {EBlendMode.ALPHA, EBlendMode.ADDITIVE, EBlendMode.MULTIPLY};

    private static final int COLUMNS = 6;
    private static final int ROWS = 3;

    // One material for the whole grid: changed once, every sprite changes
    private Material spriteMaterial;
    private final List<RectangleRenderer> sprites = new ArrayList<>();
    private int textureIndex;
    private int blendIndex;

    // One material for the colored rectangles, a color each as a value of the renderer
    private Material colorMaterial;
    private final List<RectangleRenderer> colored = new ArrayList<>();

    // One Mesh drawn by two renderers
    private Mesh sharedMesh;
    private MeshRenderer leftOfMesh;
    private MeshRenderer rightOfMesh;

    // The five blend modes, each over the same background
    private RectangleRenderer background;
    private final List<RectangleRenderer> blendSamples = new ArrayList<>();
    private final List<Text> blendLabels = new ArrayList<>();

    private Text help;
    private Text counters;
    private float counterTimer;

    @Override
    public void launch(String[] args) {
        DemoCamera.free(this);
        setClearColor(225, 225, 232, 1f);

        // The grid of sprites: one material, retained by each of them
        spriteMaterial = new Material("texture2D").setMainTexture(TEXTURES[0]);
        for (int row = 0; row < ROWS; ++row) {
            for (int column = 0; column < COLUMNS; ++column) {
                RectangleRenderer sprite = new RectangleRenderer(spriteMaterial);
                sprite.setSizePix(80, 80);
                sprite.setPosition(new Vector2f(40 + column * 95, 90 + row * 95));
                sprites.add(sprite);
            }
        }

        // Colored rectangles: the shader and the state are shared, the color is each one's own
        colorMaterial = new Material("colorShape2D");
        Color4f[] colors = {new Color4f(0.9f, 0.3f, 0.3f, 1f), new Color4f(0.3f, 0.8f, 0.4f, 1f),
                new Color4f(0.3f, 0.5f, 0.95f, 1f), new Color4f(0.95f, 0.8f, 0.2f, 1f)};
        for (int i = 0; i < colors.length; ++i) {
            RectangleRenderer rectangle = new RectangleRenderer(colorMaterial);
            rectangle.setSizePix(80, 30);
            rectangle.setPosition(new Vector2f(40 + i * 95, 390));
            rectangle.setColorMode(colors[i]);
            colored.add(rectangle);
        }

        // Two renderers, one Mesh: the vertices are uploaded once
        sharedMesh = new Mesh(true);
        sharedMesh.addVboFloat(Mesh.COMMON_VERTEX_POSITION_CHANNEL, new float[]{0, 0, 1, 0, 1, 1, 0, 1}, 2,
                Mesh.STATIC_STORE);
        sharedMesh.setEbo(new int[]{0, 1, 2, 0, 2, 3});
        leftOfMesh = new MeshRenderer(sharedMesh, "colorShape2D");
        leftOfMesh.setColorMode(new Color4f(0.9f, 0.5f, 0.9f, 1f));
        leftOfMesh.setPosition(new Vector3f(40, 440, 0));
        leftOfMesh.setScale(new Vector3f(80, 50, 1));
        rightOfMesh = new MeshRenderer(sharedMesh, "colorShape2D");
        rightOfMesh.setColorMode(new Color4f(0.4f, 0.9f, 0.9f, 1f));
        rightOfMesh.setPosition(new Vector3f(135, 440, 0));
        rightOfMesh.setScale(new Vector3f(40, 80, 1));

        // The five blend modes over the same background
        background = new RectangleRenderer("colorShape2D");
        background.setColorMode(new Color4f(0.85f, 0.6f, 0.2f, 1f));
        background.setSizePix(580, 190);
        background.setPosition(new Vector2f(660, 90));

        EBlendMode[] modes = EBlendMode.values();
        for (int i = 0; i < modes.length; ++i) {
            RectangleRenderer sample = new RectangleRenderer("colorShape2D");
            sample.setColorMode(new Color4f(0.3f, 0.6f, 0.95f, 0.5f));
            sample.setSizePix(90, 110);
            sample.setPosition(new Vector2f(670 + i * 113, 110));
            sample.setBlendMode(modes[i]);
            blendSamples.add(sample);

            blendLabels.add(label(14, new Vector2f(715 + i * 113, 232)).setText(modes[i].name()));
        }

        Camera2D hud = camera2D().copy();
        help = label(20, new Vector2f(640, 20));
        help.setReferenceCamera(hud);
        help.setText("T : texture of the shared material   B : its blend mode   ESC : menu");
        counters = label(16, new Vector2f(640, 650));
        counters.setReferenceCamera(hud);
    }

    private static Text label(int size, Vector2f position) {
        Text text = new Text();
        text.setFontSize(size)
                .setReference(EDirection.Up)
                .setAlignment(ETextAlignment.Center)
                .setPosition(position);
        return text;
    }

    @Override
    public void update() {
        super.update();

        KeyboardManager keyboard = context().getKeyboardManager();
        if (keyboard.keyPressed(GLFW_KEY_ESCAPE)) {
            sceneManager().setNewScene(new MenuScene(), new String[]{});
            return;
        }

        // One change in the material: the whole grid follows
        if (keyboard.keyPressed(GLFW_KEY_T)) {
            textureIndex = (textureIndex + 1) % TEXTURES.length;
            spriteMaterial.setMainTexture(TEXTURES[textureIndex]);
        }

        if (keyboard.keyPressed(GLFW_KEY_B)) {
            blendIndex = (blendIndex + 1) % BLENDS.length;
            spriteMaterial.setBlendMode(BLENDS[blendIndex]);
        }

        counterTimer -= GameTime.DeltaTime();
        if (counterTimer <= 0) {
            counterTimer = 0.5f;
            counters.setText(describeCounters());
        }
    }

    private String describeCounters() {
        JSONObject stats = EngineStats.toJson();
        return "Grid material: " + spriteMaterial.references() + " references, " + TEXTURES[textureIndex] + ", "
                + BLENDS[blendIndex] + ", version " + spriteMaterial.version()
                + "   Mesh: " + sharedMesh.references() + " references"
                + "\ndraw calls " + stats.optDouble("drawCallsPerFrame") + "   program binds "
                + stats.optDouble("programBindsPerFrame") + "   texture binds " + stats.optDouble("textureBindsPerFrame")
                + "   GL changes " + stats.optDouble("glStateChangesPerFrame") + " (skipped "
                + stats.optDouble("glStateChangesSkippedPerFrame") + ")";
    }

    @Override
    public void display() {
        clear();

        for (RectangleRenderer sprite : sprites)
            sprite.display();
        for (RectangleRenderer rectangle : colored)
            rectangle.display();
        leftOfMesh.display();
        rightOfMesh.display();

        background.display();
        for (RectangleRenderer sample : blendSamples)
            sample.display();
        for (Text label : blendLabels)
            label.display();

        help.display();
        counters.display();

        super.display();
    }

    @Override
    public void unload() {
        super.unload();

        for (RectangleRenderer sprite : sprites)
            sprite.unload();
        for (RectangleRenderer rectangle : colored)
            rectangle.unload();
        leftOfMesh.unload();
        rightOfMesh.unload();
        background.unload();
        for (RectangleRenderer sample : blendSamples)
            sample.unload();
        for (Text label : blendLabels)
            label.unload();
        help.unload();
        counters.unload();
        // The materials and the mesh are freed with their last renderer
    }
}
