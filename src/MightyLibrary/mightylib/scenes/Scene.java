package MightyLibrary.mightylib.scenes;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.main.utils.*;
import MightyLibrary.mightylib.resources.texture.BasicBindableObject;
import MightyLibrary.mightylib.resources.texture.IGLBindable;
import MightyLibrary.mightylib.resources.texture.TextureParameters;
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

public class Scene extends UUID implements IUpdatableDisplayable {
    protected final Resources resources;
    protected final ShaderManager shaderManager;
    protected final Context mainContext;
    protected Camera3D main3DCamera;
    protected Camera2D main2DCamera;
    protected SceneManagerInterface sceneManagerInterface;
    private VirtualSceneRenderer scRenderer;

    private final ArrayList<IUpdatable> updatables = new ArrayList<>();
    private final ArrayList<IDisplayable> displayables = new ArrayList<>();

    private TemplateSceneLoading loadingScene = null; // can be null

    public Scene(Camera3DCreationInfo info) {
        resources = Resources.getInstance();
        shaderManager = ShaderManager.getInstance();
        mainContext = ContextManager.getInstance().getMainContext();

        Camera3DCreationInfo parameters;

        if (info == null) {
            parameters = new Camera3DCreationInfo();
            parameters.fov = 120f;
            parameters.initialPosition = new Vector3f(0, 0, 0);
        } else {
            parameters = info;
        }

        main3DCamera = mainContext.createCamera(parameters);
        main2DCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0,0));
        shaderManager.setCameras(main2DCamera, main3DCamera);

        sceneManagerInterface = null;
    }

    public Scene(){
        this(null);
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

    protected void setLoadingScene(TemplateSceneLoading loadingScene){
        this.loadingScene = loadingScene;
    }

    public void init(String[] args, IGLBindable bindable) {
        dispose();

        scRenderer = new VirtualSceneRenderer(mainContext.getWindow().getInfo(), bindable);
        scRenderer.load(0);
    }

    public void init(String[] args) {
        init(args, new BasicBindableObject().setQualityTexture(TextureParameters.REALISTIC_PARAMETERS));
    }

    public void setSceneManagerInterface(SceneManagerInterface sceneManagerInterface) {
        this.sceneManagerInterface = sceneManagerInterface;
    }

    final void updateByManager(){
        if (loadingScene != null) {
            loadingScene.updateByManager();
            if (loadingScene.isFinished()) {
                loadingScene.displayByManager();
                loadingScene = null;
            }
        } else {
            update();
        }
    }

    public void update() {
        for (IUpdatable updatable : updatables)
            updatable.update();
    }

    public void disposeByManager(){
        if (loadingScene != null) {
            loadingScene.displayByManager();
            loadingScene = null;
        } else {
            dispose();
        }
    }

    public void dispose() {
        for (IUpdatable updatable : updatables) {
            updatable.dispose();
        }
    }

    final void displayByManager(){
        if (loadingScene != null) {
            loadingScene.disposeByManager();
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
        dispose();
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
        scRenderer.unload();

        for (IUpdatable updatable : updatables)
            updatable.unload();

        for (IDisplayable displayable : displayables)
            displayable.unload();
    }
}
