package MightyLibrary.mightylib.inputs;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.main.Window;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.glfwGetKey;

/**
 * This class is the keyboard manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class KeyboardManager {

    public Window window;

    private static final int KEYS = 348;
    private static final int BEGIN_KEYS = 32;
    private final boolean[] state = new boolean[KEYS];
    private final boolean[] oldState = new boolean[KEYS];

    /**
     * Keyboard manager class.
     * Instance the class.
     */
    public KeyboardManager(){
        this.window = ManagerContainer.getInstance().window;
        Arrays.fill(state, false);
        Arrays.fill(oldState, false);
    }

    public boolean getKeyState(int keyID){
        if(keyID >= BEGIN_KEYS && keyID <= 348) return state[keyID];
        return false;
    }

    private boolean testState(int keyID){
        return glfwGetKey(window.windowId, keyID) == 1;
    }

    /**
     * Test if the key has just been pressed.
     *
     * @param keyID Key's ID.
     * @return boolean
     */
    public boolean keyPressed(int keyID){
        return (state[keyID] && !oldState[keyID]);
    }

    /**
     * Test if the key has just been released.
     *
     * @param keyID Key's ID.
     * @return boolean
     */
    public boolean keyReleased(int keyID){
        return (!state[keyID] && oldState[keyID]);
    }

    public void dispose(){
        for(int key = BEGIN_KEYS; key < state.length; key++){
            oldState[key] = state[key];
            state[key] = testState(key);
        }
    }
}