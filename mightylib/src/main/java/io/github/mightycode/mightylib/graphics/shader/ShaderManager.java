package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;

import java.util.Objects;


import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

/**
 * GLSL version of the driver and list of the shaders (shaders.json): the same for all the windows of the engine.
 * The cameras belong to the views (IView), not to this class.
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
    private String version;

    private JSONObject shadersInfo;

    public ShaderManager(ResourceSources sources) {
        this.sources = Objects.requireNonNull(sources, "sources");
        version = "330";
    }

    public void init() {
        init(-1);
    }

    // Needs a current GL context (the GLSL version of the driver)
    public void init(int version){
        System.out.println("--Load ShaderManager");

        String glslVersion = glGetString(GL_SHADING_LANGUAGE_VERSION);

        if (version == 140 || (glslVersion != null && glslVersion.contains("1.40"))){
            this.version = "1_40";
            System.out.println("--Version of shader GLSL 140");
        } else {
            this.version = "3_30";
            System.out.println("--Version of shader GLSL 330");
        }

        String shaderListPath = versionFolder() + LIST_SHADER_FILE_NAME;

        // Merged by entry: the shaders of a higher source are added, or replace those of the same name
        shadersInfo = FileMethods.mergeEntries(FileMethods.readJSONObjectLayers(sources, shaderListPath), "shaders");
    }

    public String getVersion(){
        return version;
    }

    // Logical folder of the shaders of the GLSL version, ending with '/'
    public String versionFolder() {
        return SHADERS_FOLDER + version + "/";
    }

    public JSONObject getShadersInfo() {
        return shadersInfo;
    }
}
