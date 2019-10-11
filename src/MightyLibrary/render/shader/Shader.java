package MightyLibrary.render.shader;

import MightyLibrary.util.FileMethods;
import MightyLibrary.util.ObjectId;

import java.nio.FloatBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL20.*;

public class Shader extends ObjectId{
    private String fragmentSource, vertexSource;
    private int shaderProgram;
    private HashMap<String, Integer> valuesLink;
    private final String PATH = "resources/shaders/";

    public Shader(String vertexSource, String fragmentSource){
        // Base value
        shaderProgram = 0;

        this.fragmentSource = fragmentSource;
        this.vertexSource = vertexSource;
        valuesLink = new HashMap<>();
    }

    public Shader load(){
        // Creation du shader programme
        int vShader = glCreateShader(GL_VERTEX_SHADER);
        String bf = "error";
        try {
            bf = FileMethods.readFileAsString(PATH + vertexSource);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Error path :" + PATH + vertexSource + " not found");
        }

        glShaderSource(vShader, bf);
        glCompileShader(vShader);
        if(!glGetShaderInfoLog(vShader).equals("")){
            System.out.println("Shader path " + PATH + vertexSource);
            System.out.println("Error from Vertex:\n" + glGetShaderInfoLog(vShader));
        }

        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        try {
            bf = FileMethods.readFileAsString(PATH + fragmentSource);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Error path :" + PATH + fragmentSource + " not found");
        }

        glShaderSource(fShader,bf);
        glCompileShader(fShader);
        if(!glGetShaderInfoLog(fShader).equals("")){
            System.out.println("Shader path " + PATH + fragmentSource);
            System.out.println("Error from Fragment:\n" + glGetShaderInfoLog(fShader));
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram,vShader);
        glAttachShader(shaderProgram,fShader);
        glLinkProgram(shaderProgram);
        if(!glGetShaderInfoLog(shaderProgram).equals("")) System.out.println("Error from Shader Program:\n" + glGetShaderInfoLog(shaderProgram));

        glDeleteShader(vShader);
        glDeleteShader(fShader);

        return this;
    }

    public Shader use(){
        glUseProgram(shaderProgram);
        return this;
    }

    public Shader addLink(String valueName){
        valuesLink.put(valueName, glGetUniformLocation(shaderProgram, valueName));
        return this;
    }

    public int getLink(String valueName){
        return valuesLink.get(valueName);
    }

    public int getShaderId(){
        return shaderProgram;
    }

    public void unload(){
        glDeleteShader(shaderProgram);
    }


    public Shader glUniform(String valueName, float value1){
        this.use();
        glUniform1f(this.getLink(valueName), value1);
        return this;
    }

    public Shader glUniform(String valueName, float value1, float value2){
        this.use();
        glUniform2f(getLink(valueName), value1, value2);
        return this;
    }

    public Shader glUniform(String valueName, float value1, float value2, float value3){
        this.use();
        glUniform3f(getLink(valueName), value1, value2, value3);
        return this;
    }

    public Shader glUniform(String valueName, float value1, float value2, float value3, float value4){
        this.use();
        glUniform4f(getLink(valueName), value1, value2, value3, value4);
        return this;
    }

    public Shader glUniform(String valueName, float[] value1){
        this.use();
        glUniformMatrix4fv(getLink(valueName), false, value1);
        return this;
    }

    public Shader glUniform(String valueName, FloatBuffer value1){
        this.use();
        glUniformMatrix4fv(getLink(valueName), false, value1);
        return this;
    }
}
