package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.CommandException;
import io.github.mightycode.mightylib.utils.enginecommand.JsonSelect;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.oneOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * Inspect and drive the cameras registered in the current scene (Scene.registerCamera), by id or name.
 */
public class CameraGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "camera";
    public CameraGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Inspect and drive the cameras of the current scene (id or name, main cameras : main2D, main3D)");
    }

    // The current scene of the targeted window, null while there is none
    private static Scene currentScene() {
        return ListGeneralCommand.targetScenes().getCurrentScene();
    }

    // Ids and names of the cameras of the scene, once launched: its cameras only exist from launch() on
    private static Collection<String> cameraReferences() {
        Scene scene = currentScene();
        List<String> references = new ArrayList<>();
        if (scene == null || !scene.isLaunched())
            return references;

        for (Camera camera : scene.getCameras()) {
            references.add(camera.getName());
            references.add(String.valueOf(camera.getCameraId()));
        }
        return references;
    }

    @Override
    protected void syntax(CommandNode root) {
        ArgumentType<String> cameras = oneOf(CameraGenCom::cameraReferences);

        root.requires(CommandCondition.launched(CameraGenCom::currentScene))
                .executes("Cameras of the current scene (id, name, position...)", context -> list(currentScene()))
                .then(argument("camera", cameras, "Id or name of a camera")
                        .executes("Details of a camera", context -> {
                            Camera camera = find(context.get("camera"));
                            return CommandResult.ok("Camera " + camera.getName(), describe(camera));
                        })
                        .then(literal("move", "Move to a position : x y for a 2D camera, x y z for a 3D camera")
                                .then(argument("x", decimal()).then(argument("y", decimal())
                                        .executes(context -> act(context, "move"))
                                        .then(argument("z", decimal()).executes(context -> act(context, "move"))))))
                        .then(literal("lookat", "3D camera, look at a point")
                                .then(argument("x", decimal()).then(argument("y", decimal())
                                        .then(argument("z", decimal()).executes(context -> act(context, "lookat"))))))
                        .then(literal("zoom", "2D camera, set the zoom (clamped to its limits) : level, or levelX levelY")
                                .then(argument("levelX", decimal())
                                        .executes(context -> act(context, "zoom"))
                                        .then(argument("levelY", decimal()).executes(context -> act(context, "zoom")))))
                        .then(literal("rotation", "2D camera, set the rotation")
                                .then(argument("degrees", decimal()).executes(context -> act(context, "rotation")))));
    }

    private static Camera find(String reference) {
        Scene scene = currentScene();
        Camera camera = scene == null ? null : scene.findCamera(reference);
        if (camera == null)
            throw new CommandException("Unknown camera ->" + reference + "<-, write -> camera <- to list them");

        return camera;
    }

    // The numbers given after the action, in the order of the line
    private static CommandResult act(CommandContext context, String action) {
        Camera camera = find(context.get("camera"));

        List<Float> numbers = new ArrayList<>();
        for (String name : new String[]{"x", "y", "z", "levelX", "levelY", "degrees"}) {
            if (context.has(name))
                numbers.add(context.<Double>get(name).floatValue());
        }
        float[] values = new float[numbers.size()];
        for (int i = 0; i < values.length; ++i)
            values[i] = numbers.get(i);

        if (camera instanceof Camera3D)
            return execute3D((Camera3D) camera, action, values);
        if (camera instanceof Camera2D)
            return execute2D((Camera2D) camera, action, values);

        return CommandResult.error("Unsupported camera type " + camera.getClass().getSimpleName());
    }

    private static CommandResult execute3D(Camera3D camera, String action, float[] values) {
        String[] changed;
        switch (action) {
            case "move":
                if (values.length != 3)
                    return CommandResult.error("Usage : camera [camera] move [x] [y] [z]");
                camera.setPos(new Vector3f(values[0], values[1], values[2]));
                changed = new String[]{ "position" };
                break;
            case "lookat":
                if (values.length != 3)
                    return CommandResult.error("Usage : camera [camera] lookat [x] [y] [z]");
                camera.lookAt(new Vector3f(values[0], values[1], values[2]));
                changed = new String[]{ "front", "yaw", "pitch" };
                break;
            default:
                return CommandResult.error("The camera " + camera.getName() + " is 3D : move, lookat");
        }

        // Only the changed fields: -> camera [camera] <- gives everything
        return CommandResult.ok("Camera " + camera.getName() + " updated", JsonSelect.Select(describe(camera), changed));
    }

    private static CommandResult execute2D(Camera2D camera, String action, float[] values) {
        String[] changed;
        switch (action) {
            case "move":
                if (values.length != 2)
                    return CommandResult.error("Usage : camera [camera] move [x] [y]");
                camera.setPos(new Vector2f(values[0], values[1]));
                changed = new String[]{ "position" };
                break;
            case "zoom":
                if (values.length == 1)
                    camera.setZoomLevel(values[0]);
                else if (values.length == 2)
                    camera.setZoomLevel(new Vector2f(values[0], values[1]));
                else
                    return CommandResult.error("Usage : camera [camera] zoom [level] or zoom [levelX] [levelY]");
                // The zoom keeps its reference point: the position changes too
                changed = new String[]{ "zoom", "position" };
                break;
            case "rotation":
                if (values.length != 1)
                    return CommandResult.error("Usage : camera [camera] rotation [degrees]");
                camera.setRotation((float) Math.toRadians(values[0]));
                changed = new String[]{ "rotationDegrees" };
                break;
            default:
                return CommandResult.error("The camera " + camera.getName() + " is 2D : move, zoom, rotation");
        }

        // Only the changed fields: -> camera [camera] <- gives everything
        return CommandResult.ok("Camera " + camera.getName() + " updated", JsonSelect.Select(describe(camera), changed));
    }

    private static CommandResult list(Scene scene) {
        JSONArray cameras = new JSONArray();
        for (Camera camera : scene.getCameras())
            cameras.put(describe(camera));

        JSONObject data = new JSONObject();
        data.put("cameras", cameras);

        return CommandResult.ok("Cameras of the current scene", data);
    }

    private static JSONObject describe(Camera camera) {
        JSONObject json = new JSONObject();
        json.put("id", camera.getCameraId());
        json.put("name", camera.getName());

        if (camera instanceof Camera3D) {
            Camera3D camera3D = (Camera3D) camera;
            json.put("type", "3D");
            json.put("position", toArray(camera3D.getCamPosRef().x, camera3D.getCamPosRef().y, camera3D.getCamPosRef().z));
            Vector3f front = camera3D.getLookAtVector();
            json.put("front", toArray(front.x, front.y, front.z));
            json.put("yaw", round(camera3D.getYaw()));
            json.put("pitch", round(camera3D.getPitch()));
            json.put("fov", round(camera3D.getViewAngle()));
        } else if (camera instanceof Camera2D) {
            Camera2D camera2D = (Camera2D) camera;
            json.put("type", "2D");
            json.put("position", toArray(camera2D.getCamPosRef().x, camera2D.getCamPosRef().y));
            json.put("zoom", toArray(camera2D.getZoomLevel().x, camera2D.getZoomLevel().y));
            json.put("rotationDegrees", round((float) Math.toDegrees(camera2D.getRotation())));
        }

        return json;
    }

    private static JSONArray toArray(float... values) {
        JSONArray array = new JSONArray();
        for (float value : values)
            array.put(round(value));

        return array;
    }

    private static double round(float value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
