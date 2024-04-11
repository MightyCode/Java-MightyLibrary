package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.utils.ObjectId;
import MightyLibrary.mightylib.utils.math.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

public class Shader extends ObjectId {
    public static String PATH = "resources/shaders/";

    private final String vertexSource, geometrySource, fragmentSource;
    private int shaderProgram;
    private final HashMap<String, ShaderValue> lastValues;

    private final boolean shader2D;
    public boolean isDimension2DShader(){
        return shader2D;
    }

    public Shader(String vertexSource, String geometrySource, String fragmentSource, boolean shader2D){
        // Base value
        shaderProgram = 0;

        this.fragmentSource = fragmentSource;
        this.geometrySource = geometrySource;
        this.vertexSource = vertexSource;
        this.shader2D = shader2D;

        lastValues = new HashMap<>();
    }

    public Shader(String vertexSource, String fragmentSource, boolean shader2D){
        // Base value
        shaderProgram = 0;

        this.fragmentSource = fragmentSource;
        this.geometrySource = null;
        this.vertexSource = vertexSource;
        this.shader2D = shader2D;

        lastValues = new HashMap<>();
    }

    public void load(){
        // Creation du shader programme
        int vShader = glCreateShader(GL_VERTEX_SHADER);
        String bf = "error";
        try {
            bf = FileMethods.readFileAsString(PATH + vertexSource);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.print("Error path :" + PATH + vertexSource + " not found");
        }

        glShaderSource(vShader, bf);
        glCompileShader(vShader);
        if(!glGetShaderInfoLog(vShader).equals("")){
            System.err.println("Shader path " + PATH + vertexSource);
            System.err.println("Error from Vertex:\n" + glGetShaderInfoLog(vShader));
        }


        int gShader = glCreateShader(GL_GEOMETRY_SHADER);
        if (geometrySource != null) {
            try {
                bf = FileMethods.readFileAsString(PATH + geometrySource);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.print("Error path :" + PATH + vertexSource + " not found");
            }

            glShaderSource(gShader, bf);
            glCompileShader(gShader);
            if (!glGetShaderInfoLog(gShader).equals("")) {
                System.err.println("Shader path " + PATH + vertexSource);
                System.err.println("Error from Vertex:\n" + glGetShaderInfoLog(gShader));
            }
        }

        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        try {
            bf = FileMethods.readFileAsString(PATH + fragmentSource);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.print("Error path :" + PATH + fragmentSource + " not found");
        }

        glShaderSource(fShader,bf);
        glCompileShader(fShader);
        if(!glGetShaderInfoLog(fShader).equals("")){
            System.err.println("Shader path " + PATH + fragmentSource);
            System.err.println("Error from Fragment:\n" + glGetShaderInfoLog(fShader));
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vShader);
        if (geometrySource != null)
            glAttachShader(shaderProgram, gShader);
        glAttachShader(shaderProgram, fShader);


        glLinkProgram(shaderProgram);
        if(!glGetShaderInfoLog(shaderProgram).equals(""))
            System.err.println("Error from Shader Program:\n" + glGetShaderInfoLog(shaderProgram));


        glDeleteShader(vShader);
        if (geometrySource != null)
            glDeleteShader(gShader);
        glDeleteShader(fShader);
    }

    public void use(){
        glUseProgram(shaderProgram);
    }

    public void addLink(String valueName) {
        lastValues.put(valueName, null);
    }

    public int getLink(String valueName){
        return 0;
    }

    public int getShaderId(){
        return shaderProgram;
    }

    public void unload(){
        glDeleteShader(shaderProgram);
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

        this.use();
        int link = glGetUniformLocation(shaderProgram, value.getName());

        if (value.getType() == Float.class) {
            Float v = value.getObjectTyped(Float.class);
            glUniform1f(link, v);
        } else if (value.getType() == Vector2f.class) {
            Vector2f v = value.getObjectTyped(Vector2f.class);
            glUniform2f(link, v.x, v.y);
        } else if (value.getType() == Vector3f.class) {
            Vector3f v = value.getObjectTyped(Vector3f.class);
            glUniform3f(link, v.x, v.y, v.z);
        } else if (
                value.getType() == Vector4f.class ||
                value.getType() == Color4f.class) {
            Vector4f v = value.getObjectTyped(Vector4f.class);
            glUniform4f(link, v.x, v.y, v.z, v.w);
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
}
