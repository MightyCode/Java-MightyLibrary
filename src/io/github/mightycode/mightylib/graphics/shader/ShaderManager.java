package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.FileMethods;
import org.json.JSONObject;


import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

/**
 * GLSL version of the driver and list of the shaders (shaders.json): the same for all the windows of the engine.
 * The cameras belong to the views (IView), not to this class.
 */
public class ShaderManager {
    public static final String SHADER_INFO_PATH = "resources/shaders/";
    public static final String LIST_SHADER_FILE_NAME = "shaders.json";

    public static final String GENERIC_PROJECTION_FIELD_NAME = "projection";
    public static final String GENERIC_VIEW_FIELD_NAME = "view";
    public static final String GENERIC_MODEL_FIELD_NAME = "model";
    public static final String GENERIC_COLOR_FIELD_NAME = "color";

    private String version;

    private JSONObject shadersInfo;

    public ShaderManager() {
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

        String shaderListPath = SHADER_INFO_PATH + getVersion() + "/" + ShaderManager.LIST_SHADER_FILE_NAME;

        shadersInfo = new JSONObject(FileMethods.readFileAsString(shaderListPath));
    }

    public String getVersion(){
        return version;
    }

    public JSONObject getShadersInfo() {
        return shadersInfo;
    }
}
