package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneRegistry;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

// The current scene, or a change to a scene registered by a plugin (EngineRegistry.scenes)
public final class SceneGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "scene";
    public SceneGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The current scene and whether it is still loading, or change the scene");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Name of the current scene and whether it is still loading", context -> currentScene())
                .then(argument("name", suggest(() -> scenes().completions()),
                        "A registered scene: its identifier (demo:scene2d) or its path alone when no other scene has"
                                + " it (scene2d), case ignored")
                        .executes("Change the scene", context -> changeScene(context.get("name"), new String[]{""}))
                        .then(argument("args", word(), "Arguments given to the scene").variadic()
                                .executes("Change the scene with arguments", context -> {
                                    List<String> sceneArgs = context.getList("args");
                                    return changeScene(context.get("name"), sceneArgs.toArray(new String[0]));
                                })));
    }

    private static SceneRegistry scenes() {
        return Views.engine().scenes();
    }

    private CommandResult changeScene(String name, String[] sceneArgs) {
        List<SceneRegistry.Entry> matches = scenes().match(name);
        if (matches.isEmpty())
            return CommandResult.error("Unknown scene ->" + name + "<-: give the identifier or the path of a scene"
                    + " registered by a plugin (registry.scenes())");

        if (matches.size() > 1)
            return CommandResult.error("Ambiguous scene ->" + name + "<-, give its identifier: " + matches.stream()
                    .map(SceneRegistry.Entry::toString).collect(Collectors.joining(", ")));

        SceneRegistry.Entry entry = matches.get(0);

        // A scene belongs to the window of the view creating it: the targeted context, not the engine
        Scene scene;
        try (Views.Scope ignored = Views.enter(Views.engine().targetContext())) {
            scene = entry.create();
        } catch (RuntimeException e) {
            Logger.LogException("The scene " + entry.identifier() + " can't be created", e);
            return CommandResult.error("Can't create " + entry.identifier() + " : " + e);
        }

        ListGeneralCommand.targetScenes().requestSceneChange(scene, sceneArgs);

        JSONObject data = new JSONObject()
                .put("id", entry.identifier().toString())
                .put("scene", scene.getClass().getName());
        return CommandResult.ok("Changing scene to " + entry.identifier()
                + ", use -> scene <- to know when it is loaded", data);
    }

    // The identifier of the current scene when it is registered, else its class (created by code)
    private CommandResult currentScene() {
        JSONObject data = new JSONObject();
        Scene scene = ListGeneralCommand.targetScenes().getCurrentScene();
        SceneRegistry.Entry entry = scene == null ? null : scenes().entryOf(scene.getClass());

        data.put("scene", scene == null ? JSONObject.NULL : scene.getClass().getName());
        data.put("id", entry == null ? JSONObject.NULL : entry.identifier().toString());
        data.put("loading", ListGeneralCommand.targetScenes().isCurrentSceneLoading());

        String name = entry != null ? entry.identifier().toString()
                : scene != null ? scene.getClass().getName() : "none";
        return CommandResult.ok("Current scene " + name, data);
    }
}
