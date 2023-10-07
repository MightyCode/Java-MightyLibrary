package MightyLibrary.mightylib.graphics.shader;

import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.util.ObjectId;
import MightyLibrary.mightylib.util.math.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

public class Shader extends ObjectId {
    public static String PATH = "resources/shaders/";

    private final String vertexSource, geometrySource, fragmentSource;
    private int shaderProgram;

    private final HashMap<String, Integer> valuesLink;
    private final HashMap<String, ShaderValue> lastValue;

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

        valuesLink = new HashMap<>();
        lastValue = new HashMap<>();
    }

    public Shader(String vertexSource, String fragmentSource, boolean shader2D){
        // Base value
        shaderProgram = 0;

        this.fragmentSource = fragmentSource;
        this.geometrySource = null;
        this.vertexSource = vertexSource;
        this.shader2D = shader2D;

        valuesLink = new HashMap<>();
        lastValue = new HashMap<>();
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

    public void addLink(String valueName){
        use();
        valuesLink.put(valueName, glGetUniformLocation(shaderProgram, valueName));
        lastValue.put(valueName, null);
    }

    public int getLink(String valueName){
        return valuesLink.getOrDefault(valueName, -1);
    }

    public int getShaderId(){
        return shaderProgram;
    }

    public void unload(){
        glDeleteShader(shaderProgram);
    }

    public void sendValueToShader(ShaderValue value) {
        if (value.equals(lastValue.get(value.getName()))
                && !value.shouldForceUpdate())
            return;

        System.out.println(getName() + " : Effectively send : " + value.getName());
        /*if (value.getName().equals("view")
                && lastValue.get(value.getName()) != null) {
            FloatBuffer b = lastValue.get(value.getName()).getObjectTyped(FloatBuffer.class).duplicate().slice();

            FloatBuffer a = value.getObjectTyped(FloatBuffer.class).duplicate().slice();

            while(b.hasRemaining())
                System.out.print(b.get() + " ");
            System.out.println();

            while(a.hasRemaining())
                System.out.print(a.get() + " ");
            System.out.println();

            System.out.println(b + " " + a);
            System.out.println(value.equals(lastValue.get(value.getName())));
        }*/

        value.resetForceUpdate();
        lastValue.put(value.getName(), value.clone());

        this.use();
        int link = this.getLink(value.getName());

        if (value.getType() == Float.class) {
            Float v = (Float) value.getObject();
            glUniform1f(link, v);
        } else if (value.getType() == Vector2f.class) {
            Vector2f v = (Vector2f) value.getObject();
            glUniform2f(link, v.x, v.y);
        } else if (value.getType() == Vector3f.class) {
            Vector3f v = (Vector3f) value.getObject();
            glUniform3f(link, v.x, v.y, v.z);
        } else if (
                value.getType() == Vector4f.class ||
                value.getType() == Color4f.class) {
            Vector4f v = (Vector4f) value.getObject();
            glUniform4f(link, v.x, v.y, v.z, v.w);
        } else if (value.getType() == FloatBuffer.class) {
            FloatBuffer v = (FloatBuffer) value.getObject();
            glUniformMatrix4fv(link, false, v);
        } else if (value.getType() == Integer.class) {
            Integer v = (Integer) value.getObject();
            glUniform1i(link, v);
        } else {
            System.err.println("Unknown shader value type : " + value.getType());
        }
    }
}
