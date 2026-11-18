package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.graphics.shader.IShaderService;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.oneOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.text;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The shader service: version written by the prelude, global defines, declared shaders; change the global defines
public final class ShadersGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "shaders";

    private final Supplier<IShaderService> service;

    public ShadersGenCom() {
        this(() -> Views.engine().shaders());
    }

    // The service read when the command runs (tests give their own)
    ShadersGenCom(Supplier<IShaderService> service) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The shaders: GLSL version, global defines, declared shaders; change the global defines");
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("The version written by the prelude, the global defines and the declared shaders",
                        context -> list())
                .then(literal("define", "Set a global define: every shader is read again")
                        .then(argument("name", suggest(() -> service.get().defines().keySet()),
                                "GLSL identifier (not starting with GL_)")
                                .executes("#define <name>, without value", context -> define(context.get("name"), ""))
                                .then(argument("value", text(), "Value, the rest of the line")
                                        .executes("#define <name> <value>",
                                                context -> define(context.get("name"), context.get("value"))))))
                .then(literal("undefine", "Remove a global define: every shader is read again")
                        .then(argument("name", oneOf(() -> service.get().defines().keySet()), "A global define")
                                .executes(context -> undefine(context.get("name")))));
    }

    private CommandResult list() {
        IShaderService shaders = service.get();
        String version = firstLine(shaders.prelude(Map.of()));
        Map<String, String> defines = shaders.defines();

        StringBuilder message = new StringBuilder(version).append(", ").append(defines.size())
                .append(" global define(s)");
        for (Map.Entry<String, String> define : defines.entrySet())
            message.append("\n  ").append(defineText(define.getKey(), define.getValue()));

        JSONObject declarations = shaders.declarations();
        JSONArray entries = new JSONArray();
        message.append("\n").append(declarations.length()).append(" declared shader(s)");
        for (String name : new TreeSet<>(declarations.keySet())) {
            JSONObject declaration = declarations.getJSONObject(name);
            JSONObject entry = new JSONObject();
            entry.put("name", name);
            entry.put("files", declaration.optJSONArray("files") == null ? new JSONArray()
                    : declaration.getJSONArray("files"));
            entry.put("tags", declaration.optJSONArray("tags") == null ? new JSONArray()
                    : declaration.getJSONArray("tags"));
            JSONObject entryDefines = declaration.optJSONObject("defines");
            entry.put("defines", entryDefines == null ? new JSONObject() : entryDefines);
            entries.put(entry);

            message.append("\n  ").append(name);
            if (entryDefines != null && entryDefines.length() > 0)
                message.append(" (defines: ").append(String.join(", ", new TreeSet<>(entryDefines.keySet())))
                        .append(")");
        }

        JSONObject data = new JSONObject();
        data.put("version", version);
        data.put("folder", shaders.folder());
        data.put("defines", new JSONObject(defines));
        data.put("shaders", entries);
        return CommandResult.ok(message.toString(), data);
    }

    private CommandResult define(String name, String value) {
        IShaderService shaders = service.get();
        String before = shaders.defines().get(name);
        try {
            shaders.setDefine(name, value);
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }

        JSONObject data = new JSONObject();
        data.put("name", name);
        data.put("value", value);
        data.put("changed", !value.equals(before));
        if (value.equals(before))
            return CommandResult.ok(defineText(name, value) + " already set", data);

        return CommandResult.ok(defineText(name, value) + ", the shaders are read again", data);
    }

    private CommandResult undefine(String name) {
        service.get().removeDefine(name);

        JSONObject data = new JSONObject();
        data.put("name", name);
        return CommandResult.ok(name + " removed, the shaders are read again", data);
    }

    private static String defineText(String name, String value) {
        return "#define " + name + (value.isEmpty() ? "" : " " + value);
    }

    private static String firstLine(String text) {
        int end = text.indexOf('\n');
        return end < 0 ? text : text.substring(0, end);
    }
}
