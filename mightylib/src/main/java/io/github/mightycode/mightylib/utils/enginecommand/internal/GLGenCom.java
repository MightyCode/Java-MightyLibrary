package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.SharedObjects;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.text.DrawnTexts;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandCondition;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import org.json.JSONArray;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import org.json.JSONObject;

import java.util.List;
import java.util.function.ToIntFunction;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.word;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public final class GLGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gl";
    private static final String CONTAINS = "contains";

    public GLGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Number of OpenGL objects alive by type, elements shared by GLResources with their references,"
                        + " and the elements of the fallback data");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.requires(CommandCondition.window());
        root.executes("Compare it before and after a scene change to find leaks", context -> describe(false))
                .then(literal("summary", "The shared elements given by type as elements/references")
                        .executes(context -> describe(true)))
                .then(literal("materials", "The materials and meshes in use: shader, references, blend, sort key part, version")
                        .executes("See what is shared between renderers", GLGenCom::materials))
                .then(literal("text", "The texts drawn by the last frame as text, from top to bottom then left to right")
                        .option(Option.of(CONTAINS, "Keep only the texts that contain this string")
                                .value("string", word()))
                        .executes("Check what is on screen without a screenshot", GLGenCom::drawnTexts));
    }

    // The objects retained by at least one user: what a scene that is left must have given back
    private static int retained(List<?> objects, ToIntFunction<Object> references) {
        int count = 0;
        for (Object object : objects) {
            if (references.applyAsInt(object) > 0)
                ++count;
        }
        return count;
    }

    private static CommandResult materials(CommandContext context) {
        JSONArray materials = new JSONArray();
        int shared = 0;
        for (Material material : SharedObjects.MATERIALS.snapshot()) {
            if (material.references() <= 0)
                continue;

            if (material.references() > 1)
                ++shared;

            JSONObject json = new JSONObject();
            json.put("shader", material.getShader().getName());
            json.put("references", material.references());
            json.put("blend", material.getBlendMode().name());
            json.put("sortKeyPart", material.sortKeyPart());
            json.put("version", material.version());
            materials.put(json);
        }

        JSONArray meshes = new JSONArray();
        int sharedMeshes = 0;
        for (Mesh mesh : SharedObjects.MESHES.snapshot()) {
            if (mesh.references() <= 0)
                continue;

            if (mesh.references() > 1)
                ++sharedMeshes;

            JSONObject json = new JSONObject();
            json.put("references", mesh.references());
            json.put("vbos", mesh.getVboCount());
            json.put("vertices", mesh.getVertexCount());
            meshes.put(json);
        }

        JSONObject data = new JSONObject();
        data.put("materials", materials);
        data.put("meshes", meshes);
        data.put("sharedMaterials", shared);
        data.put("sharedMeshes", sharedMeshes);
        return CommandResult.ok(materials.length() + " material(s) alive, " + shared + " shared; "
                + meshes.length() + " mesh(es) alive, " + sharedMeshes + " shared", data);
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

        // Retained by at least one user: a scene that is left must have given them back
        JSONObject sharedObjects = new JSONObject();
        sharedObjects.put("Material", retained(SharedObjects.MATERIALS.snapshot(), o -> ((Material) o).references()));
        sharedObjects.put("Mesh", retained(SharedObjects.MESHES.snapshot(), o -> ((Mesh) o).references()));
        data.put("retainedObjects", sharedObjects);

        data.put("glContexts", describeContexts());
        JSONObject shared = Views.engine().glResources().describe();
        // Data type, element type, neededBeforeDrawing, state ("loaded", "waiting", "failed")
        JSONArray fallbacks = Views.engine().glResources().describeFallbacks();
        data.put("fallbacks", fallbacks);
        String fallbackMessage = describeFallbacks(fallbacks);

        if (summary) {
            data.put("sharedElements", summarize(shared));
            return CommandResult.ok("OpenGL objects alive, shared elements and references by type; " + fallbackMessage, data);
        }

        data.put("sharedElements", shared);

        return CommandResult.ok("OpenGL objects alive and shared elements with their number of references; "
                + fallbackMessage, data);
    }

    // Their number and the data types of those that failed
    // What each OpenGL context owns (vertex arrays and framebuffers are not shared between windows)
    private static JSONArray describeContexts() {
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
        return glContexts;
    }

    private static String describeFallbacks(JSONArray fallbacks) {
        StringBuilder failed = new StringBuilder();
        for (int i = 0; i < fallbacks.length(); ++i) {
            JSONObject fallback = fallbacks.getJSONObject(i);
            if (!"failed".equals(fallback.getString("state")))
                continue;

            if (failed.length() > 0)
                failed.append(", ");
            failed.append(fallback.getString("dataType"));
        }

        return fallbacks.length() + " fallback element(s)" + (failed.length() > 0 ? ", failed: " + failed : "");
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
