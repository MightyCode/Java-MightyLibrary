package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.graphics.shape.font.TextManager;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.scene.SceneManager;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;

public class ManagerContainer {

    public Window window;

    public InputManager inpManager;
    public KeyboardManager keyManager;
    public MouseManager mouseManager;

    public Camera cam;

    public SceneManager sceneManager;

    public ShaderManager shadManager;
    public Resources resources;
    public TextManager textManager;


    // Singleton pattern
    private static ManagerContainer manContainer;

    public static ManagerContainer getInstance(){
        if(manContainer == null) manContainer = new ManagerContainer();
        return manContainer;
    }

    private ManagerContainer() {
    }

    public ManagerContainer setManager(InputManager im){
        inpManager = im;
        return this;
    }

    public ManagerContainer setManager(KeyboardManager km){
        keyManager = km;
        return this;
    }

    public ManagerContainer setManager(MouseManager mm){
        mouseManager = mm;
        return this;
    }

    public ManagerContainer setManager(Camera cam){
        this.cam = cam;
        return this;
    }

    public ManagerContainer setManager(Window window){
        this.window = window;
        return this;
    }

    public ManagerContainer setManager(SceneManager scManager){
        this.sceneManager = scManager;
        return this;
    }

    public ManagerContainer setManager(ShaderManager shadManager){
        this.shadManager = shadManager;
        return this;
    }


    public ManagerContainer setManager(TextManager textManager){
        this.textManager = textManager;
        return this;
    }
}
