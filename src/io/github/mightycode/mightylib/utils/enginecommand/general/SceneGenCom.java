package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONObject;

import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

public class SceneGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "scene";
    public SceneGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The current scene and whether it is still loading, or change the scene");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Name of the current scene and whether it is still loading", context -> currentScene())
                .then(argument("name", suggest(() -> SceneClasses.Find(ListGeneralCommand.targetScenes().getScenesPackage())),
                        "Class name relative to the package of the first scene (for example Platformer2D.PlatformerBox2D)"
                                + " or the full class name")
                        .executes("Change the scene", context -> changeScene(context.get("name"), new String[]{""}))
                        .then(argument("args", word(), "Arguments given to the scene").variadic()
                                .executes("Change the scene with arguments", context -> {
                                    List<String> sceneArgs = context.getList("args");
                                    return changeScene(context.get("name"), sceneArgs.toArray(new String[0]));
                                })));
    }

    private CommandResult changeScene(String name, String[] sceneArgs) {
        Class<?> sceneClass = findSceneClass(name);
        if (sceneClass == null)
            return CommandResult.error("Unknown scene ->" + name + "<-, give its class name relative to "
                    + ListGeneralCommand.targetScenes().getScenesPackage() + " or its full name");

        if (!Scene.class.isAssignableFrom(sceneClass))
            return CommandResult.error(sceneClass.getName() + " is not a scene");

        Scene scene;
        try {
            scene = (Scene) sceneClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return CommandResult.error("Can't create " + sceneClass.getName() + " : " + e);
        }

        ListGeneralCommand.targetScenes().requestSceneChange(scene, sceneArgs);

        return CommandResult.ok("Changing scene to " + sceneClass.getName()
                + ", use -> scene <- to know when it is loaded");
    }

    private CommandResult currentScene() {
        JSONObject data = new JSONObject();
        Scene scene = ListGeneralCommand.targetScenes().getCurrentScene();

        data.put("scene", scene == null ? JSONObject.NULL : scene.getClass().getName());
        data.put("loading", ListGeneralCommand.targetScenes().isCurrentSceneLoading());

        return CommandResult.ok("Current scene", data);
    }

    private Class<?> findSceneClass(String name) {
        String[] candidates = { ListGeneralCommand.targetScenes().getScenesPackage() + "." + name, name };

        for (String candidate : candidates) {
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException ignored) {
                // Try the next candidate
            }
        }

        return null;
    }
}
