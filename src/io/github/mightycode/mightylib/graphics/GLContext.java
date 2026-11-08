package io.github.mightycode.mightylib.graphics;

import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.Callback;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL32C.GL_PROGRAM_POINT_SIZE;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * One OpenGL context: the state that belongs to it and not to the objects shared between the contexts.
 * <p>
 * All the contexts of the engine share their objects with a hidden root context (textures, buffers and programs
 * are created once and usable in every window), except the vertex arrays, the framebuffers and the renderbuffers,
 * which each context owns: they are tracked here, and deleted in the context that owns them (immediately when it
 * is current, at its next makeCurrent otherwise). The bound program is a state of the context too.
 * <p>
 * Main thread only. A context is current from makeCurrent until another one is.
 */
public final class GLContext {
    // Contexts alive, the root included
    private static final List<GLContext> LIVE = new ArrayList<>();
    private static GLContext current;

    private final String name;
    private final long windowHandle;
    private final boolean root;
    private final GLCapabilities capabilities;
    // Only set when OpenGL error checks are enabled
    private Callback debugMessageCallback;
    private boolean destroyed;

    // Program bound in this context, -1 when unknown: avoids sending glUseProgram again
    private int currentProgram = -1;

    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> fbos = new ArrayList<>();
    private final List<Integer> rbos = new ArrayList<>();
    // Deletions asked while another context was current
    private final List<Runnable> deferred = new ArrayList<>();

    private GLContext(String name, long windowHandle, boolean root) {
        this.name = name;
        this.windowHandle = windowHandle;
        this.root = root;

        // The context of the window must be current for the capabilities to be created
        glfwMakeContextCurrent(windowHandle);
        capabilities = GL.createCapabilities();
        current = this;
        LIVE.add(this);

        if (Logger.DO_CHECK)
            setupDebugMessages();
    }

    /** Hints of the windows of the engine (core profile 3.3, hidden): each window adds its own. */
    public static void applyWindowHints() {
        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // A debug context lets the driver report OpenGL errors itself, with a readable message
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, Logger.DO_CHECK ? GLFW_TRUE : GLFW_FALSE);
    }

    /**
     * The hidden context every window shares its objects with: it lives as long as the engine, so closing any
     * window leaves the shared objects alive.
     */
    public static GLContext createRoot() {
        applyWindowHints();
        long handle = glfwCreateWindow(1, 1, "MightyLibrary GL root", NULL, NULL);
        if (handle == NULL)
            throw new IllegalStateException("The shared OpenGL context can't be created");

        return new GLContext("root", handle, true);
    }

    /** The context of a window just created (shared with the root): made current and given the default GL state. */
    public static GLContext ofWindow(String name, long windowHandle) {
        GLContext context = new GLContext(name, windowHandle, false);
        context.applyDefaultState();
        return context;
    }

    private void applyDefaultState() {
        glfwSwapInterval(0);

        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        // Enable antiAliasing
        glEnable(GL_MULTISAMPLE);

        glEnable(GL_PROGRAM_POINT_SIZE);
    }

    private void setupDebugMessages() {
        debugMessageCallback = GLUtil.setupDebugMessageCallback(System.err);
        if (debugMessageCallback == null) {
            System.err.println("OpenGL debug messages are not supported by this driver");
            return;
        }

        // Notifications are very frequent and not errors: only keep low, medium and high severity messages
        if (capabilities.OpenGL43)
            GL43.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL43.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
        else if (capabilities.GL_KHR_debug)
            KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
    }

    // ---- Current context ----

    public static GLContext current() {
        if (current == null)
            throw new IllegalStateException("No current OpenGL context (every window destroyed, or not created yet)");

        return current;
    }

    public static GLContext currentOrNull() {
        return current;
    }

    /** Make this context the one the GL calls of the thread go to; the deletions waiting for it are done. */
    public void makeCurrent() {
        if (destroyed)
            throw new IllegalStateException("The OpenGL context " + name + " is destroyed");

        if (current != this) {
            glfwMakeContextCurrent(windowHandle);
            GL.setCapabilities(capabilities);
            current = this;
        }

        if (!deferred.isEmpty())
            runDeferred();
    }

    private void runDeferred() {
        List<Runnable> toRun = new ArrayList<>(deferred);
        deferred.clear();
        for (Runnable deletion : toRun)
            deletion.run();
    }

    public String getName() { return name; }

    public long getWindowHandle() { return windowHandle; }

    public boolean isRoot() { return root; }

    public boolean isDestroyed() { return destroyed; }

    // More than one window draws: what a context creates for another one must be flushed to be visible there
    public static boolean hasSeveralWindows() {
        int windows = 0;
        for (GLContext context : LIVE) {
            if (!context.root)
                ++windows;
        }

        return windows > 1;
    }

    // ---- Bound program ----

    public int currentProgram() { return currentProgram; }

    public void setCurrentProgram(int program) { currentProgram = program; }

    // A program is deleted (its id can be reused): no context considers it bound anymore
    public static void forgetProgram(int program) {
        for (GLContext context : LIVE) {
            if (context.currentProgram == program)
                context.currentProgram = -1;
        }
    }

    // ---- Objects owned by the context ----

    public void addVertexArray(int vao) { vaos.add(vao); }

    public void removeVertexArray(Integer vao) { vaos.remove(vao); }

    public void addFramebuffer(int fbo) { fbos.add(fbo); }

    public void removeFramebuffer(Integer fbo) { fbos.remove(fbo); }

    public void addRenderbuffer(int rbo) { rbos.add(rbo); }

    public void removeRenderbuffer(Integer rbo) { rbos.remove(rbo); }

    public List<Integer> vertexArrays() { return vaos; }

    public List<Integer> framebuffers() { return fbos; }

    public List<Integer> renderbuffers() { return rbos; }

    // The deletion is done at the next makeCurrent of this context
    public void deleteLater(Runnable deletion) {
        deferred.add(deletion);
    }

    public int pendingDeletions() { return deferred.size(); }

    public static List<GLContext> live() {
        return new ArrayList<>(LIVE);
    }

    // ---- End ----

    /** Forget the context: its objects are freed with it. The window is destroyed by the caller afterward. */
    public void destroy() {
        if (destroyed)
            return;

        destroyed = true;
        LIVE.remove(this);
        if (current == this) {
            current = null;
            glfwMakeContextCurrent(NULL);
            GL.setCapabilities(null);
        }

        if (debugMessageCallback != null) {
            debugMessageCallback.free();
            debugMessageCallback = null;
        }

        vaos.clear();
        fbos.clear();
        rbos.clear();
        deferred.clear();
    }

    /** The root has no Window: it destroys its hidden window itself. */
    public void destroyRoot() {
        destroy();
        glfwDestroyWindow(windowHandle);
    }
}
