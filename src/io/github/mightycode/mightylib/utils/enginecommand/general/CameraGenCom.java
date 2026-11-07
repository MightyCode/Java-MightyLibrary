package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.SceneManager;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.JsonSelect;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Inspect and drive the cameras registered in the current scene (Scene.registerCamera), by id or name.
 */
public class CameraGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "camera";
    private final SceneManager sceneManager;

    public CameraGenCom(SceneManager sceneManager) {
        super(COMMAND_NAME, GENERAL_COMMAND);
        this.sceneManager = sceneManager;
    }

    @Override
    public CommandResult execute(String[] args) {
        if (checkArgumentHelp(args))
            return CommandResult.ok(help());

        Scene scene = sceneManager.getCurrentScene();
        if (scene == null)
            return CommandResult.error("No current scene");

        if (args.length < 2)
            return list(scene);

        Camera camera = scene.findCamera(args[1]);
        if (camera == null)
            return CommandResult.error("Unknown camera ->" + args[1] + "<-, write -> camera <- to list them");

        if (args.length < 3)
            return CommandResult.ok("Camera " + camera.getName(), describe(camera));

        try {
            float[] values = parseValues(args);

            if (camera instanceof Camera3D)
                return execute3D((Camera3D) camera, args[2], values);
            if (camera instanceof Camera2D)
                return execute2D((Camera2D) camera, args[2], values);
        } catch (NumberFormatException e) {
            return CommandResult.error("Invalid number : " + e.getMessage());
        }

        return CommandResult.error("Unsupported camera type " + camera.getClass().getSimpleName());
    }

    private CommandResult execute3D(Camera3D camera, String action, float[] values) {
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
                return unknownArgumentError(action + " (3D camera : move, lookat)");
        }

        // Only the changed fields: -> camera [camera] <- gives everything
        return CommandResult.ok("Camera " + camera.getName() + " updated", JsonSelect.Select(describe(camera), changed));
    }

    private CommandResult execute2D(Camera2D camera, String action, float[] values) {
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
                return unknownArgumentError(action + " (2D camera : move, zoom, rotation)");
        }

        // Only the changed fields: -> camera [camera] <- gives everything
        return CommandResult.ok("Camera " + camera.getName() + " updated", JsonSelect.Select(describe(camera), changed));
    }

    // Numbers after the action: camera [camera] [action] [values...]
    private static float[] parseValues(String[] args) {
        float[] values = new float[args.length - 3];
        for (int i = 0; i < values.length; ++i)
            values[i] = Float.parseFloat(args[i + 3]);

        return values;
    }

    private CommandResult list(Scene scene) {
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

    @Override
    public String help() {
        return super.help() +
                "\ncamera : cameras of the current scene (id, name, position...)" +
                "\ncamera [id or name] : details of a camera (main cameras : main2D, main3D)" +
                "\ncamera [camera] move [x] [y] [z] : 3D camera, move to a position" +
                "\ncamera [camera] lookat [x] [y] [z] : 3D camera, look at a point" +
                "\ncamera [camera] move [x] [y] : 2D camera, move to a position" +
                "\ncamera [camera] zoom [level] / zoom [levelX] [levelY] : 2D camera, set the zoom (clamped to its limits)" +
                "\ncamera [camera] rotation [degrees] : 2D camera, set the rotation";
    }
}
