package io.github.mightycode.mightylib.graphics.internal;

import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.Callback;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL32C.GL_PROGRAM_POINT_SIZE;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * One OpenGL context: the state that belongs to it and not to the objects shared between the contexts.
 * <p>
 * All the contexts of the engine share their objects with a hidden root context (textures, buffers and programs
 * are created once and usable in every window), except the vertex arrays, the framebuffers and the renderbuffers,
 * which each context owns: they are tracked here, and deleted in the context that owns them (immediately when it
 * is current, at its next makeCurrent otherwise). The OpenGL state (capabilities, blend, depth mask, scissor, bound
 * program and textures) is a state of the context too, kept by its GLStateCache.
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

    // State last set in this context: a change equal to it is not sent again
    private final GLStateCache state;
    // Size of the viewport of this context in pixels (glViewport through setViewport), 0 before the first one
    private int viewportWidth;
    private int viewportHeight;

    // GL ids are unique: a set, removed in constant time
    private final Set<Integer> vaos = new LinkedHashSet<>();
    private final Set<Integer> fbos = new LinkedHashSet<>();
    private final Set<Integer> rbos = new LinkedHashSet<>();
    // Deletions asked while another context was current
    private final List<Runnable> deferred = new ArrayList<>();
    // Draws asked in this context and not sent yet (the 2D batches), null when nothing batches here
    private IPendingDraws pendingDraws;

    private GLContext(String name, long windowHandle, boolean root) {
        this.name = name;
        this.windowHandle = windowHandle;
        this.root = root;

        // The context of the window must be current for the capabilities to be created
        glfwMakeContextCurrent(windowHandle);
        capabilities = GL.createCapabilities();
        state = new GLStateCache(new GLStateApplier());
        current = this;
        LIVE.add(this);

        if (Logger.isGLErrorCheck())
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
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, Logger.isGLErrorCheck() ? GLFW_TRUE : GLFW_FALSE);
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

    /**
     * Frames wait for the screen refresh (swap interval 1) or not (0). The swap interval belongs to the current
     * context: this one is made current for the call, then the previous one is again.
     */
    public void setVsync(boolean vsync) {
        GLContext previous = current;
        makeCurrent();
        glfwSwapInterval(vsync ? 1 : 0);

        if (previous != null && previous != this && !previous.destroyed)
            previous.makeCurrent();
    }

    private void applyDefaultState() {
        glfwSwapInterval(0);

        state.activeTexture(0);
        state.blend(true, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        state.capability(GLStateCache.DEPTH_TEST, true);

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

    // ---- Waiting draws ----

    // Causes of the barriers of the engine that are not a renderer
    public static final String CAUSE_CONTEXT = "context";
    public static final String CAUSE_CLEAR = "clear";
    public static final String CAUSE_SCISSOR = "scissor";
    public static final String CAUSE_FRAMEBUFFER = "framebuffer";

    public IPendingDraws pendingDraws() {
        return pendingDraws;
    }

    public void setPendingDraws(IPendingDraws pendingDraws) {
        this.pendingDraws = pendingDraws;
    }

    /** Sends the draws waiting in this context (a barrier), nothing when none. */
    public void flushDraws(Object cause) {
        if (pendingDraws != null)
            pendingDraws.flush(cause);
    }

    /** Sends the draws waiting in the current context, if there is one (before a draw alone, a clear...). */
    public static void FlushCurrentDraws(Object cause) {
        if (current != null && current.pendingDraws != null)
            current.pendingDraws.flush(cause);
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
            // Sent in the context they were asked in, before it stops being current
            if (current != null)
                current.flushDraws(CAUSE_CONTEXT);

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

    // ---- State ----

    /** The OpenGL state of this context: every state change of the engine goes through it. */
    public GLStateCache state() { return state; }

    /** The state of the current context. */
    public static GLStateCache currentState() { return current().state; }

    /**
     * glClear of the color and the depth of the bound framebuffer (limited by the scissor when enabled): the depth
     * mask made writable and the sRGB encoding disabled first (the clear color is stored as written).
     */
    public static void ClearColorAndDepth() {
        FlushCurrentDraws(CAUSE_CLEAR);
        GLStateCache state = currentState();
        state.depthMask(true);
        state.capability(GLStateCache.FRAMEBUFFER_SRGB, false);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    // A program is deleted (its id can be reused): no context considers it bound anymore
    public static void forgetProgram(int program) {
        for (GLContext context : LIVE)
            context.state.forgetProgram(program);
    }

    // A texture is deleted (its id can be reused): no context considers it bound anymore
    public static void forgetTexture(int texture) {
        for (GLContext context : LIVE)
            context.state.forgetTexture(texture);
    }

    // ---- Viewport ----

    // glViewport, remembered by the current context (read by the shaders that size things in pixels)
    public static void setViewport(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
        if (current != null) {
            current.viewportWidth = width;
            current.viewportHeight = height;
        }
    }

    public int viewportWidth() { return viewportWidth; }

    public int viewportHeight() { return viewportHeight; }

    // ---- Objects owned by the context ----

    public void addVertexArray(int vao) { vaos.add(vao); }

    public void removeVertexArray(int vao) { vaos.remove(vao); }

    public void addFramebuffer(int fbo) { fbos.add(fbo); }

    public void removeFramebuffer(int fbo) { fbos.remove(fbo); }

    public void addRenderbuffer(int rbo) { rbos.add(rbo); }

    public void removeRenderbuffer(int rbo) { rbos.remove(rbo); }

    public Set<Integer> vertexArrays() { return vaos; }

    public Set<Integer> framebuffers() { return fbos; }

    public Set<Integer> renderbuffers() { return rbos; }

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
        if (pendingDraws != null) {
            IPendingDraws dropped = pendingDraws;
            pendingDraws = null;
            dropped.contextDestroyed();
        }
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
