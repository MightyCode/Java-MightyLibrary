package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.main.Window;
import MightyLibrary.mightylib.render.shape._2D.VirtualSceneRenderer;
import MightyLibrary.mightylib.util.math.Color4f;

import static org.lwjgl.opengl.GL11.*;

public class Scene {
    protected ManagerContainer manContainer;
    protected Window window;
    protected VirtualSceneRenderer scRenderer;

    public Scene(){
        this.manContainer = ManagerContainer.getInstance();
        window = manContainer.window;
        scRenderer = new VirtualSceneRenderer();
    }


    public void init(String[] args){}


    public void update(){}


    public void display(){}


    public void setVirtualScene(){
        scRenderer.bindFrameBuff();
        manContainer.window.setVirtualViewport();
    }

    public void setAndDisplayRealScene(){
        scRenderer.unbindFrameBuff();
        manContainer.window.setRealViewport();

        scRenderer.display();
    }

    protected void clear(){ glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); }

    protected void setClearColor(Color4f color) { glClearColor(color.getR(), color.getG(), color.getB(), color.getA());}
    protected void setClearColor(float color1, float color2, float color3, float alpha){    glClearColor(color1, color2, color3, alpha); }
    protected void setClearColor(int color1, int color2, int color3, float alpha){          glClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha); }
    protected void setClearColor(float color1, float color2, float color3){                 glClearColor(color1, color2, color3, 1f);}
    protected void setClearColor(float color, float alpha){                                 glClearColor(color, color, color, alpha);}
    protected void setClearColor(float color){                                              glClearColor(color, color, color, 1f);}

    public void setScreen(String newScreen, String args){}

    public void unload() {
        scRenderer.unload();
    }
}