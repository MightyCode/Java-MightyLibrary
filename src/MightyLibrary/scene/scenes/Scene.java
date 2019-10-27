package MightyLibrary.scene.scenes;

import MightyLibrary.util.ManagerContainer;
import MightyLibrary.util.math.Color4f;

import static org.lwjgl.opengl.GL11.*;

public class Scene {
    private boolean isATrueSceneScreen;
    protected ManagerContainer manContainer;

    public Scene(){
       isATrueSceneScreen = false;
    }

    public Scene(ManagerContainer manContainer){
        this.manContainer = manContainer;
        isATrueSceneScreen = true;
    }

    public void init(){}

    public void update(){}

    public void display(){}

    protected void clear(){ glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); }

    protected void setClearColor(Color4f color) { glClearColor(color.getR(), color.getG(), color.getB(), color.getA());}
    protected void setClearColor(float color1, float color2, float color3, float alpha){    glClearColor(color1, color2, color3, alpha); }
    protected void setClearColor(int color1, int color2, int color3, float alpha){          glClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha); }
    protected void setClearColor(float color1, float color2, float color3){                 glClearColor(color1, color2, color3, 1f);}
    protected void setClearColor(float color, float alpha){                                 glClearColor(color, color, color, alpha);}
    protected void setClearColor(float color){                                              glClearColor(color, color, color, 1f);}

    public void setScreen(String newScreen, String args){}

    public void unload() { }
}
