package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.renderer._2D.VirtualSceneRenderer;
import io.github.mightycode.mightylib.graphics.surface.BasicBindableObject;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.main.utils.IDisplayable;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.main.utils.IUpdatableDisplayable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.utils.math.UUID;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.*;

/**
 * What a window opened by a context shows (a dialog, a tool window): a small scene without scene changes or loading
 * screen. Its resources must already be loaded (by the batches of the scene that opens it); what it creates in
 * launch() (renderers, texts, GUI) belongs to it and is freed in unload().
 * <p>
 * Code of the view runs with the view as current (Views.current) and its window's OpenGL context current.
 * Close it with close(), or let the user close the window.
 */
public abstract class WindowView extends UUID implements IView, IUpdatableDisplayable {
    private Engine engine;
    private WindowContext windowContext;
    private Camera2D main2DCamera;
    private Camera3D main3DCamera;
    private VirtualSceneRenderer scRenderer;

    // Copy on write: objects can be added or removed while the lists are iterated
    private final List<IUpdatable> updatables = new CopyOnWriteArrayList<>();
    private final List<IDisplayable> displayables = new CopyOnWriteArrayList<>();

    // Null until the view sets one
    private float[] clearColor = null;

    // ---- IView ----

    @Override
    public final Engine engine() {
        if (engine == null)
            throw new IllegalStateException("The view is not open in a window yet");

        return engine;
    }

    @Override
    public final WindowContext context() {
        if (windowContext == null)
            throw new IllegalStateException("The view is not open in a window yet");

        return windowContext;
    }

    // The window where the view is shown
    public final WindowContext window() { return context(); }

    @Override
    public final Camera2D camera2D() {
        if (main2DCamera == null)
            throw new IllegalStateException("The cameras of a window view exist from launch() on");

        return main2DCamera;
    }

    @Override
    public final Camera3D camera3D() {
        if (main3DCamera == null)
            throw new IllegalStateException("The cameras of a window view exist from launch() on");

        return main3DCamera;
    }

    // ---- Life of the view, to override ----

    // Surface on which the view is drawn before being displayed on its window
    protected IGLBindable createVirtualSceneBindable() {
        return new BasicBindableObject().setQualityTexture(TextureParameters.REALISTIC_PARAMETERS);
    }

    /** Called once the window exists: create the renderers here. The cameras and the virtual surface exist. */
    protected void launch(String[] args) {}

    @Override
    public void update() {
        for (IUpdatable updatable : updatables)
            updatable.update();
    }

    @Override
    public void postUpdate() {
        for (IUpdatable updatable : updatables)
            updatable.postUpdate();
    }

    @Override
    public void display() {
        for (IDisplayable displayable : displayables)
            displayable.display();
    }

    /** Called when the window closes: release what launch() created. Call super.unload(). */
    @Override
    public void unload() {
        if (scRenderer != null)
            scRenderer.unload();

        // An object added as updatable and displayable is unloaded once (it would release its GL elements twice)
        Set<Object> unloaded = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IUpdatable updatable : updatables) {
            if (unloaded.add(updatable))
                updatable.unload();
        }

        for (IDisplayable displayable : displayables) {
            if (unloaded.add(displayable))
                displayable.unload();
        }
    }

    public void addUpdatable(IUpdatable updatable) {
        updatables.add(updatable);
    }

    public void addDisplayable(IDisplayable displayable) {
        displayables.add(displayable);
    }

    public void addUpdatableAndDisplayable(IUpdatableDisplayable object) {
        addUpdatable(object);
        addDisplayable(object);
    }

    public void removeUpdatable(IUpdatable updatable) {
        updatables.remove(updatable);
    }

    public void removeDisplayable(IDisplayable displayable) {
        displayables.remove(displayable);
    }

    public void removeUpdatableAndDisplayable(IUpdatableDisplayable object) {
        removeUpdatable(object);
        removeDisplayable(object);
    }

    /** Close the window at the end of the frame. */
    public final void close() {
        context().requestClose();
    }

    // ---- Drawing helpers, like Scene ----

    protected void setVirtualScene() {
        scRenderer.bindFrameBuff();
        windowContext.getWindow().setVirtualViewport();
    }

    protected void setAndDisplayRealScene() {
        scRenderer.unbindFrameBuff();
        windowContext.getWindow().setRealViewport();

        scRenderer.display();
    }

    protected void clear() { glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); }

    // The clear color is a state of the OpenGL context of the window, applied again before each display
    protected void setClearColor(Color4f color) { setClearColor(color.getR(), color.getG(), color.getB(), color.getA()); }

    protected void setClearColor(float color1, float color2, float color3, float alpha) {
        clearColor = new float[]{color1, color2, color3, alpha};
        glClearColor(color1, color2, color3, alpha);
    }

    protected void setClearColor(float color1, float color2, float color3) {
        setClearColor(color1, color2, color3, 1f);
    }

    // ---- Called by the context, with the window's OpenGL context current ----

    final void attach(Engine engine, WindowContext windowContext, String[] args) {
        this.engine = engine;
        this.windowContext = windowContext;

        try (Views.Scope ignored = Views.enter(this)) {
            main3DCamera = windowContext.createCamera(new Camera3DCreationInfo(120f, new Vector3f(0, 0, 0)));
            main2DCamera = new Camera2D(windowContext.getWindow().getInfo(), new Vector2f(0, 0));

            scRenderer = new VirtualSceneRenderer(windowContext.getWindow().getInfo(), createVirtualSceneBindable());

            launch(args);
        }
    }

    final void updateByContext() {
        try (Views.Scope ignored = Views.enter(this)) {
            update();
        }
    }

    final void postUpdateByContext() {
        try (Views.Scope ignored = Views.enter(this)) {
            postUpdate();
        }
    }

    final void displayByContext() {
        try (Views.Scope ignored = Views.enter(this)) {
            if (clearColor != null)
                glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);

            display();
        }
    }

    final void unloadByContext() {
        try (Views.Scope ignored = Views.enter(this)) {
            unload();
        }
    }
}
