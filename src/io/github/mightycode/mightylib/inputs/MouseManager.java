package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.main.WindowInfo;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWScrollCallback;

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

    public static final int MOUSE_BUTTONS = GLFW_MOUSE_BUTTON_LAST + 1;
    private final boolean[] state = new boolean[MOUSE_BUTTONS];
    private final boolean[] oldState = new boolean[MOUSE_BUTTONS];

    private final Vector2f pos;
    // In window pixels, simulation included (getRawPos)
    private final Vector2f windowPos = new Vector2f();
    private final double[] cursorX = new double[1];
    private final double[] cursorY = new double[1];
    private final Vector2f oldPos;

    private final Vector2f relativePos;

    private boolean displayCursor;


    private final Vector2f mouseScroll;

    // Simulated mouse (commands, scripts, later replays), applied at the next postUpdate like real inputs
    private final InputSimulation buttonSimulation;
    // Window pixels, origin at the top left
    private Vector2f pendingSimulatedCursor;
    // Kept until the real cursor moves
    private Vector2f simulatedCursor;
    private double lastRealCursorX, lastRealCursorY;
    private final Vector2f pendingScroll;

    // Cursor movement in window pixels
    private final Vector2f delta = new Vector2f();
    private float lastWindowX = Float.NaN, lastWindowY = Float.NaN;
    private boolean lastWindowSimulated;

    /** For the tests only : a mouse without window, whose positions a subclass provides. */
    protected MouseManager() {
        windowInfo = null;
        pos = new Vector2f(0);
        oldPos = new Vector2f(0);
        relativePos = new Vector2f(0);
        mouseScroll = new Vector2f();
        buttonSimulation = new InputSimulation();
        pendingScroll = new Vector2f();
    }

    /**
     * Mouse manager class.
     * Instance the class
     */
    public MouseManager(WindowInfo windowInfo){
        pos = new Vector2f(0);
        oldPos = new Vector2f(0);
        relativePos = new Vector2f(0);

        this.windowInfo = windowInfo;

        displayCursor = true;

        Arrays.fill(state, false);
        Arrays.fill(oldState, false);

        mouseScroll = new Vector2f();
        buttonSimulation = new InputSimulation();
        pendingScroll = new Vector2f();
        lastRealCursorX = Double.NaN;
        lastRealCursorY = Double.NaN;

        // Freed with the window (Window.destroyWindow)
        glfwSetScrollCallback(windowInfo.getWindowId(), new GLFWScrollCallback() {
            @Override
            public void invoke(final long window, final double xOffset, final double yOffset) {
                // Several scroll events can happen during one tick
                mouseScroll.x += (float) xOffset;
                mouseScroll.y += (float) yOffset;
            }
        });

        mouseUpdate();
        postUpdate();
    }

    public boolean getButton(int buttonId){
        if(!captured && buttonId >= 0 && buttonId < MOUSE_BUTTONS){
            return state[buttonId];
        }
        return false;
    }

    public boolean getState(int buttonId){
        return !captured && realButton(buttonId);
    }

    // The button of the real mouse now (GLFW), whatever the capture
    protected boolean realButton(int buttonId) {
        return glfwGetMouseButton(windowInfo.getWindowId(), buttonId) == GLFW_PRESS;
    }

    // The position of the real cursor now (GLFW), in window pixels
    protected void realCursor(double[] x, double[] y) {
        glfwGetCursorPos(windowInfo.getWindowId(), x, y);
    }

    public boolean buttonPressed(int buttonID){
        return !captured && state[buttonID] && !oldState[buttonID];
    }

    public boolean buttonReleased(int buttonID){
        return !captured && !state[buttonID] && oldState[buttonID];
    }

    // ---- Capture (InputManager.setCaptured): the console owns the mouse, the game sees no button, no scroll,
    // no movement and a cursor out of the window ----
    private static final float NOWHERE = -1_000_000f;
    private boolean captured;

    void setCaptured(boolean captured) { this.captured = captured; }

    public boolean isCaptured() { return captured; }

    // ---- Raw reads, whatever the capture, simulation included: for the owner of the capture only (the console) ----

    // Scroll of the tick
    public Vector2f getRawMouseScroll() { return new Vector2f(mouseScroll); }

    // Position of the cursor in window pixels (origin at the top left, like simulateMove), not the virtual one
    public Vector2f getRawPos() { return new Vector2f(windowPos); }

    public boolean getRawButton(int buttonId) {
        return isButton(buttonId) && state[buttonId];
    }

    public boolean rawButtonPressed(int buttonId) {
        return isButton(buttonId) && state[buttonId] && !oldState[buttonId];
    }

    public boolean rawButtonReleased(int buttonId) {
        return isButton(buttonId) && !state[buttonId] && oldState[buttonId];
    }

    private static boolean isButton(int buttonId) {
        return buttonId >= 0 && buttonId < MOUSE_BUTTONS;
    }

    public void mouseUpdate(){
        realCursor(cursorX, cursorY);
        double realX = cursorX[0];
        double realY = cursorY[0];

        if (pendingSimulatedCursor != null) {
            simulatedCursor = pendingSimulatedCursor;
            pendingSimulatedCursor = null;
        } else if (simulatedCursor != null && (realX != lastRealCursorX || realY != lastRealCursorY)) {
            // The real cursor moved: it takes over again
            simulatedCursor = null;
        }
        lastRealCursorX = realX;
        lastRealCursorY = realY;

        boolean simulated = simulatedCursor != null;
        float windowX = simulated ? simulatedCursor.x : (float) realX;
        float windowY = simulated ? simulatedCursor.y : (float) realY;

        // No movement when the source changes (real / simulated) : it would be a jump
        if (!Float.isNaN(lastWindowX) && simulated == lastWindowSimulated)
            delta.set(windowX - lastWindowX, windowY - lastWindowY);
        else
            delta.set(0);
        lastWindowX = windowX;
        lastWindowY = windowY;
        lastWindowSimulated = simulated;

        windowPos.set(windowX, windowY);
        toVirtual(windowX, windowY, pos);
    }

    /**
     * Movement of the cursor during the last tick in window pixels (y positive downward), independent of the
     * virtual size : for a camera look. Unbounded when the cursor is disabled (setCursor(false)).
     */
    public float deltaX() { return captured ? 0 : delta.x; }
    public float deltaY() { return captured ? 0 : delta.y; }

    // Window pixels (origin at the top left) to the virtual coordinates given by pos()
    private void toVirtual(float x, float y, Vector2f result) {
        // A mouse without window (tests): the same
        if (windowInfo == null) {
            result.set(x, y);
            return;
        }

        if (windowInfo.getRatio() <= windowInfo.getVirtualRatio()){
            float diff = windowInfo.getSizeRef().x * 1f / windowInfo.getVirtualSizeRef().x;

            int size = (int)(windowInfo.getVirtualSizeRef().y * diff);

            result.set((x / diff), ((y - (windowInfo.getSizeRef().y - size) * 0.5f) / diff));
        } else {
            float diff = windowInfo.getSizeRef().y * 1f / windowInfo.getVirtualSizeRef().y;

            int size = (int)(windowInfo.getVirtualSizeRef().x * diff);

            result.set(((x - (windowInfo.getSizeRef().x - size) * 0.5f) / diff), (y / diff));
        }
    }

    /**
     * Move the cursor from the next tick, in window pixels (origin at the top left, like screenshots).
     * The position is kept until the real cursor moves.
     */
    public void simulateMove(float windowX, float windowY) {
        pendingSimulatedCursor = new Vector2f(windowX, windowY);
    }

    public boolean isCursorSimulated() {
        return simulatedCursor != null;
    }

    // Press a button during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulateButton(int buttonId, int ticks) {
        checkButton(buttonId);
        buttonSimulation.press(buttonId, ticks);
    }

    public void simulateButtonRelease(int buttonId) {
        checkButton(buttonId);
        buttonSimulation.release(buttonId);
    }

    // Scroll seen by the next tick (positive y : up)
    public void simulateScroll(float x, float y) {
        pendingScroll.add(x, y);
    }

    private void checkButton(int buttonId) {
        if (buttonId < 0 || buttonId >= MOUSE_BUTTONS)
            throw new IllegalArgumentException("Mouse button must be between 0 and " + (MOUSE_BUTTONS - 1));
    }

    public float posX(){
        return captured ? NOWHERE : pos.x;
    }
    public float posY(){
        return captured ? NOWHERE : pos.y;
    }

    public Vector2f pos(){return new Vector2f(posX(), posY());}

    public float oldPosX() { return captured ? NOWHERE : oldPos.x;}
    public float oldPosY() { return captured ? NOWHERE : oldPos.y;}

    public Vector2f oldPos() { return new Vector2f(oldPosX(), oldPosY());}

    // A blocked mouse (the window waits for a modal window) has no button, no scroll and its cursor is nowhere
    private boolean blocked;

    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public boolean isBlocked() { return blocked; }

    public void postUpdate(){
        oldPos.set(pos);
        mouseUpdate();
        if (blocked) {
            pos.set(-1_000_000f);
            delta.set(0);
        }

        if (windowInfo != null) {
            relativePos.x = pos.x / windowInfo.getSizeRef().x;
            relativePos.y = pos.y / windowInfo.getSizeRef().y;
        }

        buttonSimulation.update();
        for(int key = 0; key < state.length; ++key){
            oldState[key] = state[key];
            // Whatever the capture (the reads hide it): the owner of the capture reads it, and a button held when
            // the capture ends is not pressed again
            state[key] = !blocked && (realButton(key) || buttonSimulation.getState(key));
        }

        // Real scroll events of the next tick are added by the callback
        if (blocked)
            mouseScroll.set(0);
        else
            mouseScroll.set(pendingScroll);
        pendingScroll.set(0);
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
        // Without capture (tests run next to the work of the user) : the cursor stays free, the state is kept
        if(displayCursor || NO_CAPTURE)
            glfwSetInputMode(windowInfo.getWindowId(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        else
            glfwSetInputMode(windowInfo.getWindowId(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    // -Dmightylib.input.noCursorCapture=true : setCursor(false) never captures nor hides the cursor (scenario.py)
    public static final String NO_CAPTURE_PROPERTY = "mightylib.input.noCursorCapture";
    private static final boolean NO_CAPTURE = Boolean.getBoolean(NO_CAPTURE_PROPERTY);

    // True when the cursor is never captured (NO_CAPTURE_PROPERTY)
    public static boolean isCaptureDisabled() { return NO_CAPTURE; }

    public boolean getCursorState(){
        return displayCursor;
    }

    public Vector2f getMouseScroll() { return captured ? new Vector2f() : new Vector2f(mouseScroll); }
}