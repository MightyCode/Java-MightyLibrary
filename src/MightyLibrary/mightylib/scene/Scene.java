package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.graphics.shape._2D.VirtualSceneRenderer;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.Color4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;

public class Scene {
    protected final Resources resources;
    protected final ShaderManager shaderManager;
    protected final Context mainContext;
    protected Camera mainCamera;

    protected SceneManagerInterface sceneManagerInterface;
    private VirtualSceneRenderer scRenderer;

    public Scene(CameraCreationInfo info){
        resources = Resources.getInstance();
        shaderManager = ShaderManager.getInstance();
        mainContext = ContextManager.getInstance().getMainContext();

        if (info == null){
            CameraCreationInfo cci = new CameraCreationInfo();
            cci.fov = 120f;
            cci.initialPosition = new Vector3f(0, 0, 0);
        }

        mainCamera = mainContext.createCamera(info);
        sceneManagerInterface = null;
    }

    public Scene(){
        this(null);
    }

    public void init(String[] args){
        shaderManager.reloadProjection(mainCamera);
        dispose();

        scRenderer = new VirtualSceneRenderer(mainContext.getWindow().getInfo());
        scRenderer.setTexturePosition(new Vector4f(0, 1, 1, 0));
        scRenderer.updateShape();
    }

    public void setSceneManagerInterface(SceneManagerInterface sceneManagerInterface){
        this.sceneManagerInterface = sceneManagerInterface;
    }

    public void update(){}

    public void dispose(){
        shaderManager.dispose(mainCamera);
    }


    public void display(){}


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
    protected void setClearColor(float color1, float color2, float color3, float alpha){    glClearColor(color1, color2, color3, alpha); }
    protected void setClearColor(int color1, int color2, int color3, float alpha){          glClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha); }
    protected void setClearColor(float color1, float color2, float color3){                 glClearColor(color1, color2, color3, 1f);}
    protected void setClearColor(float color, float alpha){                                 glClearColor(color, color, color, alpha);}
    protected void setClearColor(float color){                                              glClearColor(color, color, color, 1f);}

    public void unload() {
        scRenderer.unload();
    }
}
