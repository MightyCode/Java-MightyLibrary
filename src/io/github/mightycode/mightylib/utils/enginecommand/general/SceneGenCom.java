package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

import java.util.Arrays;

public class SceneGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "scene";
    private final SceneManager sceneManager;

    public SceneGenCom(SceneManager sceneManager) {
        super(COMMAND_NAME);
        this.sceneManager = sceneManager;
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return currentScene();

        Class<?> sceneClass = findSceneClass(args[1]);
        if (sceneClass == null)
            return CommandResult.error("Unknown scene ->" + args[1] + "<-, give its class name relative to "
                    + sceneManager.commandTarget().getScenesPackage() + " or its full name");

        if (!Scene.class.isAssignableFrom(sceneClass))
            return CommandResult.error(sceneClass.getName() + " is not a scene");

        Scene scene;
        try {
            scene = (Scene) sceneClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return CommandResult.error("Can't create " + sceneClass.getName() + " : " + e);
        }

        String[] sceneArgs = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[]{""};
        sceneManager.commandTarget().requestSceneChange(scene, sceneArgs);

        return CommandResult.ok("Changing scene to " + sceneClass.getName()
                + ", use -> scene <- to know when it is loaded");
    }

    private CommandResult currentScene() {
        JSONObject data = new JSONObject();
        Scene scene = sceneManager.commandTarget().getCurrentScene();

        data.put("scene", scene == null ? JSONObject.NULL : scene.getClass().getName());
        data.put("loading", sceneManager.commandTarget().isCurrentSceneLoading());

        return CommandResult.ok("Current scene", data);
    }

    private Class<?> findSceneClass(String name) {
        String[] candidates = { sceneManager.commandTarget().getScenesPackage() + "." + name, name };

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
                " the first scene (for example Platformer2D.Test2DPlatformerBox2D) or the full class name";
    }
}
