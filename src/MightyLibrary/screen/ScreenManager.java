package MightyLibrary.screen;

import MightyLibrary.inputs.InputManager;
import MightyLibrary.inputs.KeyboardManager;
import MightyLibrary.inputs.MouseManager;
import MightyLibrary.main.Window;
import MightyLibrary.render.texture.TextureManager;
import MightyLibrary.screen.screens.TestScreen;
import MightyLibrary.screen.screens.Screen;
import MightyLibrary.render.shader.ShaderManager;
import MightyLibrary.util.ManagerContainer;
import org.joml.Vector3f;

public class ScreenManager {
    private ManagerContainer manContainer;

    private Screen currentScreen;

    private boolean wantChange;
    private String[] changeArgs;
    private String newScreenName = "";

    private Window window;

    public ScreenManager(Window window, ManagerContainer manContainer){
        this.window = window;
        this.manContainer = manContainer;

        manContainer.setManager(this);
        manContainer.setManager(new MouseManager(this.manContainer.wParams)).setManager( new KeyboardManager(this.manContainer.wParams));
        manContainer.setManager(new InputManager(this.manContainer.keyManager,this. manContainer.mouseManager, new int[][]{{78, 89},{0, 0}}));

        manContainer.setManager(new Camera(manContainer, 70f, new Vector3f(0.0f, 0.0f, 10.0f)));

        manContainer.setManager(new ShaderManager(manContainer.cam));

        manContainer.setManager(new TextureManager());
        // Load every texture
        manContainer.texManager.init();

        setNewScreen("testScreen", new String[]{""});
        changeScreen();
    }

    public void update(){
        if(wantChange){
            changeScreen();
        }

        currentScreen.update();
    }

    public void display(){
        currentScreen.display();
    }

    public void dispose(){
        manContainer.shadManager.dispose();
        manContainer.inpManager.dispose();
    }

    public void setNewScreen(String screenName, String[] args){
        this.newScreenName = screenName;
        this.changeArgs = args;
        this.wantChange = true;
    }

    private void changeScreen(){
        if(currentScreen != null){
            currentScreen.unload();
        }

        if(newScreenName.equals("menuscreen") || newScreenName.equals("menuScreen") || newScreenName.equals("MenuScreen")){
            //currentScreen = new MenuScreeen(manContainer, changeArgs);
        }

        if(newScreenName.equals("testscreen") || newScreenName.equals("testScreen") || newScreenName.equals("TestScreen")){
            currentScreen = new TestScreen(manContainer, changeArgs);
        }

        assert currentScreen != null;
        currentScreen.init();

        wantChange = false;
        newScreenName = "";
    }

    public void exit(){
        window.exit();
    }

    public void unload(){
        currentScreen.unload();
        manContainer.shadManager.unload();
        manContainer.texManager.unload();
    }
}
