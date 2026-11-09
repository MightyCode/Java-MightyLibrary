package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.graphics.GLResources;
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
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.scenes.camera.Camera;
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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.*;

public abstract class Scene extends UUID implements IUpdatableDisplayable, IView {
    private final Engine engine;
    protected final Resources resources;
    protected final GLResources glResources;

    protected final WindowContext mainContext;
    protected Camera3D main3DCamera;
    protected Camera2D main2DCamera;
    protected SceneManagerInterface sceneManagerInterface;
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

    protected String[] args;

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
     * configuration makes it persistent. By default its parent is the window of the scene.
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
    public final Camera3D camera3D() {
        if (main3DCamera == null)
            throw new IllegalStateException("The cameras of " + getClass().getSimpleName() + " exist from launch() on");

        return main3DCamera;
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
     * Called when the scene is set, before its resources are loaded: declare what the scene needs here.
     * The cameras and the virtual scene do not exist yet. No need to call super.
     */
    public void init(String[] args) {}

    /**
     * Called once the resources are loaded: create the renderers here.
     * The cameras and the virtual scene already exist. No need to call super.
     */
    public void launch(String[] args) {}

    // Scene displayed while this scene loads, null to display nothing
    protected TemplateSceneLoading createLoadingScene(String[] args) {
        return sceneManagerInterface.createDefaultLoadingScene();
    }

    // Surface on which the scene is drawn before being displayed on the window
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

    public void addUpdatable(IUpdatable updatable){
        updatables.add(updatable);
    }

    public void addDisplayable(IDisplayable displayable){
        displayables.add(displayable);
    }

    public void addUpdatableAndDisplayable(IUpdatableDisplayable object){
        addUpdatable(object);
        addDisplayable(object);
    }

    public void removeUpdatable(IUpdatable updatable){
        updatables.remove(updatable);
    }

    public void removeDisplayable(IDisplayable displayable){
        displayables.remove(displayable);
    }

    public void removeUpdatableAndDisplayable(IUpdatableDisplayable object){
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
     * scene is left, even during its loading. It can be typed while the scene loads: BaseCommand.requireLaunched.
     * @throws IllegalArgumentException if a command with the same name already exists
     */
    protected final void addCommand(BaseCommand command) {
        engine.commands().register(command, CommandOwner.Of(this, "scene"));
    }

    public void setSceneManagerInterface(SceneManagerInterface sceneManagerInterface) {
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
                update();
            }
        }
    }

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
            display0();
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
            System.err.println("--" + getClass().getSimpleName() + " still waits for GL elements : " + glResources.describeWaitingElements());
        }
    }

    public void display() {
        for (IDisplayable displayable : displayables)
            displayable.display();
    }

    protected void setVirtualScene(){
        scRenderer.bindFrameBuff();
        mainContext.getWindow().setVirtualViewport();
    }

    protected void setAndDisplayRealScene(){
        scRenderer.unbindFrameBuff();
        mainContext.getWindow().setRealViewport();

        scRenderer.display();
    }

    protected void clear(){ glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); }

    // The clear color is a global GL state: kept by the scene and applied again before each of its displays,
    // so the loading scene (displayed after launch) or another scene can't replace it
    protected void setClearColor(Color4f color) { setClearColor(color.getR(), color.getG(), color.getB(), color.getA());}
    protected void setClearColor(float color1, float color2, float color3, float alpha){
        clearColor = new float[]{color1, color2, color3, alpha};
        glClearColor(color1, color2, color3, alpha); }
    protected void setClearColor(int color1, int color2, int color3, float alpha){
        setClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha); }
    protected void setClearColor(float color1, float color2, float color3){
        setClearColor(color1, color2, color3, 1f);}
    protected void setClearColor(float color, float alpha){
        setClearColor(color, color, color, alpha);}
    protected void setClearColor(float color){
        setClearColor(color, color, color, 1f);}

    private void applyClearColor() {
        if (clearColor != null)
            glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
    }

    // Resources
    protected String[] getInvolvedBatch(){
        return new String[]{};
    }

    /**
     * Called when the scene is left, only if launch() was called: release what launch() created. Call super.unload().
     * The scene can be left while it is still loading: then only release() is called.
     */
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
     * Called when the scene is left, after unload(), launched or not: release what init() created
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
