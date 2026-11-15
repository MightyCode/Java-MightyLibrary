package io.github.mightycode.mightylibdemo.main;

import static org.lwjgl.glfw.GLFW.*;

import io.github.mightycode.mightylib.algorithms.wavefunctioncollapse.WaveCollapseRulesLoader;
import io.github.mightycode.mightylib.inputs.inputType.AxisBinding;
import io.github.mightycode.mightylib.inputs.inputType.AxisButtons;
import io.github.mightycode.mightylib.inputs.inputType.AxisGamepad;
import io.github.mightycode.mightylib.inputs.inputType.AxisMouse;
import io.github.mightycode.mightylib.inputs.inputType.AxisSum;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadAxis;
import io.github.mightycode.mightylib.inputs.inputType.InputGamepadButton;
import io.github.mightycode.mightylib.inputs.inputType.InputOr;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.resources.map.TileMapLoader;
import io.github.mightycode.mightylib.resources.map.TileSetLoader;
import io.github.mightycode.mightylib.resources.models.ModelLoader;
import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.resources.texture.TextureDataAtlasLoader;
import io.github.mightycode.mightylib.services.EngineServices;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylibdemo.scenes.DemoScenes;
import io.github.mightycode.mightylibdemo.scenes.MenuScene;
import org.json.JSONObject;

/**
 * What the demo adds to the engine, like a real game: the loaders it uses, its commands, its actions and axes with
 * their default bindings (in every window of the game, not in the detached console), its scenes (DemoScenes), a service
 * (TickCounterService, shown by EngineServices2D), and hooks on the life of the engine, counted and shown by the command
 * demo:lifecycle.
 */
public class DemoPlugin implements IPlugin {
    public static final String NAMESPACE = "demo";

    // Events seen by the hooks (main thread)
    private int started;
    private int sceneChanges;
    private String lastScene;
    private String previousScene;
    private int windowsOpened;
    private int windowsClosed;

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public void register(EngineRegistry registry) {
        // The optional loaders of the library the demo uses: all of them (LibraryLoaders.all() does the same)
        registry.resources()
                .add(new TileSetLoader())
                .add(new TileMapLoader())
                .add(new SoundLoader())
                .add(new ModelLoader())
                .add(new TextureDataAtlasLoader())
                .add(new WaveCollapseRulesLoader());

        // Commands of the game: kept for the whole run, whatever the scene
        registry.commands().register(new BaseCommand(registry.id("menu"), ECommandLevel.USER, "Go back to the menu") {
            @Override
            protected void syntax(CommandNode root) {
                root.executes(context -> {
                    Views.engine().sceneManager().requestSceneChange(new MenuScene(), new String[]{});
                    return CommandResult.ok("Back to the menu");
                });
            }
        });

        registerInputs(registry);
        DemoScenes.Register(registry);
        // Requires the sound, after the console when the game has one (it is then created without it otherwise)
        registry.services().register(TickCounterService.KEY, TickCounterService::new)
                .requires(EngineServices.SOUND)
                .after(EngineServices.CONSOLE);
        registerLifecycle(registry);
    }

    private void registerLifecycle(EngineRegistry registry) {
        registry.lifecycle()
                .onStarted(() -> ++started)
                .onSceneChanged((context, previous, next) -> {
                    ++sceneChanges;
                    previousScene = previous == null ? null : previous.getClass().getSimpleName();
                    lastScene = context.getName() + ": " + next.getClass().getSimpleName();
                })
                .onWindowOpened(window -> ++windowsOpened)
                .onWindowClosed(window -> ++windowsClosed)
                .onShutdown(() -> System.out.println("--Demo: shutdown after " + sceneChanges + " scene change(s)"));

        registry.commands().register(new BaseCommand(registry.id("lifecycle"), ECommandLevel.DEV,
                "The events of the life of the engine seen by the hooks of the demo") {
            @Override
            protected void syntax(CommandNode root) {
                root.executes(context -> {
                    JSONObject data = new JSONObject()
                            .put("started", started)
                            .put("sceneChanges", sceneChanges)
                            .put("lastScene", lastScene == null ? JSONObject.NULL : lastScene)
                            .put("previousScene", previousScene == null ? JSONObject.NULL : previousScene)
                            .put("windowsOpened", windowsOpened)
                            .put("windowsClosed", windowsClosed);
                    return CommandResult.ok("Started " + started + ", " + sceneChanges + " scene change(s), "
                            + windowsOpened + " window(s) opened, " + windowsClosed + " closed", data);
                });
            }
        });
    }

    private static void registerInputs(EngineRegistry registry) {
        registry.inputs()
                .action(ActionId.ESCAPE, new InputOr(
                        new InputSimple(GLFW_KEY_ESCAPE, EInputType.Keyboard),
                        new InputGamepadButton(GLFW_GAMEPAD_BUTTON_BACK)))

                // 3D moves : left stick, triggers to go up and down, left stick click to go faster
                .action(ActionId.MOVE_LEFT, new InputOr(
                        new InputSimple(GLFW_KEY_A, EInputType.Keyboard),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.NEGATIVE)))
                .action(ActionId.MOVE_RIGHT, new InputOr(
                        new InputSimple(GLFW_KEY_D, EInputType.Keyboard),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.POSITIVE)))
                .action(ActionId.MOVE_FORWARD, new InputOr(
                        new InputSimple(GLFW_KEY_W, EInputType.Keyboard),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.NEGATIVE)))
                .action(ActionId.MOVE_BACKWARD, new InputOr(
                        new InputSimple(GLFW_KEY_S, EInputType.Keyboard),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.POSITIVE)))
                .action(ActionId.MOVE_UP, new InputOr(
                        new InputSimple(GLFW_KEY_SPACE, EInputType.Keyboard),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, InputGamepadAxis.POSITIVE)))
                .action(ActionId.MOVE_DOWN, new InputOr(
                        new InputSimple(GLFW_KEY_LEFT_CONTROL, EInputType.Keyboard),
                        new InputGamepadAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, InputGamepadAxis.POSITIVE)))
                .action(ActionId.SHIFT, new InputOr(
                        new InputSimple(GLFW_KEY_LEFT_SHIFT, EInputType.Keyboard),
                        new InputGamepadButton(GLFW_GAMEPAD_BUTTON_LEFT_THUMB)))

                .action(ActionId.MOVE_LEFT_2D, new InputOr(
                        keys(GLFW_KEY_A, GLFW_KEY_LEFT),
                        gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_LEFT, GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.NEGATIVE)))
                .action(ActionId.MOVE_RIGHT_2D, new InputOr(
                        keys(GLFW_KEY_D, GLFW_KEY_RIGHT),
                        gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, GLFW_GAMEPAD_AXIS_LEFT_X, InputGamepadAxis.POSITIVE)))
                .action(ActionId.MOVE_UP_2D, new InputOr(
                        keys(GLFW_KEY_W, GLFW_KEY_UP),
                        gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.NEGATIVE)))
                .action(ActionId.MOVE_DOWN_2D, new InputOr(
                        keys(GLFW_KEY_S, GLFW_KEY_DOWN),
                        gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.POSITIVE)))

                .action(ActionId.TAB, new InputSimple(GLFW_KEY_TAB, EInputType.Keyboard))
                .action(ActionId.ENTER, new InputOr(
                        new InputSimple(GLFW_KEY_ENTER, EInputType.Keyboard),
                        new InputGamepadButton(GLFW_GAMEPAD_BUTTON_A)))

                .action(ActionId.SELECT_UP, new InputOr(
                        new InputSimple(GLFW_KEY_W, EInputType.Keyboard),
                        gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.NEGATIVE)))
                .action(ActionId.SELECT_DOWN, new InputOr(
                        new InputSimple(GLFW_KEY_S, EInputType.Keyboard),
                        gamepadDirection(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GLFW_GAMEPAD_AXIS_LEFT_Y, InputGamepadAxis.POSITIVE)))

                .action(ActionId.LEFT_CLICK, new InputSimple(GLFW_MOUSE_BUTTON_1, EInputType.Mouse))
                .action(ActionId.RIGHT_CLICK, new InputSimple(GLFW_MOUSE_BUTTON_2, EInputType.Mouse))

                // 3D moves from the actions above (keys or left stick and triggers, progressive) : no double counting
                .axis(AxisId.MOVE_RIGHT_3D, new AxisButtons(ActionId.MOVE_LEFT, ActionId.MOVE_RIGHT))
                .axis(AxisId.MOVE_FORWARD_3D, new AxisButtons(ActionId.MOVE_BACKWARD, ActionId.MOVE_FORWARD))
                .axis(AxisId.MOVE_UP_3D, new AxisButtons(ActionId.MOVE_DOWN, ActionId.MOVE_UP))

                // Degrees for this tick : the mouse in pixels and the right stick (pixels per second when fully pushed)
                // added, then multiplied by the same sensitivity. Up is positive : the y of both are inverted.
                .axis(AxisId.LOOK_RIGHT, new AxisSum(
                        new AxisMouse(AxisMouse.X),
                        lookStick(GLFW_GAMEPAD_AXIS_RIGHT_X)).scale(LOOK_SENSITIVITY))
                .axis(AxisId.LOOK_UP, new AxisSum(
                        new AxisMouse(AxisMouse.Y),
                        lookStick(GLFW_GAMEPAD_AXIS_RIGHT_Y)).scale(-LOOK_SENSITIVITY));
    }

    // Degrees per mouse pixel, not adjustable yet
    private static final float LOOK_SENSITIVITY = 0.05f;
    // A stick fully pushed turns like a mouse moving this number of pixels per second (150 degrees per second)
    private static final float LOOK_STICK_PIXELS_PER_SECOND = 3000;

    // Curve 2 : precise near the center, full speed fully pushed
    private static AxisBinding lookStick(int axis) {
        return new AxisGamepad(axis).curve(2).scale(LOOK_STICK_PIXELS_PER_SECOND).perSecond();
    }

    private static IAction keys(int first, int second) {
        return new InputOr(new InputSimple(first, EInputType.Keyboard), new InputSimple(second, EInputType.Keyboard));
    }

    // A direction of any gamepad : its d-pad button or its left stick pushed beyond the half
    private static IAction gamepadDirection(int dpadButton, int stickAxis, int direction) {
        return new InputOr(new InputGamepadButton(dpadButton), new InputGamepadAxis(stickAxis, direction));
    }
}
