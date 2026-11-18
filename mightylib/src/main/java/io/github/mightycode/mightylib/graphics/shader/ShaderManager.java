package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;

import java.util.Objects;

/**
 * List of the shaders (shaders.json) and prelude written before their sources: the same for all the windows of the
 * engine. The cameras belong to the views (IView), not to this class.
 */
public final class ShaderManager {
    public static final String LIST_SHADER_FILE_NAME = "shaders.json";

    public static final String GENERIC_PROJECTION_FIELD_NAME = "projection";
    public static final String GENERIC_VIEW_FIELD_NAME = "view";
    public static final String GENERIC_MODEL_FIELD_NAME = "model";
    public static final String GENERIC_COLOR_FIELD_NAME = "color";

    // Logical folder of the shaders, in the sources of the resources
    private static final String SHADERS_FOLDER = "shaders/";

    // Where shaders.json is read
    private final ResourceSources sources;

    private JSONObject shadersInfo;

    public ShaderManager(ResourceSources sources) {
        this.sources = Objects.requireNonNull(sources, "sources");
    }

    public void init() {
        System.out.println("--Load ShaderManager");

        // Merged by entry: the shaders of a higher source are added, or replace those of the same name
        shadersInfo = FileMethods.mergeEntries(
                FileMethods.readJSONObjectLayers(sources, folder() + LIST_SHADER_FILE_NAME), "shaders");
    }

    // Logical folder of the shaders, ending with '/'
    public String folder() {
        return SHADERS_FOLDER;
    }

    // Written before the source of every shader (the files have no #version)
    public String prelude() {
        return "#version 330 core\n";
    }

    public JSONObject getShadersInfo() {
        return shadersInfo;
    }
}
