package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.scenes.camera.Camera2D;

public abstract class GUI {
    protected Camera2D referenceCamera;
    private boolean forceSelect;

    public GUI(Camera2D referenceCamera){
        this.referenceCamera = referenceCamera;
    }

    public GUI(){
        this(ShaderManager.getInstance().getMainCamera2D());
    }

    public void forceSelect(boolean state){
        this.forceSelect = state;
        this.forceUnselect = false;
    }

    boolean forceSelected() { return forceSelect; }

    private boolean forceUnselect;

    public void forceUnselect(boolean state){
        this.forceSelect = true;
        this.forceUnselect = state;
    }

    boolean forceUnselected() { return forceUnselect; }

    public abstract boolean GUIMouseSelected();

    public abstract void display();

    public abstract boolean mouseDisableIt();

    public abstract void unload();
}
