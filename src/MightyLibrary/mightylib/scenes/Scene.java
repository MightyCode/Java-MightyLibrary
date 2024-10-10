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

import java.util.ArrayList;

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

    private final ArrayList<IUpdatable> updatables = new ArrayList<>();
    private final ArrayList<IDisplayable> displayables = new ArrayList<>();

    private TemplateSceneLoading loadingScene = null; // can be null
    private boolean isWaitingForLoading = false;

    protected String[] args;

    private Camera3DCreationInfo cameraParameters;

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

    public abstract void init(String[] args);

    public void init(String[] args, TemplateSceneLoading loadingScene) {
        this.args = args;
        this.loadingScene = loadingScene;
        this.loadingScene.launch(this.args);
    }

    public void launch(String[] args, IGLBindable bindable) {
        initGlobalParameters();

        scRenderer = new VirtualSceneRenderer(mainContext.getWindow().getInfo(), bindable);
        scRenderer.load(0);
    }

    public void launch(String[] args) {
        launch(args, new BasicBindableObject().setQualityTexture(TextureParameters.REALISTIC_PARAMETERS));
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


    public void setWaitForLoading() {
        isWaitingForLoading = true;
    }

    public void setSceneManagerInterface(SceneManagerInterface sceneManagerInterface) {
        this.sceneManagerInterface = sceneManagerInterface;
    }

    final void updateByManager(){
        if (isWaitingForLoading) {
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

    void disposeByManager(){
        if (isWaitingForLoading) {
            loadingScene.disposeByManager();
        } else {
            dispose();
        }
    }

    public void dispose() {
        for (IUpdatable updatable : updatables) {
            updatable.dispose();
        }
    }

    final void displayByManager() {
        if (isWaitingForLoading) {
            loadingScene.displayByManager();

            if (loadingScene.isFinished()) {
                isWaitingForLoading = false;
                this.launch(args);
            }
        } else {
            display();
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
        if (scRenderer != null)
            scRenderer.unload();

        for (IUpdatable updatable : updatables)
            updatable.unload();

        for (IDisplayable displayable : displayables)
            displayable.unload();
    }
}
