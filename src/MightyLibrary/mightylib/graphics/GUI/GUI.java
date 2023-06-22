package MightyLibrary.mightylib.graphics.GUI;

public abstract class GUI {

    private boolean forceSelect;

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
