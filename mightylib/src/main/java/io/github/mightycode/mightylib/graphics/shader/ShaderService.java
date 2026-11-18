package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The shader service of the engine: GLSL 330 core, shaders/shaders.json read once at its creation, merged by entry
 * through the sources (a higher source adds shaders or replaces those of the same name), global defines. No GL.
 */
public final class ShaderService implements IShaderService {
    public static final String LIST_FILE_NAME = "shaders.json";
    public static final String FOLDER = "shaders/";
    public static final String VERSION = "#version 330 core";

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final JSONObject declarations;
    // Reads again every ShaderData (main thread)
    private final Runnable reloadShaders;

    // Written on the main thread, a new map each time; read by the workers (one snapshot per reading of a shader)
    private volatile SortedMap<String, String> defines = Collections.unmodifiableSortedMap(new TreeMap<>());

    public ShaderService(Resources resources) {
        this(resources.sources(), () -> resources.reloadAllOfType(ShaderData.class));
    }

    // The sources where shaders.json is read; reloadShaders reads again every shader
    ShaderService(ResourceSources sources, Runnable reloadShaders) {
        Objects.requireNonNull(sources, "sources");
        this.reloadShaders = Objects.requireNonNull(reloadShaders, "reloadShaders");

        System.out.println("--Read the list of the shaders");
        JSONObject merged = FileMethods.mergeEntries(
                FileMethods.readJSONObjectLayers(sources, FOLDER + LIST_FILE_NAME), "shaders");
        JSONObject shaders = merged.optJSONObject("shaders");
        declarations = shaders == null ? new JSONObject() : shaders;
    }

    /**
     * The rule of the defines, global or of an entry: the name is a GLSL identifier not starting with GL_ (reserved),
     * the value is on one line (a value from a command or a mod can't slip a directive into the prelude).
     *
     * @throws IllegalArgumentException otherwise
     */
    public static void CheckDefine(String name, String value) {
        if (name == null || !IDENTIFIER.matcher(name).matches())
            throw new IllegalArgumentException("Invalid define name \"" + name + "\": expected a GLSL identifier");
        if (name.startsWith("GL_"))
            throw new IllegalArgumentException("Invalid define name " + name + ": GL_ is reserved by GLSL");
        if (value == null)
            throw new IllegalArgumentException("No value for the define " + name + " (\"\" for none)");
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)
            throw new IllegalArgumentException("The value of the define " + name + " has a line break");
    }

    @Override
    public JSONObject declarations() {
        return declarations;
    }

    @Override
    public String folder() {
        return FOLDER;
    }

    @Override
    public String prelude(Map<String, String> entryDefines) {
        // One read: the same snapshot for the whole prelude
        SortedMap<String, String> globals = defines;

        StringBuilder prelude = new StringBuilder(VERSION).append('\n');
        for (Map.Entry<String, String> define : globals.entrySet()) {
            if (!entryDefines.containsKey(define.getKey()))
                appendDefine(prelude, define.getKey(), define.getValue());
        }

        for (Map.Entry<String, String> define : new TreeMap<>(entryDefines).entrySet())
            appendDefine(prelude, define.getKey(), define.getValue());

        return prelude.toString();
    }

    private static void appendDefine(StringBuilder prelude, String name, String value) {
        prelude.append("#define ").append(name);
        if (!value.isEmpty())
            prelude.append(' ').append(value);
        prelude.append('\n');
    }

    @Override
    public void setDefine(String name, String value) {
        checkMainThread();
        CheckDefine(name, value);
        if (value.equals(defines.get(name)))
            return;

        TreeMap<String, String> changed = new TreeMap<>(defines);
        changed.put(name, value);
        publish(changed);
    }

    @Override
    public void removeDefine(String name) {
        checkMainThread();
        if (name == null || !defines.containsKey(name))
            return;

        TreeMap<String, String> changed = new TreeMap<>(defines);
        changed.remove(name);
        publish(changed);
    }

    private void publish(TreeMap<String, String> changed) {
        defines = Collections.unmodifiableSortedMap(changed);
        reloadShaders.run();
    }

    @Override
    public Map<String, String> defines() {
        return defines;
    }

    private static void checkMainThread() {
        if (!MainThread.isMainThread())
            throw new IllegalStateException("The defines of the shaders change on the main thread");
    }
}
