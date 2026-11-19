package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.IColorService;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONObject;

import java.util.Objects;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.choice;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The color space of the draws: of the engine (default) and of the current scene of the targeted window
public final class ColorSpaceGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "colorspace";
    public static final String TOGGLE = "toggle";

    private final Supplier<IColorService> service;
    // The view the command changes: the current scene of the targeted window, null while there is none
    private final Supplier<Scene> view;

    public ColorSpaceGenCom() {
        this(() -> Views.engine().colors(), () -> ListGeneralCommand.targetScenes().getCurrentScene());
    }

    // The service and the view read when the command runs (tests give their own)
    ColorSpaceGenCom(Supplier<IColorService> service, Supplier<Scene> view) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Color space of the draws (linear or gamma): of the engine and of the current scene");
        this.service = Objects.requireNonNull(service, "service");
        this.view = Objects.requireNonNull(view, "view");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("The modes of the engine and of the current scene, and the resolved one", context -> show())
                .then(literal("engine", "The default of the draws, for every view that inherits")
                        .then(argument("mode", choice("linear", "gamma"), "linear or gamma")
                                .executes(context -> setEngine(context.get("mode")))))
                .then(literal("view", "The mode of the current scene (inherit: the one of the engine)")
                        .requires(CommandCondition.window()).requires(CommandCondition.launched(this::currentView))
                        .then(argument("mode", choice("linear", "gamma", "inherit", TOGGLE),
                                "linear, gamma, inherit, or toggle (the other one than the resolved mode)")
                                .executes(context -> setView(context.get("mode")))));
    }

    private Scene currentView() {
        try {
            return view.get();
        } catch (RuntimeException e) {
            // No engine or no window: no view
            return null;
        }
    }

    private CommandResult show() {
        IColorService colors = service.get();
        Scene scene = currentView();
        JSONObject data = describe(colors, scene);

        String message = "Engine " + colors.colorSpace().lowerName();
        if (scene != null)
            message += ", scene " + scene.getClass().getSimpleName() + " " + scene.colorSpace().lowerName()
                    + " (draws " + colors.resolve(null, scene.colorSpace()).lowerName() + ")";
        return CommandResult.ok(message, data);
    }

    private CommandResult setEngine(String mode) {
        IColorService colors = service.get();
        colors.setColorSpace(ERenderColorSpace.parse(mode));
        return CommandResult.ok("Engine " + colors.colorSpace().lowerName(), describe(colors, currentView()));
    }

    private CommandResult setView(String mode) {
        IColorService colors = service.get();
        Scene scene = currentView();
        ERenderColorSpace chosen = TOGGLE.equals(mode) ? Toggled(colors.resolve(null, scene.colorSpace()))
                : ERenderColorSpace.parse(mode);
        scene.setColorSpace(chosen);

        return CommandResult.ok("Scene " + scene.getClass().getSimpleName() + " " + chosen.lowerName() + " (draws "
                + colors.resolve(null, chosen).lowerName() + ")", describe(colors, scene));
    }

    // The other mode than the resolved one
    static ERenderColorSpace Toggled(ERenderColorSpace resolved) {
        return resolved.isLinear() ? ERenderColorSpace.GAMMA : ERenderColorSpace.LINEAR;
    }

    private static JSONObject describe(IColorService colors, Scene scene) {
        JSONObject data = new JSONObject();
        data.put("engine", colors.colorSpace().lowerName());
        if (scene != null) {
            data.put("scene", scene.getClass().getName());
            data.put("view", scene.colorSpace().lowerName());
            data.put("resolved", colors.resolve(null, scene.colorSpace()).lowerName());
        } else {
            data.put("resolved", colors.colorSpace().lowerName());
        }
        return data;
    }
}
