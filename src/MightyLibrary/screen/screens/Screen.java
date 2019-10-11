package MightyLibrary.screen.screens;

import MightyLibrary.util.ManagerContainer;

public class Screen {
    private boolean isATrueScreenScreen;
    protected ManagerContainer manContainer;

    public Screen(){
       isATrueScreenScreen = false;
    }

    public Screen(ManagerContainer manContainer){
        this.manContainer = manContainer;
        isATrueScreenScreen = true;
    }

    public void init(){}

    public void display(){}

    public void update(){}

    public void setScreen(String newScreen, String args){}

    public void unload() { }
}
