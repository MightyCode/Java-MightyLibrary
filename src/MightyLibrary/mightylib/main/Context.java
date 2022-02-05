package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.scene.CameraCreationInfo;
import org.joml.Vector3f;

public class Context {
    private final Window window;
    private final InputManager inputManager;
    private final KeyboardManager keyboardManager;
    private final MouseManager mouseManager;


    Context(Window window, InputManager inputManager, KeyboardManager keyboardManager, MouseManager mouseManager){
        this.window = window;
        this.inputManager = inputManager;
        this.keyboardManager = keyboardManager;
        this.mouseManager = mouseManager;
    }

    public Window getWindow() { return window; }
    public InputManager getInputManager() { return inputManager; }
    public KeyboardManager getKeyboardManager() { return keyboardManager; }
    public MouseManager getMouseManager() { return mouseManager; }


    public Camera createCamera(CameraCreationInfo info){
        return new Camera(window.getInfo(), mouseManager, 120f, new Vector3f(0.0f, 0.0f, 10.0f));
    }


    public void dispose(){
        inputManager.dispose();
    }


    public void unload(){
        window.destroyWindow();
    }
}
