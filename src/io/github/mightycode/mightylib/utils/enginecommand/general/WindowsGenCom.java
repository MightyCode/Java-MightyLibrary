package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.GLFW_FOCUSED;
import static org.lwjgl.glfw.GLFW.glfwGetWindowAttrib;

/**
 * The contexts and their windows: parent, modal, blocked by a modal window, focus, size, OpenGL objects owned by
 * the OpenGL context of each window. The window that the other commands work on is marked "target".
 */
public class WindowsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "windows";

    public WindowsGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The contexts (a scene and its windows) and their windows: parent, modal, blocked, focus, display, sizes and OpenGL objects");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("The window marked target is the one that input, screenshot, monitors, scene and camera work on"
                + " (see the window command)", context -> describe());
    }

    private static CommandResult describe() {
        Engine engine = Views.engine();
        WindowContext target = engine.commandWindow();

        JSONArray contexts = new JSONArray();
        int windowCount = 0;
        for (Context context : engine.contexts().getContexts()) {
            JSONArray windows = new JSONArray();
            for (WindowContext window : context.getWindows()) {
                windows.put(describe(window, target));
                ++windowCount;
            }

            JSONObject json = new JSONObject();
            json.put("name", context.getName());
            json.put("main", context.isMain());
            json.put("closing", context.isCloseRequested());
            json.put("scene", context.getSceneManager() == null || context.getSceneManager().getCurrentScene() == null
                    ? JSONObject.NULL : context.getSceneManager().getCurrentScene().getClass().getName());
            json.put("windows", windows);
            contexts.put(json);
        }

        JSONObject data = new JSONObject();
        data.put("target", target.getId());
        data.put("contexts", contexts);

        return CommandResult.ok(engine.contexts().getContexts().size() + " context(s), " + windowCount + " window(s)", data);
    }

    private static JSONObject describe(WindowContext window, WindowContext target) {
        JSONObject json = new JSONObject();
        json.put("id", window.getId());
        json.put("title", window.getName());
        json.put("main", window.isMain());
        json.put("parent", window.getParent() == null ? JSONObject.NULL : window.getParent().getId());
        json.put("modal", window.isModal());
        json.put("blocked", window.isBlocked());
        json.put("closesEngine", window.closesEngine());
        json.put("target", window == target);

        long glfwId = window.getWindow().getInfo().getWindowId();
        json.put("focused", glfwId != 0 && glfwGetWindowAttrib(glfwId, GLFW_FOCUSED) == 1);
        json.put("mode", window.getWindow().getInfo().getWindowMode().name().toLowerCase(Locale.ROOT));
        json.put("display", window.getDisplay().toString());
        json.put("size", window.getWindow().getInfo().getSizeRef().x + "x" + window.getWindow().getInfo().getSizeRef().y);
        json.put("virtualSize", window.getWindow().getInfo().getVirtualSizeRef().x + "x"
                + window.getWindow().getInfo().getVirtualSizeRef().y);

        GLContext gl = window.getWindow().getGLContext();
        if (gl != null) {
            JSONObject objects = new JSONObject();
            objects.put("vertexArrays", gl.vertexArrays().size());
            objects.put("framebuffers", gl.framebuffers().size());
            objects.put("renderbuffers", gl.renderbuffers().size());
            objects.put("pendingDeletions", gl.pendingDeletions());
            json.put("glObjects", objects);
        }

        return json;
    }
}
