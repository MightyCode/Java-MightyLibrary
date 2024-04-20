package MightyLibrary.mightylib.utils;

import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL20.*;

public class Logger {
    public static void CheckOpenGLError(String message) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.out.println(message + " Error: " + error);

            System.exit(error);
        }
    }
    public static void CheckOpenGLError() {
        CheckOpenGLError("");
    }

    public static void CheckShaderError(int shader, String message) {
        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        Logger.CheckOpenGLError("Error checking shader status: \n\t " + message);

        if (status == GL_FALSE) {
            System.out.println(message + " Error: \n\t" + glGetShaderInfoLog(shader));
            Logger.CheckOpenGLError("Error checking shader info log: " + message);
            System.exit(1);
        }
    }

    public static void CheckProgramError(int program, String message) {
        int status = glGetProgrami(program, GL_LINK_STATUS);
        Logger.CheckOpenGLError("Error checking program status: \n\t" + message);

        if (status == GL_FALSE) {
            System.out.println(message + " Error: \n\t" + glGetProgramInfoLog(program));
            Logger.CheckOpenGLError("Error checking program info log: " + message);
            System.exit(1);
        }
    }
}
