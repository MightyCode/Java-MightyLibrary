package io.github.mightycode.mightylib.utils;

import static org.lwjgl.opengl.GL20.*;

public class Logger {
    // glGetError after the sensitive GL calls, GL objects created on the main thread only (EngineConfig.setOpenGLErrorCheck).
    // Read from any thread (GenDeleteResources checks the calling thread)
    private static volatile boolean glErrorCheck = true;

    public static boolean isGLErrorCheck() {
        return glErrorCheck;
    }

    public static void setGLErrorCheck(boolean glErrorCheck) {
        Logger.glErrorCheck = glErrorCheck;
    }

    public static String getCurrentStackTrace() {
        return Thread.currentThread().getStackTrace()[2].toString();
    }

    /**
     * Unexpected exception that is handled (the engine goes on): one "--" line saying what failed, then the stack trace.
     * Expected failures (wrong data, missing file reported to the user) print one line without stack trace instead.
     */
    public static void LogException(String message, Throwable error) {
        System.err.println("--" + message + ": " + error);
        error.printStackTrace(System.err);
    }

    public static void CheckOpenGLError(String message) {
        if (!glErrorCheck)
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

    // Shader compile and link status are always checked: only done at loading and a failure must never go unnoticed
    public static void CheckShaderError(int shader, String message) {
        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        Logger.CheckOpenGLError("Error checking shader status: \n\t " + message);

        if (status == GL_FALSE) {
            throw new RuntimeException(message + " Error: \n\t" + glGetShaderInfoLog(shader)
                            + "\n" + getCurrentStackTrace());
        }
    }

    public static void CheckProgramError(int program, String message) {
        int status = glGetProgrami(program, GL_LINK_STATUS);
        Logger.CheckOpenGLError("Error checking program status: \n\t" + message);

        if (status == GL_FALSE) {
            throw new RuntimeException(message + " Error: \n\t" + glGetProgramInfoLog(program)
                    + "\n" + getCurrentStackTrace());
        }
    }
}
