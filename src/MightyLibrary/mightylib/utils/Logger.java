package MightyLibrary.mightylib.utils;

import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL20.*;

public class Logger {
    public static boolean DO_CHECK = true;

    public static String getCurrentStackTrace() {
        return Thread.currentThread().getStackTrace()[2].toString();
    }

    public static void CheckOpenGLError(String message) {
        if (!DO_CHECK)
            return;

        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.out.println(message + " Error: " + error);

            throw new RuntimeException(getCurrentStackTrace());
        }
    }
    public static void CheckOpenGLError() {
        CheckOpenGLError("");
    }

    public static void CheckShaderError(int shader, String message) {
        if (!DO_CHECK)
            return;

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        Logger.CheckOpenGLError("Error checking shader status: \n\t " + message);

        if (status == GL_FALSE) {
            throw new RuntimeException(message + " Error: \n\t" + glGetShaderInfoLog(shader)
                            + "\n" + getCurrentStackTrace());
        }
    }

    public static void CheckProgramError(int program, String message) {
        if (!DO_CHECK)
            return;

        int status = glGetProgrami(program, GL_LINK_STATUS);
        Logger.CheckOpenGLError("Error checking program status: \n\t" + message);

        if (status == GL_FALSE) {
            throw new RuntimeException(message + " Error: \n\t" + glGetProgramInfoLog(program)
                    + "\n" + getCurrentStackTrace());
        }
    }
}
