package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

/**
 * The shader service of the engine: GLSL 330 core, shaders/shaders.json read once at its creation, merged by entry
 * through the sources (a higher source adds shaders or replaces those of the same name). No GL.
 */
public final class ShaderService implements IShaderService {
    public static final String LIST_FILE_NAME = "shaders.json";
    public static final String FOLDER = "shaders/";
    public static final String VERSION = "#version 330 core";

    private final JSONObject declarations;
    // Reads again every ShaderData (main thread)
    private final Runnable reloadShaders;

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
        return VERSION + "\n";
    }
}
