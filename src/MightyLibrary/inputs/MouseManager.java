package MightyLibrary.inputs;

import MightyLibrary.main.WindowParams;
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

    private WindowParams wParams;

    private static final int MOUSE_BUTTONS = 7;
    private final boolean[] state = new boolean[MOUSE_BUTTONS];
    private final boolean[] oldState = new boolean[MOUSE_BUTTONS];

    public Vector2f pos;
    public Vector2f oldPos;

    public Vector2f relativePos;
    public Vector2f relativeOldPos;

    /**
     * Mouse manager class.
     * Instance the class
     */
    public MouseManager(WindowParams wParams){
        pos = new Vector2f(0);
        oldPos = new Vector2f(0);
        relativePos = new Vector2f(0);
        relativeOldPos = new Vector2f(0);

        this.wParams = wParams;

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
        return glfwGetMouseButton(wParams.windowId, buttonId) == 1;
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

        glfwGetCursorPos(wParams.windowId, x, y);
        pos.set((float)x.get(0), (float)y.get(0));
    }

    public float posX(){
        return pos.x;
    }
    public float posY(){
        return pos.y;
    }

    public Vector2f getPos(){return new Vector2f(pos);}

    public void dispose(){
        oldPos.set(pos);
        mouseUpdate();

        relativeOldPos = new Vector2f(relativePos);
        relativePos.x = posX() / wParams.size.x;
        relativePos.y = posY() / wParams.size.y;

        for(int key = 0; key < state.length; key++){
            oldState[key] = state[key];
            state[key] = getState(key);
        }
    }

    // Glfw settings
    public void setCursor(boolean state){
        if(state)glfwSetInputMode(wParams.windowId, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        else glfwSetInputMode(wParams.windowId, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }
}