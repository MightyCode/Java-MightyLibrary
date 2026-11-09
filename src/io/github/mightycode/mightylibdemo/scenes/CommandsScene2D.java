package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A command of the scene: demo:spawn (level CHEAT) creates a shape in the scene, so it runs under the scene (its view,
 * its OpenGL context). Added in init: it exists during the loading and answers "Scene still loading" until launch.
 * The argument "slow" keeps the loading screen 3 seconds, to leave the scene during its loading.
 * ESCAPE goes back to the menu, like the game command demo:menu.
 * Check with: scene CommandsScene2D, wait-loaded, demo:spawn square, resources, scene MenuScene, help.
 */
public class CommandsScene2D extends Scene {
    private static final List<String> SHAPES = Arrays.asList("square", "wide", "tall");
    private static final long SLOW_LOADING_MILLISECONDS = 3000;

    private final Random random = new Random();
    private Text text;
    private int spawned;

    @Override
    public void init(String[] args) {
        addCommand(new SpawnCommand());
    }

    @Override
    protected TemplateSceneLoading createLoadingScene(String[] args) {
        TemplateSceneLoading loading = super.createLoadingScene(args);
        if (loading != null && args != null && Arrays.asList(args).contains("slow")) {
            long end = System.currentTimeMillis() + SLOW_LOADING_MILLISECONDS;
            loading.addElementToLoad(() -> System.currentTimeMillis() >= end);
        }

        return loading;
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.12f, 0.12f, 0.16f);

        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();
        text = new Text();
        text.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setColor(ColorList.White())
                .setReference(EDirection.None)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.08f))
                .setFontSize(26)
                .setText(describe());
        addDisplayable(text);
    }

    private String describe() {
        return "Console (²): cheats on, then demo:spawn square|wide|tall (" + spawned + " shapes) | ESCAPE: menu";
    }

    @Override
    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();
        super.display();
        setAndDisplayRealScene();
    }

    // A shape at a random place: a renderer of the scene, unloaded with it
    private RectangleRenderer spawn(String shape) {
        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();
        float size = 40 + random.nextInt(40);
        float width = shape.equals("wide") ? size * 2.5f : size;
        float height = shape.equals("tall") ? size * 2.5f : size;

        RectangleRenderer renderer = new RectangleRenderer("colorShape2D");
        renderer.setColorMode(new Color4f(0.3f + random.nextFloat() * 0.7f, 0.3f + random.nextFloat() * 0.7f,
                0.3f + random.nextFloat() * 0.7f, 1f));
        renderer.setReferenceDirection(EDirection.None);
        renderer.setSizePix(width, height);
        renderer.setPosition(new Vector2f(width + random.nextFloat() * (windowSize.x - 2 * width),
                windowSize.y * 0.2f + random.nextFloat() * (windowSize.y * 0.7f)));
        addDisplayable(renderer);

        ++spawned;
        text.setText(describe());
        return renderer;
    }

    private class SpawnCommand extends BaseCommand {
        SpawnCommand() {
            super(Identifier.of("demo", "spawn"), ECommandLevel.CHEAT);
        }

        @Override
        public Collection<String> completeArgument(String[] args) {
            return args.length == 2 ? SHAPES : Collections.emptyList();
        }

        @Override
        public CommandResult execute(String[] args) {
            requireLaunched(CommandsScene2D.this);

            if (args.length < 2)
                return noEnoughArgumentError();
            String shape = args[1].toLowerCase();
            if (!SHAPES.contains(shape))
                return unknownArgumentError(args[1]);

            spawn(shape);
            return CommandResult.ok("A " + shape + " shape appears",
                    new JSONObject().put("shape", shape).put("shapes", spawned));
        }

        @Override
        public String help() {
            return super.help() + "\ndemo:spawn square|wide|tall : create a shape in the scene";
        }
    }
}
