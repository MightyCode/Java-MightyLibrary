package MightyLibrary.mightylib.graphics.GUI;

public abstract class GUI {

    protected boolean userSelect;

    public void userSelect(boolean state){
        this.userSelect = state;
    }

    public abstract boolean GUISelected();

    public abstract void display();

    public abstract boolean mouseDeableIt();

    public abstract void unload();
}
