package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The shader service of the engine: GLSL 330 core, shaders/shaders.json read once at its creation, merged by entry
 * through the sources (a higher source adds shaders or replaces those of the same name), global defines, batch
 * equivalents ("batch", "batchInstanced" fields, checked against the tags and the batch formats of their target).
 * No GL.
 */
public final class ShaderService implements IShaderService {
    public static final String LIST_FILE_NAME = "shaders.json";
    public static final String FOLDER = "shaders/";
    public static final String VERSION = "#version 330 core";

    private final JSONObject declarations;
    // Shader name to its batch equivalent ("batch" and "batchInstanced" fields), checked once: immutable
    private final Map<String, String> batchShaders;
    private final Map<String, String> batchInstancedShaders;
    // Batch program to its format (registry.shaders().batchFormat): immutable
    private final Map<String, BatchFormat> batchFormats;
    // Reads again every ShaderData (main thread)
    private final Runnable reloadShaders;

    // Written on the main thread, a new map each time; read by the workers (one snapshot per reading of a shader)
    private volatile SortedMap<String, String> defines = Collections.unmodifiableSortedMap(new TreeMap<>());

    /** The batch formats by batch program (Registrations.batchFormats): a batch link to a program without is dropped. */
    public ShaderService(Resources resources, Map<String, BatchFormat> batchFormats) {
        this(resources.sources(), () -> resources.reloadAllOfType(ShaderData.class), batchFormats);
    }

    // The sources where shaders.json is read; reloadShaders reads again every shader
    ShaderService(ResourceSources sources, Runnable reloadShaders, Map<String, BatchFormat> batchFormats) {
        Objects.requireNonNull(sources, "sources");
        this.batchFormats = Collections.unmodifiableMap(new TreeMap<>(batchFormats));
        this.reloadShaders = Objects.requireNonNull(reloadShaders, "reloadShaders");

        System.out.println("--Read the list of the shaders");
        JSONObject merged = FileMethods.mergeEntries(
                FileMethods.readJSONObjectLayers(sources, FOLDER + LIST_FILE_NAME), "shaders");
        JSONObject shaders = merged.optJSONObject("shaders");
        declarations = shaders == null ? new JSONObject() : shaders;
        batchShaders = batchLinks(declarations, "batch", BATCH_2D_TAG, this.batchFormats);
        batchInstancedShaders = batchLinks(declarations, "batchInstanced", BATCH_2D_INSTANCED_TAG, this.batchFormats);
    }

    /*
     * Shader name to the shader named by its field, when that one is declared with the tag and has a batch format; an
     * error line otherwise
     */
    private static Map<String, String> batchLinks(JSONObject declarations, String field, String tag,
                                                  Map<String, BatchFormat> formats) {
        Map<String, String> links = new TreeMap<>();
        for (String name : declarations.keySet()) {
            JSONObject entry = declarations.optJSONObject(name);
            if (entry == null || !entry.has(field))
                continue;

            Object value = entry.get(field);
            if (!(value instanceof String)) {
                System.err.println("--Shader " + name + ": \"" + field + "\" is not a shader name, drawn alone");
                continue;
            }

            String target = (String) value;
            JSONObject targetEntry = declarations.optJSONObject(target);
            if (targetEntry == null) {
                System.err.println("--Shader " + name + ": its \"" + field + "\" shader " + target
                        + " is not declared, drawn alone");
            } else if (!hasTag(targetEntry, tag)) {
                System.err.println("--Shader " + name + ": its \"" + field + "\" shader " + target
                        + " is not tagged " + tag + ", drawn alone");
            } else if (!formats.containsKey(target)) {
                System.err.println("--Shader " + name + ": its \"" + field + "\" shader " + target
                        + " has no batch format (registry.shaders().batchFormat), drawn alone");
            } else {
                links.put(name, target);
            }
        }

        return Collections.unmodifiableMap(links);
    }

    private static boolean hasTag(JSONObject entry, String tag) {
        JSONArray tags = entry.optJSONArray("tags");
        if (tags == null)
            return false;

        for (int i = 0; i < tags.length(); ++i) {
            if (tag.equals(tags.opt(i)))
                return true;
        }

        return false;
    }

    @Override
    public String batchShader(String shaderName) {
        return batchShaders.get(shaderName);
    }

    @Override
    public String batchInstancedShader(String shaderName) {
        return batchInstancedShaders.get(shaderName);
    }

    @Override
    public BatchFormat batchFormat(String batchShaderName) {
        return batchFormats.get(batchShaderName);
    }

    @Override
    public JSONObject declarations() {
        // A deep copy: read once by the loader, and by the command shaders
        return new JSONObject(declarations.toString());
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
        checkDefine(name, value);
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
