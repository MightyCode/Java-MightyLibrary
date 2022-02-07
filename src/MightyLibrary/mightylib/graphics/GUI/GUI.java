package MightyLibrary.mightylib.graphics.GUI;

public abstract class GUI {

    private boolean forceSelect;

    public void forceSelect(boolean state){
        this.forceSelect = state;
    }

    boolean forceSelected() { return forceSelect; }

    public abstract boolean GUIMouseSelected();

    public abstract void display();

    public abstract boolean mouseDisableIt();

    public abstract void unload();
}
