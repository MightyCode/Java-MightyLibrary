package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.KeyboardManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;

public class Context {
    private final Window window;
    private final InputManager inputManager;
    private final KeyboardManager keyboardManager;
    private final MouseManager mouseManager;

    private final SystemInfo systemInfo;

    Context(Window window,
            InputManager inputManager,
            KeyboardManager keyboardManager,
            MouseManager mouseManager){

        this.window = window;
        this.inputManager = inputManager;
        this.keyboardManager = keyboardManager;
        this.mouseManager = mouseManager;

        this.systemInfo = new SystemInfo(this.window.getInfo());
    }

    public Window getWindow() { return window; }

    public InputManager getInputManager() { return inputManager; }
    public KeyboardManager getKeyboardManager() { return keyboardManager; }
    public MouseManager getMouseManager() { return mouseManager; }

    public SystemInfo getSystemInfo() { return systemInfo; }


    public Camera3D createCamera(Camera3DCreationInfo info){
        return new Camera3D(window.getInfo(), info.fov, info.initialPosition);
    }


    public void postUpdate(){
        inputManager.postUpdate();
    }


    public void unload(){
        window.destroyWindow();
    }
}
