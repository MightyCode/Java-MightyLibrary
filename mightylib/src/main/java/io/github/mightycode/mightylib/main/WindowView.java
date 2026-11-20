package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.graphics.renderer._2D.VirtualSceneRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.IDrawSortKey;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.graphics.surface.BasicBindableObject;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.main.utils.IDisplayable;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.main.utils.IUpdatableDisplayable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandOwner;
import io.github.mightycode.mightylib.utils.math.UUID;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
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
    // The 2D batch of the view, created at the first use
    private DrawBatch2D batch2D;
    private WindowContext windowContext;
    private Camera2D main2DCamera;
    private Camera3D main3DCamera;
    private VirtualSceneRenderer scRenderer;

    // Copy on write: objects can be added or removed while the lists are iterated
    private final List<IUpdatable> updatables = new CopyOnWriteArrayList<>();
    private final List<IDisplayable> displayables = new CopyOnWriteArrayList<>();

    // Null until the view sets one
    private float[] clearColor = null;
    private ERenderColorSpace colorSpace = ERenderColorSpace.INHERIT;
    // Null: those of the engine
    private String finalPassShader;
    private PictureSettings picture;

    // ---- IView ----

    /** The 2D batch of the view: its batched draws, sent at its barriers and at the end of display. */
    @Override
    public final DrawBatch2D batch2D() {
        if (batch2D == null)
            batch2D = new DrawBatch2D(Batches2D.of(engine()));
        return batch2D;
    }

    /**
     * The order of the batched 2D draws of the view (IDrawSortKey, DrawSortKeys), null for the order they are asked
     * (the default, nothing sorted).
     */
    public final void setDrawSortKey(IDrawSortKey sortKey) {
        batch2D().setDrawSortKey(sortKey);
    }

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
    public final ERenderColorSpace colorSpace() {
        return colorSpace;
    }

    /**
     * The color space of the draws of this view: LINEAR (a lit scene), GAMMA, or INHERIT (the default) for the one
     * of the engine. A renderer may still override it. Takes effect at the next draw, at any time.
     */
    public final void setColorSpace(ERenderColorSpace colorSpace) {
        this.colorSpace = Objects.requireNonNull(colorSpace, "colorSpace");
    }

    @Override
    public final String finalPassShader() {
        return finalPassShader;
    }

    // The shader of the final pass of this view (name of a ShaderData), null for the one of the engine
    public final void setFinalPassShader(String shaderName) {
        finalPassShader = shaderName;
    }

    @Override
    public final PictureSettings picture() {
        return picture;
    }

    // The settings of the final pass of this view, null for those of the engine (the player's)
    public final void setPicture(PictureSettings picture) {
        this.picture = picture;
    }

    @Override
    public final Camera3D camera3D() {
        if (main3DCamera == null)
            throw new IllegalStateException("The cameras of a window view exist from launch() on");

        return main3DCamera;
    }

    // ---- Life of the view, to override ----

    /**
     * Hook called when the window opens, before launch: the surface on which the view is drawn (setVirtualScene)
     * before being scaled to its window (setAndDisplayRealScene). Unloaded by super.unload().
     */
    protected IGLBindable createVirtualSceneBindable() {
        return new BasicBindableObject().setQualityTexture(TextureParameters.REALISTIC_PARAMETERS);
    }

    /**
     * Hook called once the window exists, with its OpenGL context current: create the renderers here. The cameras
     * and the virtual surface exist. No need to call super.
     */
    protected void launch(String[] args) {}

    // Hook called at each update. Call super.update() to update the objects added with addUpdatable.
    @Override
    public void update() {
        for (IUpdatable updatable : updatables)
            updatable.update();
    }

    // Hook called after update. Call super.postUpdate() to post-update the objects added with addUpdatable.
    @Override
    public void postUpdate() {
        for (IUpdatable updatable : updatables)
            updatable.postUpdate();
    }

    /**
     * Hook called at each frame, with the OpenGL context of the window current. Usually setVirtualScene(), clear(),
     * the drawing, then setAndDisplayRealScene(). super.display() draws the objects added with addDisplayable.
     */
    @Override
    public void display() {
        for (IDisplayable displayable : displayables)
            displayable.display();
    }

    /**
     * Hook called when the window closes: release what launch() created. Call super.unload() (it unloads the
     * virtual surface and the added objects).
     */
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

    /**
     * A command of this view, run under it (its view and the OpenGL context of its window), removed when the
     * window closes. From launch() on.
     * The same view opened in several windows has the command in each: the window targeted by the commands runs it.
     * @throws IllegalArgumentException if a command of the engine, the game or this view has the same identifier
     */
    protected final void addCommand(BaseCommand command) {
        engine().commands().register(command, CommandOwner.Of(this, "window"));
    }

    public final void addUpdatable(IUpdatable updatable) {
        updatables.add(updatable);
    }

    public final void addDisplayable(IDisplayable displayable) {
        displayables.add(displayable);
    }

    public final void addUpdatableAndDisplayable(IUpdatableDisplayable object) {
        addUpdatable(object);
        addDisplayable(object);
    }

    public final void removeUpdatable(IUpdatable updatable) {
        updatables.remove(updatable);
    }

    public final void removeDisplayable(IDisplayable displayable) {
        displayables.remove(displayable);
    }

    public final void removeUpdatableAndDisplayable(IUpdatableDisplayable object) {
        removeUpdatable(object);
        removeDisplayable(object);
    }

    /** Close the window at the end of the frame. */
    public final void close() {
        context().requestClose();
    }

    // ---- Drawing helpers, like Scene ----

    protected final void setVirtualScene() {
        scRenderer.bindFrameBuff();
        windowContext.getWindow().setVirtualViewport();
    }

    protected final void setAndDisplayRealScene() {
        scRenderer.unbindFrameBuff();
        windowContext.getWindow().setRealViewport();
        // A scissor of the virtual scene left enabled would cut the final pass (the window has another size)
        clearScissor();

        scRenderer.display();
    }

    /**
     * Limits the next draws (and clears) to a rectangle of the surface of the view, in pixels of the virtual scene,
     * origin at the bottom left: to draw the same thing in several modes side by side. Undone by clearScissor,
     * and by setAndDisplayRealScene.
     */
    protected final void setScissor(int x, int y, int width, int height) {
        GLContext.FlushCurrentDraws(GLContext.CAUSE_SCISSOR);
        GLContext.currentState().scissor(x, y, width, height);
    }

    protected final void clearScissor() {
        GLContext.FlushCurrentDraws(GLContext.CAUSE_SCISSOR);
        GLContext.currentState().disableScissor();
    }

    protected final void clear() {
        GLContext.ClearColorAndDepth();
    }

    // The clear color is a state of the OpenGL context of the window, applied again before each display
    protected final void setClearColor(Color4f color) { setClearColor(color.getR(), color.getG(), color.getB(), color.getA()); }

    protected final void setClearColor(float color1, float color2, float color3, float alpha) {
        clearColor = new float[]{color1, color2, color3, alpha};
        glClearColor(color1, color2, color3, alpha);
    }

    protected final void setClearColor(float color1, float color2, float color3) {
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
            // The draws of the view are sent before another view draws
            if (batch2D != null)
                batch2D.endView();
        }
    }

    final void unloadByContext() {
        try (Views.Scope ignored = Views.enter(this)) {
            engine.commands().removeOwnedBy(CommandOwner.Of(this, "window"));
            unload();
        }
    }
}
