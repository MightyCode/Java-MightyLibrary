package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.main.utils.*;
import io.github.mightycode.mightylib.graphics.surface.BasicBindableObject;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.ContextManager;
import io.github.mightycode.mightylib.graphics.renderer._2D.VirtualSceneRenderer;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.scenes.camera.Camera;
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

public abstract class Scene extends UUID implements IUpdatableDisplayable {
    protected final Resources resources;
    protected final GLResources glResources;

    protected final ShaderManager shaderManager;
    protected final Context mainContext;
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

    public Scene(Camera3DCreationInfo info) {
        resources = Resources.getInstance();
        glResources = GLResources.getInstance();

        shaderManager = ShaderManager.getInstance();
        mainContext = ContextManager.getInstance().getMainContext();

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

    private void initGlobalParameters(){
        main3DCamera = mainContext.createCamera(cameraParameters);
        main2DCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0,0));
        shaderManager.setCameras(main2DCamera, main3DCamera);

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

    final void launchByManager(String[] args) {
        this.args = args;
        initGlobalParameters();

        scRenderer = new VirtualSceneRenderer(mainContext.getWindow().getInfo(), createVirtualSceneBindable());

        launch(args);
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

    public void setSceneManagerInterface(SceneManagerInterface sceneManagerInterface) {
        this.sceneManagerInterface = sceneManagerInterface;
    }

    final void updateByManager(){
        if (isWaitingForLoading) {
            if (loadingScene != null)
                loadingScene.updateByManager();
        } else {
            update();
        }
    }

    public void update() {
        for (IUpdatable updatable : updatables) {
            updatable.update();
        }
    }

    void postUpdateByManager(){
        if (isWaitingForLoading) {
            if (loadingScene != null)
                loadingScene.postUpdateByManager();
        } else {
            postUpdate();
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
        if (isWaitingForLoading) {
            if (loadingScene != null) {
                loadingScene.useCameras();
                loadingScene.displayByManager();
            }

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

                useCameras();
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

    // The shaders use the cameras of the scene being displayed
    final void useCameras() {
        shaderManager.setCameras(main2DCamera, main3DCamera);
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
        if (loadingScene != null) {
            loadingScene.unload();
            loadingScene = null;
        }

        if (launched)
            unload();

        release();
    }
}
