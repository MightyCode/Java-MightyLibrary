package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.graphics.GLContext;
import org.json.JSONArray;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import org.json.JSONObject;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class GLGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gl";

    public GLGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Number of OpenGL objects alive by type, and elements shared by GLResources with their references");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Compare it before and after a scene change to find leaks", context -> describe(false))
                .then(literal("summary", "The shared elements given by type as elements/references")
                        .executes(context -> describe(true)));
    }

    private static CommandResult describe(boolean summary) {
        JSONObject data = new JSONObject();

        JSONObject glObjects = new JSONObject();
        for (GenDeleteResources.EGraphicResources type : GenDeleteResources.EGraphicResources.values())
            glObjects.put(type.name(), GenDeleteResources.Count(type));

        data.put("glObjects", glObjects);

        // What each OpenGL context owns (vertex arrays and framebuffers are not shared between windows)
        JSONArray glContexts = new JSONArray();
        for (GLContext context : GLContext.live()) {
            JSONObject json = new JSONObject();
            json.put("name", context.getName());
            json.put("VertexArrayBuffer", context.vertexArrays().size());
            json.put("FramebufferObject", context.framebuffers().size());
            json.put("RenderBufferObject", context.renderbuffers().size());
            json.put("pendingDeletions", context.pendingDeletions());
            glContexts.put(json);
        }
        data.put("glContexts", glContexts);
        JSONObject shared = Views.engine().glResources().describe();

        if (summary) {
            data.put("sharedElements", summarize(shared));
            return CommandResult.ok("OpenGL objects alive, shared elements and references by type", data);
        }

        data.put("sharedElements", shared);

        return CommandResult.ok("OpenGL objects alive and shared elements with their number of references", data);
    }

    // By type: "elements/references" instead of every element
    private static JSONObject summarize(JSONObject shared) {
        JSONObject summary = new JSONObject();

        for (String type : shared.keySet()) {
            JSONObject elements = shared.optJSONObject(type);
            if (elements == null) {
                summary.put(type, shared.get(type));
                continue;
            }

            int references = 0;
            for (String name : elements.keySet())
                references += elements.optInt(name);

            summary.put(type, elements.length() + "/" + references);
        }

        return summary;
    }
}
