package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.text.DrawnTexts;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import org.json.JSONArray;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import org.json.JSONObject;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class GLGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gl";
    private static final String CONTAINS = "contains";

    public GLGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Number of OpenGL objects alive by type, and elements shared by GLResources with their references");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Compare it before and after a scene change to find leaks", context -> describe(false))
                .then(literal("summary", "The shared elements given by type as elements/references")
                        .executes(context -> describe(true)))
                .then(literal("text", "The texts drawn by the last frame as text, from top to bottom then left to right")
                        .option(Option.of(CONTAINS, "Keep only the texts that contain this string")
                                .value("string", word()))
                        .executes("Check what is on screen without a screenshot", GLGenCom::drawnTexts));
    }

    private static CommandResult drawnTexts(CommandContext context) {
        String filter = context.has(CONTAINS) ? context.get(CONTAINS) : null;

        JSONArray texts = new JSONArray();
        StringBuilder message = new StringBuilder();
        for (DrawnTexts.Entry entry : DrawnTexts.last()) {
            if (filter != null && !entry.text().contains(filter))
                continue;

            JSONObject json = new JSONObject();
            json.put("text", entry.text());
            json.put("x", Math.round(entry.x() * 10) / 10.0);
            json.put("y", Math.round(entry.y() * 10) / 10.0);
            json.put("width", Math.round(entry.width() * 10) / 10.0);
            json.put("height", Math.round(entry.height() * 10) / 10.0);
            json.put("fontSize", entry.fontSize());
            texts.put(json);

            message.append("\n(").append(Math.round(entry.x())).append(", ").append(Math.round(entry.y())).append(") ")
                    .append(entry.text().replace("\n", "\\n"));
        }

        JSONObject data = new JSONObject();
        data.put("count", texts.length());
        data.put("texts", texts);
        return CommandResult.ok(texts.length() + " text(s) drawn by the last frame" + message, data);
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
