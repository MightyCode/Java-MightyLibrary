package MightyLibrary.mightylib.inputs;

import MightyLibrary.mightylib.main.WindowInfo;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

import java.nio.DoubleBuffer;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This class is the mouse manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class MouseManager {

    private final WindowInfo windowInfo;

    private static final int MOUSE_BUTTONS = 7;
    private final boolean[] state = new boolean[MOUSE_BUTTONS];
    private final boolean[] oldState = new boolean[MOUSE_BUTTONS];

    private final Vector2f pos;
    private final Vector2f oldPos;

    private final Vector2f relativePos;
    private Vector2f relativeOldPos;

    private boolean displayCursor;

    /**
     * Mouse manager class.
     * Instance the class
     */
    public MouseManager(WindowInfo windowInfo){
        pos = new Vector2f(0);
        oldPos = new Vector2f(0);
        relativePos = new Vector2f(0);
        relativeOldPos = new Vector2f(0);

        this.windowInfo = windowInfo;

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
        return glfwGetMouseButton(windowInfo.getWindowId(), buttonId) == 1;
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

        glfwGetCursorPos(windowInfo.getWindowId(), x, y);
        if (windowInfo.getRatio() <= windowInfo.getVirtualRatio()){
            float diff = windowInfo.getSizeRef().x * 1f / windowInfo.getVirtualSizeRef().x;

            int size = (int)(windowInfo.getVirtualSizeRef().y * diff);

            pos.set(((float)x.get(0) / diff),
                    (((float)y.get(0) - (windowInfo.getSizeRef().y - size) * 0.5f) / diff));
        } else {
            float diff = windowInfo.getSizeRef().y * 1f / windowInfo.getVirtualSizeRef().y;

            int size = (int)(windowInfo.getVirtualSizeRef().x * diff);

            pos.set((((float)x.get(0) - (windowInfo.getSizeRef().x - size) * 0.5f) / diff),
                    ((float)y.get(0) / diff));
        }
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
        relativePos.x = posX() / windowInfo.getSizeRef().x;
        relativePos.y = posY() / windowInfo.getSizeRef().y;

        for(int key = 0; key < state.length; ++key){
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
        if(displayCursor)
            glfwSetInputMode(windowInfo.getWindowId(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        else
            glfwSetInputMode(windowInfo.getWindowId(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    public boolean getCursorState(){
        return displayCursor;
    }
}