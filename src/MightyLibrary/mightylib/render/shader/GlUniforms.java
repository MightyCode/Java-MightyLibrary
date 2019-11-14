package MightyLibrary.mightylib.render.shader;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
public abstract class GlUniforms {
    public static void glUniform(Shader shader, String valueName, float value1){
        shader.use();
        glUniform1f(shader.getLink(valueName), value1);
    }

    public static void glUniform(Shader shader, String valueName, float value1, float value2){
        shader.use();
        glUniform2f(shader.getLink(valueName), value1, value2);
    }

    public static void glUniform(Shader shader, String valueName, float value1, float value2, float value3){
        shader.use();
        glUniform3f(shader.getLink(valueName), value1, value2, value3);
    }

    public static void glUniform(Shader shader, String valueName, float value1, float value2, float value3, float value4){
        shader.use();
        glUniform4f(shader.getLink(valueName), value1, value2, value3, value4);
    }

    public static void glUniform(Shader shader, String valueName, float[] value1){
        shader.use();
        glUniformMatrix4fv(shader.getLink(valueName), false, value1);
    }
    
    public static void glUniform(Shader shader, String valueName, FloatBuffer value1){
        shader.use();
        glUniformMatrix4fv(shader.getLink(valueName), false, value1);
    }
}
