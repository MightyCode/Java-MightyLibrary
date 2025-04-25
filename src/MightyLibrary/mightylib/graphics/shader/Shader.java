package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.MultiSourceDataType;
import MightyLibrary.mightylib.utils.Logger;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.glUniform1ui;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

public class Shader extends MultiSourceDataType {
    private static class MissingLinkRuntimeException extends RuntimeException {
        public MissingLinkRuntimeException(String message) {
            super(message);
        }
    }

    private String vertexContent, fragmentContent;
    private String geometryContent;
    private boolean isUsingGeometryShader;

    private int shaderProgram;
    private final HashMap<String, ShaderValue> lastValues;

    private int vShader, fShader, gShader;

    private final boolean shader2D;

    public boolean isDimension2DShader(){
        return shader2D;
    }

    // Todo: move the shader opengl content to GLResources, load here only the texture content
    public Shader(String name, boolean shader2D, String ... paths){
        super(TYPE_SET_UP.MAIN_CONTEXT, name, paths);
        shaderProgram = -1;

        this.shader2D = shader2D;
        isUsingGeometryShader = false;

        lastValues = new HashMap<>();
        vShader = -1;
        fShader = -1;
        gShader = -1;
    }

    public void activateGeometryShader(){
        isUsingGeometryShader = true;
    }

    public void setShaderContent(int i, String shaderContent){
        if (i == 0)
            setVertexContent(shaderContent);
        else if (i == 1)
            setFragmentContent(shaderContent);
        else if (i == 2)
            setGeometryContent(shaderContent);

        sourceLoaded[i] = true;
    }

    private void setVertexContent(String vertexContent) {
        if (vShader != -1) {
            glDeleteShader(vShader);
            Logger.CheckOpenGLError("Delete vertex shader (id : " + vShader + ")");
        }

        this.vertexContent = vertexContent;

        // Creation du shader programme
        vShader = glCreateShader(GL_VERTEX_SHADER);
        Logger.CheckOpenGLError("Create vertex shader (id : " + vShader + ")");

        glShaderSource(vShader, vertexContent);
        Logger.CheckOpenGLError("Vertex Shader source (id : " + vShader + ")");

        glCompileShader(vShader);
        Logger.CheckOpenGLError("Vertex Shader compile (id : " + vShader + ")");

        Logger.CheckShaderError(vShader, "Vertex Shader (" + getDataName() + ") compile (id : " + vShader + ")");
    }

    private void setFragmentContent(String fragmentContent) {
        if (fShader != -1) {
            glDeleteShader(fShader);
            Logger.CheckOpenGLError("Delete fragment shader (id : " + fShader + ")");
        }

        this.fragmentContent = fragmentContent;

        fShader = glCreateShader(GL_FRAGMENT_SHADER);
        Logger.CheckOpenGLError("Create fragment shader (id : " + fShader + ")");

        glShaderSource(fShader, fragmentContent);
        Logger.CheckOpenGLError("Fragment Shader source (id : " + fShader + ")");

        glCompileShader(fShader);
        Logger.CheckOpenGLError("Fragment Shader compile (id : " + fShader + ")");

        Logger.CheckShaderError(fShader, "Fragment Shader (" + getDataName() + ") compile (id : " + fShader + ")");
    }

    private void setGeometryContent(String geometryContent){
        if (gShader != -1) {
            glDeleteShader(gShader);
            Logger.CheckOpenGLError("Delete geometry shader (id : " + gShader + ")");
        }

        this.geometryContent = geometryContent;

        if (isUsingGeometryShader) {
            gShader = glCreateShader(GL_GEOMETRY_SHADER);
            Logger.CheckOpenGLError("Create geometry shader (id : " + gShader + ")");

            glShaderSource(gShader, geometryContent);
            Logger.CheckOpenGLError("Geometry Shader source (id : " + gShader + ")");

            glCompileShader(gShader);
            Logger.CheckOpenGLError("Geometry Shader compile (id : " + gShader + ")");

            Logger.CheckShaderError(gShader, "Geometry Shader (" + getDataName() + ") compile (id : " + gShader + ")");
        }
    }

     @Override
    protected boolean internLoad() {
        if (vertexContent == null || fragmentContent == null) {
            System.err.println("No enough information to load the shader");
            return false;
        }

        shaderProgram = glCreateProgram();
        Logger.CheckOpenGLError("Create program shader (id : " + shaderProgram + ")");

        glAttachShader(shaderProgram, vShader);
        Logger.CheckOpenGLError("Attach vertex shader (id : " + vShader + ")");

        if (isUsingGeometryShader)
            glAttachShader(shaderProgram, gShader);

        Logger.CheckOpenGLError("Attach geometry shader (id : " + vShader + ")");

        glAttachShader(shaderProgram, fShader);
        Logger.CheckOpenGLError("Attach program shader (id : " + vShader + ")");

        glLinkProgram(shaderProgram);
        Logger.CheckOpenGLError("Link program shader (id : " + vShader + ")");
        Logger.CheckProgramError(shaderProgram, "Link program shader (id : " + vShader + ")");

        Logger.CheckOpenGLError("Ma bite");
        glDeleteShader(vShader);
        Logger.CheckOpenGLError("Delete vertex shader (id : " + vShader + ")");
        glDeleteShader(fShader);
        Logger.CheckOpenGLError("Delete fragment shader (id : " + vShader + ")");

        if (isUsingGeometryShader) {
            glDeleteShader(gShader);
            Logger.CheckOpenGLError("Delete geometry shader (id : " + vShader + ")");
        }

        vShader = -1;
        fShader = -1;
        gShader = -1;

        return true;
    }

    public void use() {
        use("Use program shader (id : " + shaderProgram + ")");
    }

    public void use(String message) {
        glUseProgram(shaderProgram);
        Logger.CheckOpenGLError(message);
    }

    public int getLink(String valueName){
        return glGetUniformLocation(shaderProgram, valueName);
    }

    public int getShaderId(){
        return shaderProgram;
    }

    public void sendValueToShader(ShaderValue value) {
        boolean added = false;
        if (!lastValues.containsKey(value.getName())) {
            lastValues.put(value.getName(), value.clone());
            added = true;
        }

        ShaderValue lastValue = lastValues.get(value.getName());

        if (!added && value.equals(lastValue) && !value.shouldForceUpdate())
            return;

        value.resetForceUpdate();

        lastValues.put(value.getName(), value.clone());

        this.use("Send value " + value.getName() + " to shader " + getDataName());
        int link = glGetUniformLocation(shaderProgram, value.getName());
        Logger.CheckOpenGLError("Get uniform location : " + value.getName());

        if (link == -1) {
            System.out.println("Program content :\n=======\n " + vertexContent
                    + "\n======\n" + fragmentContent + "\n=======\n");
            System.out.println("Following program link not found : " + value.getName() + " in shader " + getDataName()
                    + " (id : " + shaderProgram + ")");

            // stack trace :
            throw new MissingLinkRuntimeException(Logger.getCurrentStackTrace());
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
            glUniform1i(link, value.getObjectTyped(Color4f.class).toRGBA());
        } else if (value.getType() == Matrix4f.class) {
            glUniformMatrix4fv(link, false, MatrixToFloatBuffer(value.getObjectTyped(Matrix4f.class)));
        } else if (value.getType() == Integer.class || value.getType() == Boolean.class) {
            Integer v = value.getObjectTyped(Integer.class);
            glUniform1i(link, v);
        } else {
            System.err.println("Unknown shader value type : " + value.getType());
        }
    }

    private static FloatBuffer MatrixToFloatBuffer(Matrix4f matrix){
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

        matrix.get(buffer);

        return buffer;
    }

    public void internUnload() {
        System.out.println("Unloading shader : " + shaderProgram);
        if (shaderProgram != -1) {
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

        lastValues.clear();
    }
}
