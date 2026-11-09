package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import java.util.Collection;
import java.util.Collections;
import org.json.JSONObject;

import java.util.Arrays;

public class SceneGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "scene";
    public SceneGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME));
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        if (args.length != 2)
            return Collections.emptyList();

        return SceneClasses.Find(ListGeneralCommand.targetScenes().getScenesPackage());
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return currentScene();

        Class<?> sceneClass = findSceneClass(args[1]);
        if (sceneClass == null)
            return CommandResult.error("Unknown scene ->" + args[1] + "<-, give its class name relative to "
                    + ListGeneralCommand.targetScenes().getScenesPackage() + " or its full name");

        if (!Scene.class.isAssignableFrom(sceneClass))
            return CommandResult.error(sceneClass.getName() + " is not a scene");

        Scene scene;
        try {
            scene = (Scene) sceneClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return CommandResult.error("Can't create " + sceneClass.getName() + " : " + e);
        }

        String[] sceneArgs = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[]{""};
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

    @Override
    public String help() {
        return super.help() +
                "\nscene : name of the current scene and whether it is still loading" +
                "\nscene [name] [args...] : change the scene, name is the class name relative to the package of" +
                " the first scene (for example Platformer2D.PlatformerBox2D) or the full class name";
    }
}
