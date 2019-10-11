package MightyLibrary.util;

import MightyLibrary.inputs.InputManager;
import MightyLibrary.inputs.KeyboardManager;
import MightyLibrary.inputs.MouseManager;
import MightyLibrary.main.WindowParams;
import MightyLibrary.render.texture.TextureManager;
import MightyLibrary.screen.Camera;
import MightyLibrary.screen.ScreenManager;
import MightyLibrary.render.shader.ShaderManager;

public class ManagerContainer {

    public WindowParams wParams;

    public InputManager inpManager;
    public KeyboardManager keyManager;
    public MouseManager mouseManager;

    public Camera cam;

    public ScreenManager screenManager;

    public ShaderManager shadManager;
    public TextureManager texManager;


    public ManagerContainer(){}

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

    public ManagerContainer setManager(WindowParams wParams){
        this.wParams = wParams;
        return this;
    }

    public ManagerContainer setManager(ScreenManager scManager){
        this.screenManager = scManager;
        return this;
    }

    public ManagerContainer setManager(ShaderManager shadManager){
        this.shadManager = shadManager;
        return this;
    }

    public ManagerContainer setManager(TextureManager textureManager){
        this.texManager = textureManager;
        return this;
    }
}
