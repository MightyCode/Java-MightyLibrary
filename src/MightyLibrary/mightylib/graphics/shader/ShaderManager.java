package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.scenes.Camera2D;
import MightyLibrary.mightylib.scenes.Camera3D;

import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

public class ShaderManager {
    private static ShaderManager singletonInstance = null;
    public static ShaderManager getInstance(){
        if (singletonInstance == null) singletonInstance = new ShaderManager();

        return singletonInstance;
    }
    public static final String GENERIC_PROJECTION_FIELD_NAME = "projection";
    public static final String GENERIC_VIEW_FIELD_NAME = "view";
    public static final String GENERIC_MODEL_FIELD_NAME = "model";
    public static final String GENERIC_COLOR_FIELD_NAME = "color";
    private String version;
    private Camera3D mainCamera3D;
    private Camera2D mainCamera2D;

    private ShaderManager() {;
        version = "330";

        mainCamera3D = null;
        mainCamera2D = null;
    }

    public void init() {
        init(-1);
    }

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
    }

    public String getVersion(){
        return version;
    }

    public void setCameras(Camera2D mainCamera2D, Camera3D mainCamera3D){
        this.mainCamera2D = mainCamera2D;
        this.mainCamera3D = mainCamera3D;
    }

    public void sendCameraToShader(Shader shader, boolean shouldSendProjection, boolean shouldSendView) {
        if (shader.isDimension2DShader()) {
            if (shouldSendProjection)
                shader.sendValueToShader(mainCamera2D.getProjection());

            if (shouldSendView)
                shader.sendValueToShader(mainCamera2D.getView());
        } else {
            if (shouldSendProjection)
                shader.sendValueToShader(mainCamera3D.getProjection());

            if (shouldSendView)
                shader.sendValueToShader(mainCamera3D.getView());
        }
    }

    public Camera2D getMainCamera2D(){ return mainCamera2D; }
    public Camera3D getMainCamera3D(){ return mainCamera3D; }
}
