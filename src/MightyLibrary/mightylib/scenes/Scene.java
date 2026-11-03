package MightyLibrary.mightylib.scenes;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.main.utils.*;
import MightyLibrary.mightylib.graphics.surface.BasicBindableObject;
import MightyLibrary.mightylib.graphics.surface.IGLBindable;
import MightyLibrary.mightylib.graphics.surface.TextureParameters;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.graphics.renderer._2D.VirtualSceneRenderer;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.scenes.camera.Camera3D;
import MightyLibrary.mightylib.scenes.camera.Camera3DCreationInfo;
import MightyLibrary.mightylib.utils.math.UUID;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
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

    protected String[] args;

    private final Camera3DCreationInfo cameraParameters;

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
        if (loadingScene != null)
            loadingScene.launchByManager(args);

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

    protected void setClearColor(Color4f color) { glClearColor(color.getR(), color.getG(), color.getB(), color.getA());}
    protected void setClearColor(float color1, float color2, float color3, float alpha){
        glClearColor(color1, color2, color3, alpha); }
    protected void setClearColor(int color1, int color2, int color3, float alpha){
        glClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha); }
    protected void setClearColor(float color1, float color2, float color3){
        glClearColor(color1, color2, color3, 1f);}
    protected void setClearColor(float color, float alpha){
        glClearColor(color, color, color, alpha);}
    protected void setClearColor(float color){
        glClearColor(color, color, color, 1f);}

    // Resources
    protected String[] getInvolvedBatch(){
        return new String[]{};
    }

    public void unload() {
        // The scene can be left while it is still loading
        if (loadingScene != null) {
            loadingScene.unload();
            loadingScene = null;
        }

        if (scRenderer != null)
            scRenderer.unload();

        for (IUpdatable updatable : updatables)
            updatable.unload();

        for (IDisplayable displayable : displayables)
            displayable.unload();
    }
}
