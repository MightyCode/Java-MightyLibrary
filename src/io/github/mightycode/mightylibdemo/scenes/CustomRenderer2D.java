package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer.utils.EDrawMode;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2i;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;

/**
 * Shows how to draw custom geometry with a custom shader: a star (triangle fan with a color per vertex)
 * built by a Renderer subclass, and a second one built from a Mesh created by the scene.
 * Key D: toggles the depth test (the stars are at different depths and overlap).
 */
public class CustomRenderer2D extends Scene {
    private static final String SHADER = "customStar2D";
    private static final int COLOR_CHANNEL = 3;

    // A renderer which fills its own shape with a star
    private static class StarRenderer extends Renderer {
        private float time;

        public StarRenderer(float radius) {
            super(SHADER, false);

            Mesh star = getMesh();
            star.setDrawMode(EDrawMode.TriangleFan);
            fillStar(star, radius);

            addShaderValue("time", Float.class, 0f);
        }

        public void advance(float delta) {
            time += delta;
            updateShaderValue("time", time);
        }
    }

    private StarRenderer first;
    private Renderer second;
    private float secondTime;
    private boolean depthTest;

    @Override
    public void launch(String[] args) {
        setClearColor(20, 20, 40, 1f);

        Vector2i size = mainContext.getWindow().getInfo().getSizeRef();
        float centerX = size.x / 2f;
        float centerY = size.y / 2f;

        first = new StarRenderer(1f);
        first.setPosition(new Vector3f(centerX - 150, centerY, 1f));
        first.setScale(new Vector3f(180f, 180f, 1f));

        // Workaround: the constructor of Renderer always creates its own Mesh, so the one built by the scene
        // replaces it and the first one has to be unloaded by hand
        second = new Renderer(SHADER, false);
        second.getMesh().unload();
        Mesh secondMesh = new Mesh(false);
        secondMesh.setDrawMode(EDrawMode.TriangleFan);
        fillStar(secondMesh, 0.6f);
        second.setMesh(secondMesh);
        second.addShaderValue("time", Float.class, 0f);
        second.setPosition(new Vector3f(centerX, centerY, -1f));
        second.setScale(new Vector3f(280f, 280f, 1f));

        applyDepth();
    }

    // A star: the center, then outer and inner points alternately, back to the first one
    private static void fillStar(Mesh mesh, float innerRatio) {
        int points = 5;
        int vertexCount = points * 2 + 2;
        float[] positions = new float[vertexCount * 2];
        float[] colors = new float[vertexCount * 4];

        // The center
        colors[0] = colors[1] = colors[2] = colors[3] = 1f;

        for (int i = 0; i <= points * 2; ++i) {
            float radius = i % 2 == 0 ? 1f : innerRatio;
            double angle = Math.PI / 2 + i * Math.PI / points;
            int vertex = i + 1;

            positions[vertex * 2] = (float) (Math.cos(angle) * radius);
            positions[vertex * 2 + 1] = (float) (Math.sin(angle) * radius);

            colors[vertex * 4] = 0.5f + 0.5f * (float) Math.cos(i * 0.9);
            colors[vertex * 4 + 1] = 0.5f + 0.5f * (float) Math.sin(i * 0.9);
            colors[vertex * 4 + 2] = i % 2 == 0 ? 1f : 0.4f;
            colors[vertex * 4 + 3] = 1f;
        }

        mesh.addVboFloat(positions, 2, Mesh.STATIC_STORE);

        // Workaround: a Mesh gives the next channel to each VBO (no explicit channel), so the color goes
        // to channel 3 only after filling channels 1 and 2 with unused data
        float[] unused = new float[vertexCount * 2];
        mesh.addVboFloat(unused, 2, Mesh.STATIC_STORE);
        mesh.addVboFloat(unused, 2, Mesh.STATIC_STORE);
        mesh.disableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);
        mesh.disableVbo(Mesh.COMMON_NORMAL_POSITION_CHANNEL);

        mesh.addVboFloat(colors, 4, Mesh.STATIC_STORE);
    }

    // Workaround: the depth test depends on the tag of the shader (2D: none), only the Mesh can change it
    private void applyDepth() {
        first.getMesh().setDimensionTo2D(!depthTest);
        second.getMesh().setDimensionTo2D(!depthTest);
    }

    @Override
    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();
        KeyboardManager keyboardManager = mainContext.getKeyboardManager();

        if (inputManager.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        if (keyboardManager.keyPressed(GLFW_KEY_D)) {
            depthTest = !depthTest;
            applyDepth();
        }

        float delta = GameTime.DeltaTime();
        first.advance(delta);
        first.setRotationZ(first.getRotationAngle() + delta * 0.5f);

        secondTime += delta;
        second.updateShaderValue("time", secondTime);
        second.setRotationZ(-secondTime * 0.3f);
    }

    @Override
    public void display() {
        clear();

        // The second one is drawn last: it hides the first one only when the depth test is off
        first.display();
        second.display();

        super.display();
    }

    @Override
    public void unload() {
        super.unload();

        first.unload();
        second.unload();
    }
}
