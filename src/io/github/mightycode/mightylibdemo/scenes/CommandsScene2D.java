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
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.choice;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * A command of the scene: demo:spawn (level CHEAT) creates shapes in the scene (spawn count --color --size --at x y,
 * spawn clear, spawn list for the developer), so it runs under the scene (its view,
 * its OpenGL context). Added in init: it exists during the loading and answers "Scene still loading" until launch.
 * The scene argument "slow" keeps the loading screen 3 seconds, to leave the scene during its loading.
 * ESCAPE goes back to the menu, like the game command demo:menu.
 * Check with: scene CommandsScene2D, wait-loaded, spawn 3 --color red, resources, scene MenuScene, help.
 */
public class CommandsScene2D extends Scene {
    private static final List<String> SHAPES = Arrays.asList("square", "wide", "tall");
    private static final long SLOW_LOADING_MILLISECONDS = 3000;
    private static final int MAX_SPAWNED = 100;

    private final Random random = new Random();
    private Text text;
    private final List<Spawned> shapes = new ArrayList<>();

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
        return "Console (²): cheats on, then spawn 5 --color red (" + shapes.size() + " shapes), spawn --help | ESCAPE: menu";
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

    // A shape created by spawn, kept to be listed and removed
    private static final class Spawned {
        private final RectangleRenderer renderer;
        private final String shape;
        private final String color;
        private final Vector2f position;
        private final float width;
        private final float height;

        private Spawned(RectangleRenderer renderer, String shape, String color, Vector2f position, float width, float height) {
            this.renderer = renderer;
            this.shape = shape;
            this.color = color;
            this.position = position;
            this.width = width;
            this.height = height;
        }
    }

    // A shape at a random place (or at a given one, shifted by its rank): a renderer of the scene, unloaded with it
    private Spawned spawn(String shape, String colorName, float sizeFactor, Vector2f at, int rank) {
        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();
        float size = (40 + random.nextInt(40)) * sizeFactor;
        float width = shape.equals("wide") ? size * 2.5f : size;
        float height = shape.equals("tall") ? size * 2.5f : size;

        Color4f color;
        if (colorName == null)
            color = new Color4f(0.3f + random.nextFloat() * 0.7f, 0.3f + random.nextFloat() * 0.7f,
                    0.3f + random.nextFloat() * 0.7f, 1f);
        else if (colorName.equals("red"))
            color = new Color4f(1f, 0.25f, 0.25f, 1f);
        else if (colorName.equals("green"))
            color = new Color4f(0.25f, 1f, 0.25f, 1f);
        else
            color = new Color4f(0.3f, 0.4f, 1f, 1f);

        Vector2f position = at != null ? new Vector2f(at.x + rank * 12f, at.y + rank * 12f)
                : new Vector2f(width + random.nextFloat() * Math.max(1f, windowSize.x - 2 * width),
                        windowSize.y * 0.2f + random.nextFloat() * (windowSize.y * 0.7f));

        RectangleRenderer renderer = new RectangleRenderer("colorShape2D");
        renderer.setColorMode(color);
        renderer.setReferenceDirection(EDirection.None);
        renderer.setSizePix(width, height);
        renderer.setPosition(position);
        addDisplayable(renderer);

        return new Spawned(renderer, shape, colorName == null ? "random" : colorName, position, width, height);
    }

    private void clearSpawned() {
        for (Spawned shape : shapes) {
            removeDisplayable(shape.renderer);
            shape.renderer.unload();
        }

        shapes.clear();
        text.setText(describe());
    }

    /**
     * demo:spawn: the example of a command with a declared syntax: a typed argument with bounds, options with values,
     * a branch for the developer only, and a condition (the scene must be launched).
     */
    private class SpawnCommand extends BaseCommand {
        SpawnCommand() {
            super(Identifier.of("demo", "spawn"), ECommandLevel.CHEAT, "Create shapes in the scene");
        }

        @Override
        protected void syntax(CommandNode root) {
            // The renderers of the scene only exist from launch() on
            root.requires(CommandCondition.launched(CommandsScene2D.this))
                    .then(literal("clear", "Remove the shapes created").executes(context -> {
                        int count = shapes.size();
                        clearSpawned();
                        return CommandResult.ok(count + " shape(s) removed");
                    }))
                    .then(literal("list", "The shapes created with their color, position and size").level(ECommandLevel.DEV)
                            .executes(context -> {
                                JSONArray list = new JSONArray();
                                for (Spawned shape : shapes) {
                                    list.put(new JSONObject().put("shape", shape.shape).put("color", shape.color)
                                            .put("x", shape.position.x).put("y", shape.position.y)
                                            .put("width", shape.width).put("height", shape.height));
                                }
                                return CommandResult.ok(shapes.size() + " shape(s)", new JSONObject().put("shapes", list));
                            }))
                    .then(argument("count", integer(1, MAX_SPAWNED), "Number of shapes to create")
                            .option(Option.of("color", "Color of the shapes (random by default)")
                                    .value("color", choice("red", "green", "blue")))
                            .option(Option.of("size", "Size of the shapes, from half to four times the usual one")
                                    .value("factor", decimal(0.5, 4)).defaultValue(1.0))
                            .option(Option.of("at", "Where, in pixels of the window (random by default, each shape"
                                    + " shifted from the previous one)").value("x", decimal()).value("y", decimal()))
                            .option(Option.of("shape", "Proportions of the shapes")
                                    .value("shape", choice(SHAPES.toArray(new String[0]))).defaultValue("square"))
                            .executes("Create shapes in the scene", context -> {
                                int count = context.get("count");
                                float factor = context.<Double>get("size").floatValue();
                                String shape = context.get("shape");
                                String color = context.getOr("color", null);
                                Vector2f at = null;
                                if (context.has("at")) {
                                    List<Double> position = context.get("at");
                                    at = new Vector2f(position.get(0).floatValue(), position.get(1).floatValue());
                                }

                                for (int i = 0; i < count; ++i)
                                    shapes.add(spawn(shape, color, factor, at, i));
                                text.setText(describe());

                                return CommandResult.ok(count + " " + shape + " shape(s) appear",
                                        new JSONObject().put("created", count).put("shapes", shapes.size()));
                            }));
        }
    }
}
