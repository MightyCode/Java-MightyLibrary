package io.github.mightycode.mightylib.main;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The windowing system GLFW runs on, and what it allows. Wayland lets the compositor place the windows: a program
 * can neither read nor set their position, nor make one float above the others, nor give it an icon (GLFW reports
 * GLFW_FEATURE_UNAVAILABLE). The engine asks here before such calls instead of letting them fail.
 * The system property PROPERTY (-Dmightylib.glfw.platform=x11|wayland|win32|cocoa|null) chooses the platform,
 * otherwise GLFW takes the best one (Wayland in a Wayland session).
 */
public final class WindowPlatform {
    public static final String PROPERTY = "mightylib.glfw.platform";

    private static int platform = GLFW_ANY_PLATFORM;

    private WindowPlatform() {}

    // Before glfwInit: the platform asked by the system property, if any and supported
    static void applyInitHint() {
        String wanted = System.getProperty(PROPERTY);
        if (wanted == null || wanted.isEmpty())
            return;

        int hint = Parse(wanted);
        if (hint == GLFW_ANY_PLATFORM) {
            System.err.println("--Unknown " + PROPERTY + " \"" + wanted + "\": GLFW chooses the platform");
            return;
        }
        if (!glfwPlatformSupported(hint)) {
            System.err.println("--GLFW platform \"" + wanted + "\" not supported here: GLFW chooses the platform");
            return;
        }

        glfwInitHint(GLFW_PLATFORM, hint);
    }

    // After glfwInit
    static void readPlatform() {
        platform = glfwGetPlatform();
        System.out.println("--GLFW platform : " + Name());
    }

    static int Parse(String name) {
        switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "x11": return GLFW_PLATFORM_X11;
            case "wayland": return GLFW_PLATFORM_WAYLAND;
            case "win32": return GLFW_PLATFORM_WIN32;
            case "cocoa": return GLFW_PLATFORM_COCOA;
            case "null": return GLFW_PLATFORM_NULL;
            default: return GLFW_ANY_PLATFORM;
        }
    }

    public static String Name() {
        switch (platform) {
            case GLFW_PLATFORM_X11: return "X11";
            case GLFW_PLATFORM_WAYLAND: return "Wayland";
            case GLFW_PLATFORM_WIN32: return "Win32";
            case GLFW_PLATFORM_COCOA: return "Cocoa";
            case GLFW_PLATFORM_NULL: return "Null";
            default: return "unknown";
        }
    }

    public static boolean IsWayland() { return platform == GLFW_PLATFORM_WAYLAND; }

    // Window position readable and settable (centering, coming back to a place, screen of a window)
    public static boolean CanPlaceWindows() { return !IsWayland(); }

    // A window kept above the others (dialogs above their parent outside Windows)
    public static boolean CanFloatWindows() { return !IsWayland(); }

    public static boolean CanSetWindowIcon() { return !IsWayland(); }
}
