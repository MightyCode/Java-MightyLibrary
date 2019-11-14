package MightyLibrary.mightylib.inputs;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.main.Window;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;

/**
 * This class is the mouse manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class MouseManager {

    private Window window;

    private static final int MOUSE_BUTTONS = 7;
    private final boolean[] state = new boolean[MOUSE_BUTTONS];
    private final boolean[] oldState = new boolean[MOUSE_BUTTONS];

    private Vector2f pos;
    private Vector2f oldPos;

    private Vector2f relativePos;
    private Vector2f relativeOldPos;

    private boolean displayCursor;

    /**
     * Mouse manager class.
     * Instance the class
     */
    public MouseManager(){
        pos = new Vector2f(0);
        oldPos = new Vector2f(0);
        relativePos = new Vector2f(0);
        relativeOldPos = new Vector2f(0);

        this.window = ManagerContainer.getInstance().window;

        Arrays.fill(state, false);
        Arrays.fill(oldState, false);
        mouseUpdate();
        dispose();
    }

    public boolean getButton(int buttonId){
        if(buttonId < 7){
            return state[buttonId];
        }
        return false;
    }

    public boolean getState(int buttonId){
        return glfwGetMouseButton(window.windowId, buttonId) == 1;
    }

    public boolean buttonPressed(int buttonID){
        return state[buttonID] && !oldState[buttonID];
    }

    public boolean buttonReleased(int buttonID){
        return !state[buttonID] && oldState[buttonID];
    }

    public void mouseUpdate(){
        DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer y = BufferUtils.createDoubleBuffer(1);

        glfwGetCursorPos(window.windowId, x, y);
        pos.set((float)x.get(0), (float)y.get(0));
    }

    public float posX(){
        return pos.x;
    }
    public float posY(){
        return pos.y;
    }

    public Vector2f pos(){return new Vector2f(pos);}

    public float oldPosX() { return oldPos.x;}
    public float oldPosY() { return oldPos.y;}

    public Vector2f oldPos() { return new Vector2f(oldPos);}

    public void dispose(){
        oldPos.set(pos);
        mouseUpdate();

        relativeOldPos = new Vector2f(relativePos);
        relativePos.x = posX() / window.size.x;
        relativePos.y = posY() / window.size.y;

        for(int key = 0; key < state.length; key++){
            oldState[key] = state[key];
            state[key] = getState(key);
        }
    }

    public void invertCursorState(){
        setCursor(!displayCursor);
    }

    // Glfw settings
    public void setCursor(boolean state){
        this.displayCursor = state;
        glfwSetCursor();
    }

    private void glfwSetCursor(){
        if(displayCursor)glfwSetInputMode(window.windowId, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        else glfwSetInputMode(window.windowId, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    public boolean getCursorState(){
        return displayCursor;
    }
}