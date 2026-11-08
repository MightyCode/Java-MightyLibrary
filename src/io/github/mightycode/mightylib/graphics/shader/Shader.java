package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.MultiSourceDataType;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

public class Shader extends MultiSourceDataType {
    // Uniforms already reported missing, cleared when the program is deleted
    private Set<String> missingUniforms = new HashSet<>();

    private String vertexContent, fragmentContent;
    private String geometryContent;
    private boolean isUsingGeometryShader;

    // The program bound is a state of each OpenGL context (GLContext.currentProgram): the program itself is shared

    // Reused to send matrices without allocating a direct buffer for each uniform
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    private int shaderProgram;
    private final HashMap<String, ShaderValue> lastValues;
    // Uniform locations never change for a linked program
    private HashMap<String, Integer> uniformLocations;

    private int vShader, fShader, gShader;

    private final boolean shader2D;

    public boolean isDimension2DShader(){
        return shader2D;
    }

    public Shader(String name, boolean shader2D, String ... paths){
        super(TYPE_SET_UP.THREAD_PRELOAD_MAIN_LOAD, name, paths);
        shaderProgram = -1;

        this.shader2D = shader2D;
        isUsingGeometryShader = false;

        lastValues = new HashMap<>();
        uniformLocations = new HashMap<>();
        vShader = -1;
        fShader = -1;
        gShader = -1;
    }

    @Override
    protected DataType createReloadCopy() {
        if (getClass() != Shader.class || !validPaths())
            return null;

        String[] paths = new String[numberSource()];
        for (int i = 0; i < paths.length; ++i)
            paths[i] = getSourcePath(i);

        Shader copy = new Shader(dataName, shader2D, paths);
        copy.isUsingGeometryShader = isUsingGeometryShader;
        return copy;
    }

    @Override
    protected void swapContent(DataType copy) {
        Shader other = (Shader) copy;

        String otherVertex = other.vertexContent, otherFragment = other.fragmentContent, otherGeometry = other.geometryContent;
        other.vertexContent = vertexContent;
        other.fragmentContent = fragmentContent;
        other.geometryContent = geometryContent;
        vertexContent = otherVertex;
        fragmentContent = otherFragment;
        geometryContent = otherGeometry;

        int otherProgram = other.shaderProgram, otherV = other.vShader, otherF = other.fShader, otherG = other.gShader;
        other.shaderProgram = shaderProgram;
        other.vShader = vShader;
        other.fShader = fShader;
        other.gShader = gShader;
        shaderProgram = otherProgram;
        vShader = otherV;
        fShader = otherF;
        gShader = otherG;

        int otherStage = other.loadStage;
        other.loadStage = loadStage;
        loadStage = otherStage;

        HashMap<String, Integer> otherLocations = other.uniformLocations;
        other.uniformLocations = uniformLocations;
        uniformLocations = otherLocations;

        Set<String> otherMissing = other.missingUniforms;
        other.missingUniforms = missingUniforms;
        missingUniforms = otherMissing;

        // The new program has none of the values sent to the previous one (often sent once): sent again
        List<ShaderValue> sent = new ArrayList<>(lastValues.values());
        lastValues.clear();
        for (ShaderValue value : sent)
            sendValueToShader(value);
    }

    public void activateGeometryShader(){
        isUsingGeometryShader = true;
    }

    // Only keeps the source (worker thread): the GL objects are made by internLoad, on the main thread
    public void setShaderContent(int i, String shaderContent){
        if (i == 0)
            vertexContent = shaderContent;
        else if (i == 1)
            fragmentContent = shaderContent;
        else if (i == 2)
            geometryContent = shaderContent;

        sourceLoaded[i] = true;
    }

    // Stages of the load, one per internLoad (a step on the main thread each): compile each source, then link
    private static final int STAGE_VERTEX = 0, STAGE_FRAGMENT = 1, STAGE_GEOMETRY = 2, STAGE_LINK = 3, STAGE_DONE = 4;
    private int loadStage = STAGE_VERTEX;

    // Called before the first stage, for the shaders whose sources are generated by the code
    protected void prepareSources() {}

    // Called once the program is linked
    protected void afterLink() {}

    private int compile(int type, String name, String content) {
        int id = glCreateShader(type);
        Logger.CheckOpenGLError("Create " + name + " shader (id : " + id + ")");

        glShaderSource(id, content);
        Logger.CheckOpenGLError(name + " Shader source (id : " + id + ")");

        glCompileShader(id);
        Logger.CheckOpenGLError(name + " Shader compile (id : " + id + ")");

        try {
            Logger.CheckShaderError(id, name + " Shader (" + getDataName() + ") compile (id : " + id + ")");
        } catch (RuntimeException e) {
            glDeleteShader(id);
            throw e;
        }

        return id;
    }

    @Override
    protected boolean loadContinues() {
        return loadStage != STAGE_DONE;
    }

    /**
     * One stage of the load: the compilation of a source, then the link. Each is a step of its own, so a frame never
     * waits for a whole shader.
     */
    @Override
    protected boolean internLoad() {
        switch (loadStage) {
            case STAGE_VERTEX:
                prepareSources();
                if (vertexContent == null || fragmentContent == null || (isUsingGeometryShader && geometryContent == null)) {
                    System.err.println("No enough information to load the shader");
                    return false;
                }

                vShader = compile(GL_VERTEX_SHADER, "Vertex", vertexContent);
                loadStage = STAGE_FRAGMENT;
                return true;
            case STAGE_FRAGMENT:
                fShader = compile(GL_FRAGMENT_SHADER, "Fragment", fragmentContent);
                loadStage = isUsingGeometryShader ? STAGE_GEOMETRY : STAGE_LINK;
                return true;
            case STAGE_GEOMETRY:
                gShader = compile(GL_GEOMETRY_SHADER, "Geometry", geometryContent);
                loadStage = STAGE_LINK;
                return true;
            case STAGE_LINK:
                link();
                loadStage = STAGE_DONE;
                afterLink();
                return true;
            default:
                return true;
        }
    }

    private void link() {
        shaderProgram = glCreateProgram();
        Logger.CheckOpenGLError("Create program shader (id : " + shaderProgram + ")");

        glAttachShader(shaderProgram, vShader);
        if (isUsingGeometryShader)
            glAttachShader(shaderProgram, gShader);
        glAttachShader(shaderProgram, fShader);
        Logger.CheckOpenGLError("Attach shaders to program (id : " + shaderProgram + ")");

        glLinkProgram(shaderProgram);
        Logger.CheckOpenGLError("Link program shader (id : " + shaderProgram + ")");
        Logger.CheckProgramError(shaderProgram, "Link program shader (" + getDataName() + ", id : " + shaderProgram + ")");

        // Once linked, the program keeps what it needs
        glDeleteShader(vShader);
        glDeleteShader(fShader);
        if (isUsingGeometryShader)
            glDeleteShader(gShader);
        Logger.CheckOpenGLError("Delete shaders of program (id : " + shaderProgram + ")");

        vShader = -1;
        fShader = -1;
        gShader = -1;
    }

    public void use() {
        GLContext context = GLContext.current();
        if (context.currentProgram() == shaderProgram)
            return;

        glUseProgram(shaderProgram);
        context.setCurrentProgram(shaderProgram);
        EngineStats.countProgramBind();

        if (Logger.DO_CHECK)
            Logger.CheckOpenGLError("Use program shader (id : " + shaderProgram + ")");
    }

    public void use(String message) {
        glUseProgram(shaderProgram);
        GLContext.current().setCurrentProgram(shaderProgram);
        Logger.CheckOpenGLError(message);
    }

    public int getLink(String valueName){
        Integer location = uniformLocations.get(valueName);
        if (location == null) {
            location = glGetUniformLocation(shaderProgram, valueName);

            // Only a linked program gives definitive locations
            if (shaderProgram != -1)
                uniformLocations.put(valueName, location);
        }

        return location;
    }

    public int getShaderId(){
        return shaderProgram;
    }

    public void sendValueToShader(ShaderValue value) {
        ShaderValue lastValue = lastValues.get(value.getName());

        if (lastValue != null && value.equals(lastValue) && !value.shouldForceUpdate())
            return;

        value.resetForceUpdate();

        // Remember the sent value, in place when possible to avoid allocating
        if (lastValue == null || !lastValue.copyValueFrom(value))
            lastValues.put(value.getName(), value.clone());

        use();
        EngineStats.countUniformUpload();
        int link = getLink(value.getName());

        if (Logger.DO_CHECK)
            Logger.CheckOpenGLError("Get uniform location : " + value.getName());

        // Removed by the GLSL compiler when unused, or absent from the fallback shader: reported once, ignored
        if (link == -1) {
            if (missingUniforms.add(value.getName()))
                System.err.println("--Uniform " + value.getName() + " not found in shader " + getDataName()
                        + " (unused or absent), its values are ignored");
            return;
        }

        if (value.getType() == Float.class) {
            Float v = value.getObjectTyped(Float.class);
            glUniform1f(link, v);
        } else if (value.getType() == Vector2f.class) {
            Vector2f v = value.getObjectTyped(Vector2f.class);
            glUniform2f(link, v.x, v.y);
        } else if (value.getType() == Vector3f.class) {
            Vector3f v = value.getObjectTyped(Vector3f.class);
            glUniform3f(link, v.x, v.y, v.z);
        } else if (value.getType() == Vector4f.class) {
            Vector4f v = value.getObjectTyped(Vector4f.class);
            glUniform4f(link, v.x, v.y, v.z, v.w);
        } else if (value.getType() == Color4f.class) {
            glUniform1i(link, value.getObjectTyped(Color4f.class).toIntRGBA());
        } else if (value.getType() == Matrix4f.class) {
            value.getObjectTyped(Matrix4f.class).get(MATRIX_BUFFER);
            glUniformMatrix4fv(link, false, MATRIX_BUFFER);
        } else if (value.getType() == Integer.class || value.getType() == Boolean.class) {
            Integer v = value.getObjectTyped(Integer.class);
            glUniform1i(link, v);
        } else {
            System.err.println("Unknown shader value type : " + value.getType());
        }
    }

    public void internUnload() {
        System.out.println("Unloading shader : " + shaderProgram);
        if (shaderProgram != -1) {
            // The id can be reused by a new program, it must not be considered bound anymore
            GLContext.forgetProgram(shaderProgram);

            glDeleteProgram(shaderProgram);
            Logger.CheckOpenGLError("Delete program shader (id : " + shaderProgram + ")");
            shaderProgram = -1;
        }

        if (vShader != -1) {
            glDeleteShader(vShader);
            Logger.CheckOpenGLError("Delete vertex shader (id : " + vShader + ")");
            vShader = -1;
        }

        if (fShader != -1) {
            glDeleteShader(fShader);
            Logger.CheckOpenGLError("Delete fragment shader (id : " + fShader + ")");
            fShader = -1;
        }

        if (gShader != -1) {
            glDeleteShader(gShader);
            Logger.CheckOpenGLError("Delete geometry shader (id : " + gShader + ")");
            gShader = -1;
        }

        loadStage = STAGE_VERTEX;
        lastValues.clear();
        uniformLocations.clear();
        missingUniforms.clear();
    }

    // Failed to compile or link: delete what was created
    @Override
    protected void internCancelPreload() {
        internUnload();
    }
}
