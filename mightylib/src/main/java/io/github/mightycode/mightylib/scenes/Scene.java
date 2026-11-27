package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.*;
import io.github.mightycode.mightylib.graphics.surface.BasicBindableObject;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.main.WindowConfig;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.main.WindowView;
import io.github.mightycode.mightylib.graphics.renderer._2D.VirtualSceneRenderer;
import io.github.mightycode.mightylib.graphics.debug.DebugDraw2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.IDrawSortKey;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.utils.Logger;
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
 * A screen of the game: what the engine updates and displays in a window, one scene at a time per window.
 * Extend it and override its hooks: getInvolvedBatch, init (declare the resources), launch (create the objects),
 * update, postUpdate, display, unload and release. The state of the scene is reached through methods: context(),
 * resources(), glResources(), camera2D(), camera3D(), sceneManager(), args().
 */
public abstract class Scene extends UUID implements IUpdatableDisplayable, IView {
    private final Engine engine;
    private final Resources resources;
    private final GLResources glResources;
    // The 2D batch of the view, created at the first use
    private DrawBatch2D batch2D;
    private DebugDraw2D debugDraw;
    // Its debug drawings were drawn in the current display
    private boolean debugDrawn;

    private final WindowContext mainContext;
    private Camera3D main3DCamera;
    private Camera2D main2DCamera;
    private SceneManagerInterface sceneManagerInterface;
    private VirtualSceneRenderer scRenderer;

    // Copy on write: objects can be added or removed while the lists are iterated (update, display...)
    private final List<IUpdatable> updatables = new CopyOnWriteArrayList<>();
    private final List<IDisplayable> displayables = new CopyOnWriteArrayList<>();

    private TemplateSceneLoading loadingScene = null; // can be null
    private boolean isWaitingForLoading = false;
    // launch() was called, the loading waits for the GL elements
    private boolean launched = false;

    private static final long GL_LOADING_WARNING_NANOS = 10_000_000_000L;
    private long glLoadingStart = 0;
    private boolean glLoadingWarned = false;

    // Null until the scene sets one
    private float[] clearColor = null;
    private ERenderColorSpace colorSpace = ERenderColorSpace.INHERIT;
    // Null: those of the engine
    private String finalPassShader;
    private PictureSettings picture;

    private String[] args;

    private final Camera3DCreationInfo cameraParameters;

    // Cameras reachable by the camera command
    private final List<Camera> cameras = new CopyOnWriteArrayList<>();

    // A scene belongs to the engine and the window of the view that creates it
    public Scene(Camera3DCreationInfo info) {
        IView creator = Views.current();
        engine = creator.engine();
        resources = engine.resources();
        glResources = engine.glResources();
        mainContext = creator.context();

        if (info == null) {
            cameraParameters = new Camera3DCreationInfo();
            cameraParameters.fov = 120f;
            cameraParameters.initialPosition = new Vector3f(0, 0, 0);
        } else {
            cameraParameters = info;
        }

        sceneManagerInterface = null;
    }

    public Scene(){
        this(null);
    }

    /**
     * Open a window drawn by the view, belonging to this scene: closed when the scene is left unless the
     * configuration makes it persistent. By default, its parent is the window of the scene.
     */
    protected final WindowContext openWindow(WindowConfig config, WindowView view, String... args) {
        return mainContext.getContext().openWindow(config, view, args);
    }

    // ---- IView ----

    @Override
    public final Engine engine() { return engine; }

    @Override
    public final WindowContext context() { return mainContext; }

    @Override
    public final Camera2D camera2D() {
        if (main2DCamera == null)
            throw new IllegalStateException("The cameras of " + getClass().getSimpleName() + " exist from launch() on");

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
            throw new IllegalStateException("The cameras of " + getClass().getSimpleName() + " exist from launch() on");

        return main3DCamera;
    }

    // The resources of the engine (CPU side: declare, reference, read)
    protected final Resources resources() { return resources; }

    // The GL elements of the engine (GPU side of the resources)
    protected final GLResources glResources() { return glResources; }

    /** The 2D batch of the scene: its batched draws, sent at its barriers and at the end of display. */
    @Override
    public final DrawBatch2D batch2D() {
        if (batch2D == null)
            batch2D = Batches2D.of(engine).createBatch(getClass().getSimpleName());
        return batch2D;
    }

    /**
     * The debug drawings of the scene: drawn over it at the end of its display (before the final pass of
     * setAndDisplayRealScene), dropped when the scene is left.
     */
    @Override
    public final DebugDraw2D debugDraw() {
        if (debugDraw == null)
            debugDraw = new DebugDraw2D(engine.debugDraws(), this, getClass().getSimpleName());
        return debugDraw;
    }

    // Once per frame, at the end of the drawing of the scene
    private void drawDebug() {
        if (debugDraw == null || debugDrawn)
            return;

        debugDrawn = true;
        debugDraw.drawFrame(batch2D());
    }

    /**
     * The order of the batched 2D draws of the scene (IDrawSortKey, DrawSortKeys), null for the order they are asked
     * (the default, nothing sorted).
     */
    public final void setDrawSortKey(IDrawSortKey sortKey) {
        batch2D().setDrawSortKey(sortKey);
    }

    // Changes the scene of the window, preloads the next one or quits the game; null before init()
    protected final SceneManagerInterface sceneManager() { return sceneManagerInterface; }

    // A copy of the arguments the scene was set with (scene command, setNewScene), empty before init()
    protected final String[] args() {
        return args == null ? new String[0] : args.clone();
    }

    private void initGlobalParameters(){
        main3DCamera = mainContext.createCamera(cameraParameters);
        main2DCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0,0));

        cameras.clear();
        registerCamera(main3DCamera, "main3D");
        registerCamera(main2DCamera, "main2D");
    }

    /**
     * Make a camera reachable by the camera command (by its id or its name), the main cameras are registered.
     */
    public final void registerCamera(Camera camera, String name) {
        camera.setName(name);
        if (!cameras.contains(camera))
            cameras.add(camera);
    }

    public final void unregisterCamera(Camera camera) {
        cameras.remove(camera);
    }

    public final List<Camera> getCameras() {
        return Collections.unmodifiableList(cameras);
    }

    // Camera registered in this scene with this id or name, null if none
    public final Camera findCamera(String idOrName) {
        for (Camera camera : cameras) {
            if (camera.getName().equals(idOrName) || String.valueOf(camera.getCameraId()).equals(idOrName))
                return camera;
        }

        return null;
    }

    /**
     * Hook called when the scene is set, after getInvolvedBatch and createLoadingScene, before its resources are
     * loaded: declare or reference what the scene needs here. The cameras and the virtual scene do not exist yet.
     * No need to call super.
     */
    public void init(String[] args) {}

    /**
     * Hook called once the resources are loaded: create the renderers and objects here. The cameras and the
     * virtual scene exist. The scene is displayed once the GL elements it asked for are loaded. No need to call
     * super.
     */
    public void launch(String[] args) {}

    /**
     * Hook called when the scene is set, before init: the scene displayed while this one loads (the loading scene
     * of the game by default), null to display nothing. The engine launches and unloads it.
     */
    protected TemplateSceneLoading createLoadingScene(String[] args) {
        return sceneManagerInterface.createDefaultLoadingScene();
    }

    /**
     * Hook called before launch: the surface on which the scene is drawn (setVirtualScene) before being scaled to
     * the window (setAndDisplayRealScene). Unloaded by super.unload().
     */
    protected IGLBindable createVirtualSceneBindable() {
        return new BasicBindableObject().setQualityTexture(TextureParameters.REALISTIC_PARAMETERS);
    }

    final void initByManager(String[] args) {
        try (Views.Scope ignored = Views.enter(this)) {
            // The code of the scene draws in its window: its OpenGL context is the current one
            mainContext.getWindow().bindWindow();
            this.args = args;
            isWaitingForLoading = true;
            launched = false;

            loadingScene = createLoadingScene(args);
            if (loadingScene != null) {
                // Lets the loading scene leave the loading (a key to go back to the menu...)
                loadingScene.setSceneManagerInterface(sceneManagerInterface);
                loadingScene.launchByManager(args);
            }

            init(args);
        }
    }

    final void launchByManager(String[] args) {
        try (Views.Scope ignored = Views.enter(this)) {
            // The code of the scene draws in its window: its OpenGL context is the current one
            mainContext.getWindow().bindWindow();
            this.args = args;
            initGlobalParameters();

            scRenderer = new VirtualSceneRenderer(mainContext.getWindow().getInfo(), createVirtualSceneBindable());

            launch(args);
        }
    }

    public final void addUpdatable(IUpdatable updatable){
        updatables.add(updatable);
    }

    public final void addDisplayable(IDisplayable displayable){
        displayables.add(displayable);
    }

    public final void addUpdatableAndDisplayable(IUpdatableDisplayable object){
        addUpdatable(object);
        addDisplayable(object);
    }

    public final void removeUpdatable(IUpdatable updatable){
        updatables.remove(updatable);
    }

    public final void removeDisplayable(IDisplayable displayable){
        displayables.remove(displayable);
    }

    public final void removeUpdatableAndDisplayable(IUpdatableDisplayable object){
        removeUpdatable(object);
        removeDisplayable(object);
    }


    boolean isWaitingForLoading() {
        return isWaitingForLoading;
    }

    // launch() was called: the cameras, renderers and objects of the scene exist
    public final boolean isLaunched() {
        return launched;
    }

    /**
     * A command of this scene, run under it (its view and the OpenGL context of its window), removed when the
     * scene is left, even during its loading. It can be typed while the scene loads: CommandCondition.launched on the root of its syntax.
     * The same scene shown in several windows has the command in each: the window targeted by the commands runs it.
     * @throws IllegalArgumentException if a command of the engine, the game or this scene has the same identifier
     */
    protected final void addCommand(BaseCommand command) {
        engine.commands().register(command, CommandOwner.Of(this, "scene"));
    }

    final void setSceneManagerInterface(SceneManagerInterface sceneManagerInterface) {
        this.sceneManagerInterface = sceneManagerInterface;
    }

    final void updateByManager(){
        try (Views.Scope ignored = Views.enter(this)) {
            // The code of the scene draws in its window: its OpenGL context is the current one
            mainContext.getWindow().bindWindow();
            if (isWaitingForLoading) {
                if (loadingScene != null)
                    loadingScene.updateByManager();
            } else {
                // A new tick: the drawings of one frame asked by the previous tick are replaced by those of this one
                if (debugDraw != null)
                    debugDraw.updateStarted();
                update();
            }
        }
    }

    /**
     * Hook called at each update once the scene is displayed. Call super.update() to update the objects added
     * with addUpdatable.
     */
    @Override
    public void update() {
        for (IUpdatable updatable : updatables) {
            updatable.update();
        }
    }

    void postUpdateByManager(){
        try (Views.Scope ignored = Views.enter(this)) {
            // The code of the scene draws in its window: its OpenGL context is the current one
            mainContext.getWindow().bindWindow();
            if (isWaitingForLoading) {
                if (loadingScene != null)
                    loadingScene.postUpdateByManager();
            } else {
                postUpdate();
            }
        }
    }

    // Hook called after update. Call super.postUpdate() to post-update the objects added with addUpdatable.
    @Override
    public void postUpdate() {
        for (IUpdatable updatable : updatables) {
            updatable.postUpdate();
        }
    }

    /*
     * Loading in two phases, the loading scene stays displayed during both:
     * 1. the resources are loaded (CPU, workers), then launch() creates the scene objects;
     * 2. the GL elements they asked for are sent with the time left in each frame,
     *    then the scene is displayed and updated: its first frame is complete.
     */
    final void displayByManager() {
        try (Views.Scope ignored = Views.enter(this)) {
            // The code of the scene draws in its window: its OpenGL context is the current one
            mainContext.getWindow().bindWindow();
            debugDrawn = false;
            display0();
            // A scene drawn without virtual scene: its debug drawings at the end
            if (!isWaitingForLoading)
                drawDebug();
            // The draws of the scene are sent before another view draws
            if (batch2D != null)
                batch2D.endView();
        }
    }

    private void display0() {
        if (isWaitingForLoading) {
            if (loadingScene != null)
                loadingScene.displayByManager();

            if (!launched) {
                boolean resourcesLoaded = loadingScene != null ? loadingScene.isFinished() : resources.finishedCurrentLoading();
                if (resourcesLoaded) {
                    launched = true;
                    launchByManager(args);
                }
            } else if (!glResources.hasElementsToLoad()) {
                isWaitingForLoading = false;

                // Its framebuffer, texts and textures would leak at each scene change otherwise
                if (loadingScene != null) {
                    loadingScene.unload();
                    loadingScene = null;
                }
            } else {
                warnIfGLLoadingIsLong();
            }
        } else {
            applyClearColor();
            display();
        }
    }

    // An element whose dependencies never load keeps the loading screen forever: say which one
    private void warnIfGLLoadingIsLong() {
        if (glLoadingStart == 0) {
            glLoadingStart = System.nanoTime();
        } else if (!glLoadingWarned && System.nanoTime() - glLoadingStart > GL_LOADING_WARNING_NANOS) {
            glLoadingWarned = true;
            Logger.Warn(Logger.CATEGORY_ENGINE, getClass().getSimpleName() + " still waits for GL elements : " + glResources.describeWaitingElements());
        }
    }

    /**
     * Hook called at each frame once the scene is loaded, with the OpenGL context of its window current. Usually
     * setVirtualScene(), clear(), the drawing, then setAndDisplayRealScene(). super.display() draws the objects
     * added with addDisplayable.
     */
    @Override
    public void display() {
        for (IDisplayable displayable : displayables)
            displayable.display();
    }

    // Draw on the virtual scene (createVirtualSceneBindable), at its resolution
    protected final void setVirtualScene(){
        scRenderer.bindFrameBuff();
        mainContext.getWindow().setVirtualViewport();
    }

    // Draw on the window again and display the virtual scene on it, scaled
    protected final void setAndDisplayRealScene(){
        // The debug drawings over the scene, on its virtual scene
        drawDebug();
        scRenderer.unbindFrameBuff();
        mainContext.getWindow().setRealViewport();
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

    // The clear color is a global GL state: kept by the scene and applied again before each of its displays,
    // so the loading scene (displayed after launch) or another scene can't replace it
    protected final void setClearColor(Color4f color) { setClearColor(color.getR(), color.getG(), color.getB(), color.getA());}
    protected final void setClearColor(float color1, float color2, float color3, float alpha){
        clearColor = new float[]{color1, color2, color3, alpha};
        glClearColor(color1, color2, color3, alpha); }
    protected final void setClearColor(int color1, int color2, int color3, float alpha){
        setClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha); }
    protected final void setClearColor(float color1, float color2, float color3){
        setClearColor(color1, color2, color3, 1f);}
    protected final void setClearColor(float color, float alpha){
        setClearColor(color, color, color, alpha);}
    protected final void setClearColor(float color){
        setClearColor(color, color, color, 1f);}

    private void applyClearColor() {
        if (clearColor != null)
            glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
    }

    /**
     * Hook called when the scene is set, before init: the batches (resources/batch files) the scene needs, loaded
     * before launch and kept while a scene needs them.
     */
    protected String[] getInvolvedBatch(){
        return new String[]{};
    }

    /**
     * Hook called when the scene is left, only if launch() was called: release what launch() created. Call
     * super.unload() (it unloads the virtual scene and the added objects). The scene can be left while it is still
     * loading: then only release() is called.
     */
    @Override
    public void unload() {
        if (scRenderer != null)
            scRenderer.unload();
        if (debugDraw != null)
            debugDraw.delete();

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
     * Hook called when the scene is left, after unload(), launched or not: release what init() created
     * (resources added or referenced by the scene). No need to call super.
     */
    public void release() {}

    final void unloadByManager() {
        try (Views.Scope ignored = Views.enter(this)) {
            // The code of the scene draws in its window: its OpenGL context is the current one
            mainContext.getWindow().bindWindow();
            // The windows and the commands of this scene go with it
            engine.commands().removeOwnedBy(CommandOwner.Of(this, "scene"));
            mainContext.getContext().closeWindowsOpenedBy(this);
            mainContext.getWindow().bindWindow();

            if (loadingScene != null) {
                loadingScene.unload();
                loadingScene = null;
            }

            if (launched)
                unload();

            release();
        }
    }
}
