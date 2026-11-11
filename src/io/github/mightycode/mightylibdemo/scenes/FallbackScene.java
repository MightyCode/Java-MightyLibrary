package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.sounds.SoundSourceCreationInfo;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

/**
 * Resources that can't be used, replaced by the fallbacks instead of crashing (debug mode, not strict):
 * from left to right an unknown texture, a texture whose file is missing, a shader whose files are missing
 * (plain magenta), an unknown animation; below, a text with an unknown font (boxes), then a shader whose GLSL does
 * not compile and one whose #include is missing (plain magenta, one log line each). ENTER plays an unknown sound
 * (strident beep). Check: the resources command lists brokenTexture, brokenShader and missingIncludeShader in
 * usingFallback, "gl summary" lists the Shader notCompilingShader in failed.
 */
public class FallbackScene extends Scene {
    private static final String BROKEN_TEXTURE = "brokenTexture";
    private static final String BROKEN_SHADER = "brokenShader";
    private static final String NOT_COMPILING_SHADER = "notCompilingShader";
    private static final String MISSING_INCLUDE_SHADER = "missingIncludeShader";
    private static final SoundSourceCreationInfo UNKNOWN_SOUND = new SoundSourceCreationInfo();
    static {
        UNKNOWN_SOUND.name = "missingSound";
        UNKNOWN_SOUND.gainNode = "noise";
    }

    private RectangleRenderer unknownTexture, brokenTexture, brokenShader, notCompilingShader, missingIncludeShader;
    private Animation2DRenderer unknownAnimation;
    private Text unknownFont;

    @Override
    public void init(String[] args) {
        // Read by their loaders, they fail: missing files
        resources.addResourceAndSetUp(TextureData.class,
                new TextureData(BROKEN_TEXTURE, resources.folder() + "textures/missing/brokenTexture.png"));
        resources.addResourceAndSetUp(ShaderData.class, new ShaderData(BROKEN_SHADER, true,
                resources.folder() + "shaders/missing/brokenV.glsl", resources.folder() + "shaders/missing/brokenF.glsl"));

        // Read, then the program does not compile: the data is loaded, its GL element fails
        String versionFolder = engine().shaderManager().versionFolder();
        String vertex = versionFolder + "2D/essential/colorShape2DV.glsl";
        resources.addResourceAndSetUp(ShaderData.class, new ShaderData(NOT_COMPILING_SHADER, true,
                vertex, versionFolder + "fallbackDemo/notCompilingF.glsl"));
        // The include is missing: the data fails while being read
        resources.addResourceAndSetUp(ShaderData.class, new ShaderData(MISSING_INCLUDE_SHADER, true,
                vertex, versionFolder + "fallbackDemo/missingIncludeF.glsl"));
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.2f, 0.2f, 0.25f);

        unknownTexture = square("texture2D", "missingTexture", 0);
        brokenTexture = square("texture2D", BROKEN_TEXTURE, 1);
        brokenShader = square(BROKEN_SHADER, "error", 2);
        notCompilingShader = colorSquare(NOT_COMPILING_SHADER, 0);
        missingIncludeShader = colorSquare(MISSING_INCLUDE_SHADER, 1);

        Animator animator = new Animator();
        animator.addAndInitAnimation("unknown", resources.getResource(AnimationData.class, "missingAnimation"), true);
        unknownAnimation = new Animation2DRenderer("texture2D");
        unknownAnimation.init(animator);
        unknownAnimation.setScale(new Vector2f(12));
        unknownAnimation.setPosition(new Vector2f(3 * 300 + 40, 40));

        unknownFont = new Text();
        // White: the glyphs are the fallback checkerboard multiplied by the color of the text
        unknownFont.setFont("missingFont")
                .setFontSize(40)
                .setReference(EDirection.LeftUp)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(40, 400))
                .setText("Unknown font: boxes");
    }

    private RectangleRenderer square(String shader, String texture, int index) {
        RectangleRenderer renderer = new RectangleRenderer(shader);
        renderer.setMainTextureChannel(texture);
        renderer.setSizePix(250, 250);
        renderer.setPosition(new Vector2f(index * 300 + 40, 40));
        return renderer;
    }

    // Below the text: green if the shader worked, magenta (fallback) as it fails
    private RectangleRenderer colorSquare(String shader, int index) {
        RectangleRenderer renderer = new RectangleRenderer(shader);
        renderer.setColorMode(new Color4f(0, 1, 0, 1));
        renderer.setSizePix(200, 200);
        renderer.setPosition(new Vector2f(index * 300 + 40, 470));
        return renderer;
    }

    @Override
    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        if (mainContext.getInputManager().inputPressed(ActionId.ENTER))
            engine().soundManager().createSoundSource(UNKNOWN_SOUND);

        unknownAnimation.update();
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        unknownTexture.display();
        brokenTexture.display();
        brokenShader.display();
        notCompilingShader.display();
        missingIncludeShader.display();
        unknownAnimation.display();
        unknownFont.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (unknownTexture != null) {
            unknownTexture.unload();
            brokenTexture.unload();
            brokenShader.unload();
            notCompilingShader.unload();
            missingIncludeShader.unload();
            unknownAnimation.unload();
            unknownFont.unload();
        }
    }

    @Override
    public void release() {
        resources.deleteResource(TextureData.class, BROKEN_TEXTURE);
        resources.deleteResource(ShaderData.class, BROKEN_SHADER);
        resources.deleteResource(ShaderData.class, NOT_COMPILING_SHADER);
        resources.deleteResource(ShaderData.class, MISSING_INCLUDE_SHADER);
    }
}
