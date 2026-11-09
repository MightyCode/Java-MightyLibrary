package io.github.mightycode.mightylib.inputs;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * State of one gamepad slot, seen by a whole tick (computed by GamepadManager.postUpdate).
 * Buttons and axes use the GLFW ids (GLFW_GAMEPAD_BUTTON_A, GLFW_GAMEPAD_AXIS_LEFT_X...), with the standard
 * layout of an Xbox pad whatever the real pad is (mapping of GLFW / SDL_GameControllerDB).
 * Sticks : -1 to 1, y positive downward (like the window coordinates), radial dead zone applied.
 * Triggers : 0 (released) to 1 (fully pressed), dead zone applied.
 * Main thread only.
 */
public class Gamepad {
    public static final int BUTTONS = GLFW_GAMEPAD_BUTTON_LAST + 1;
    public static final int AXES = GLFW_GAMEPAD_AXIS_LAST + 1;

    public static final float DEFAULT_STICK_DEAD_ZONE = 0.2f;
    public static final float DEFAULT_TRIGGER_DEAD_ZONE = 0.1f;

    // No player assigned
    public static final int NO_PLAYER = -1;

    // Names of the GLFW_GAMEPAD_BUTTON_ / GLFW_GAMEPAD_AXIS_ constants without their prefix, indexed by id
    private static final String[] BUTTON_NAMES = {
            "A", "B", "X", "Y", "LEFT_BUMPER", "RIGHT_BUMPER", "BACK", "START", "GUIDE",
            "LEFT_THUMB", "RIGHT_THUMB", "DPAD_UP", "DPAD_RIGHT", "DPAD_DOWN", "DPAD_LEFT"
    };
    private static final String[] PLAYSTATION_BUTTON_NAMES = { "CROSS", "CIRCLE", "SQUARE", "TRIANGLE" };
    private static final String[] AXIS_NAMES = {
            "LEFT_X", "LEFT_Y", "RIGHT_X", "RIGHT_Y", "LEFT_TRIGGER", "RIGHT_TRIGGER"
    };

    public static String buttonName(int button) {
        return BUTTON_NAMES[checkButton(button)];
    }

    public static String axisName(int axis) {
        return AXIS_NAMES[checkAxis(axis)];
    }

    // Every name accepted by parseButton
    public static List<String> buttonNames() {
        List<String> names = new ArrayList<>(Arrays.asList(BUTTON_NAMES));
        names.addAll(Arrays.asList(PLAYSTATION_BUTTON_NAMES));
        return names;
    }

    public static List<String> axisNames() {
        return Arrays.asList(AXIS_NAMES);
    }

    // Name of buttonName (case insensitive), CROSS/CIRCLE/SQUARE/TRIANGLE, or the id
    public static int parseButton(String text) {
        String upper = text.toUpperCase();
        for (int button = 0; button < BUTTONS; ++button) {
            if (BUTTON_NAMES[button].equals(upper))
                return button;
        }

        for (int button = 0; button < PLAYSTATION_BUTTON_NAMES.length; ++button) {
            if (PLAYSTATION_BUTTON_NAMES[button].equals(upper))
                return button;
        }

        try {
            return checkButton(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown gamepad button ->" + text + "<-, use one of " + String.join(", ", BUTTON_NAMES));
        }
    }

    // Name of axisName (case insensitive) or the id
    public static int parseAxis(String text) {
        String upper = text.toUpperCase();
        for (int axis = 0; axis < AXES; ++axis) {
            if (AXIS_NAMES[axis].equals(upper))
                return axis;
        }

        try {
            return checkAxis(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown gamepad axis ->" + text + "<-, use one of " + String.join(", ", AXIS_NAMES));
        }
    }

    private final int id;
    private final boolean virtual;

    private boolean connected;
    private boolean connectedThisTick;
    private boolean disconnectedThisTick;
    private String name;
    private String guid;
    // False for a joystick without a gamepad mapping : it is connected but reports nothing
    private boolean mapped;

    int player;

    private final boolean[] buttons = new boolean[BUTTONS];
    private final boolean[] oldButtons = new boolean[BUTTONS];
    // Axes after the dead zones
    private final float[] axes = new float[AXES];
    private final float[] oldAxes = new float[AXES];

    private float stickDeadZone = DEFAULT_STICK_DEAD_ZONE;
    private float triggerDeadZone = DEFAULT_TRIGGER_DEAD_ZONE;

    // Simulated buttons and axes (commands, scripts, virtual gamepads), applied at the next update
    private final InputSimulation buttonSimulation = new InputSimulation();
    private final InputSimulation axisSimulation = new InputSimulation();
    private final Map<Integer, Float> pendingAxisValues = new HashMap<>();
    private final float[] simulatedAxisValues = new float[AXES];
    // Axes before the dead zones, reused by each update
    private final float[] source = new float[AXES];

    Gamepad(int id, boolean virtual) {
        this.id = id;
        this.virtual = virtual;
        this.player = NO_PLAYER;
        this.name = "";
        this.guid = "";
    }

    // Id of the slot : GLFW joystick id (GLFW_JOYSTICK_1...) or above GLFW_JOYSTICK_LAST for a virtual gamepad
    public int getId() { return id; }
    public boolean isVirtual() { return virtual; }

    public boolean isConnected() { return connected; }
    public boolean connectedThisTick() { return connectedThisTick; }
    public boolean disconnectedThisTick() { return disconnectedThisTick; }
    public boolean isMapped() { return mapped; }
    public String getName() { return name; }
    public String getGuid() { return guid; }

    // Player of the gamepad, NO_PLAYER if none
    public int getPlayer() { return player; }

    public float getStickDeadZone() { return stickDeadZone; }
    public float getTriggerDeadZone() { return triggerDeadZone; }

    public Gamepad setStickDeadZone(float deadZone) {
        stickDeadZone = checkDeadZone(deadZone);
        return this;
    }

    public Gamepad setTriggerDeadZone(float deadZone) {
        triggerDeadZone = checkDeadZone(deadZone);
        return this;
    }

    private static float checkDeadZone(float deadZone) {
        if (deadZone < 0 || deadZone >= 1)
            throw new IllegalArgumentException("Dead zone must be in [0, 1[ : " + deadZone);
        return deadZone;
    }

    // Called by the manager when the connection is known, the state applies from the next update
    void connect(String name, String guid, boolean mapped) {
        this.connected = true;
        this.connectedThisTick = true;
        this.name = name == null ? "" : name;
        this.guid = guid == null ? "" : guid;
        this.mapped = mapped;
    }

    void disconnect() {
        connected = false;
        disconnectedThisTick = true;
    }

    // End of the tick : connection flags last one tick
    void clearConnectionFlags() {
        connectedThisTick = false;
        disconnectedThisTick = false;
    }

    /**
     * Compute the state of the next tick.
     * @param rawButtons buttons read from the device, null if nothing is read (disconnected, unmapped, virtual)
     * @param rawAxes axes read from the device (GLFW ranges : triggers from -1 to 1), null if nothing is read
     */
    void update(boolean[] rawButtons, float[] rawAxes) {
        System.arraycopy(buttons, 0, oldButtons, 0, BUTTONS);
        System.arraycopy(axes, 0, oldAxes, 0, AXES);

        buttonSimulation.update();
        axisSimulation.update();
        for (Map.Entry<Integer, Float> pending : pendingAxisValues.entrySet())
            simulatedAxisValues[pending.getKey()] = pending.getValue();
        pendingAxisValues.clear();

        boolean readable = connected && rawButtons != null && rawAxes != null;
        for (int button = 0; button < BUTTONS; ++button)
            buttons[button] = (readable && rawButtons[button]) || buttonSimulation.getState(button);

        if (readable) {
            System.arraycopy(rawAxes, 0, source, 0, AXES);
        } else {
            Arrays.fill(source, 0);
            // Released triggers
            source[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = -1;
            source[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = -1;
        }

        // Simulated values replace the real ones, given in the ranges of the getters (triggers from 0 to 1)
        for (int axis = 0; axis < AXES; ++axis) {
            if (axisSimulation.getState(axis))
                source[axis] = isTrigger(axis) ? simulatedAxisValues[axis] * 2 - 1 : simulatedAxisValues[axis];
        }

        applyStickDeadZone(source, GLFW_GAMEPAD_AXIS_LEFT_X, GLFW_GAMEPAD_AXIS_LEFT_Y);
        applyStickDeadZone(source, GLFW_GAMEPAD_AXIS_RIGHT_X, GLFW_GAMEPAD_AXIS_RIGHT_Y);
        axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = applyTriggerDeadZone(source[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER]);
        axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = applyTriggerDeadZone(source[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER]);
    }

    // Radial dead zone, the rest rescaled to start from 0 : no jump at the border, diagonals kept
    private void applyStickDeadZone(float[] source, int axisX, int axisY) {
        float x = source[axisX];
        float y = source[axisY];
        float length = (float) Math.sqrt(x * x + y * y);

        if (length <= stickDeadZone) {
            axes[axisX] = 0;
            axes[axisY] = 0;
            return;
        }

        float scaled = Math.min(1, (length - stickDeadZone) / (1 - stickDeadZone));
        axes[axisX] = x / length * scaled;
        axes[axisY] = y / length * scaled;
    }

    // GLFW trigger from -1 to 1, returned from 0 to 1
    private float applyTriggerDeadZone(float raw) {
        float value = Math.max(0, Math.min(1, (raw + 1) / 2));
        if (value <= triggerDeadZone)
            return 0;

        return (value - triggerDeadZone) / (1 - triggerDeadZone);
    }

    public static boolean isTrigger(int axis) {
        return axis == GLFW_GAMEPAD_AXIS_LEFT_TRIGGER || axis == GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
    }

    public boolean getButton(int button) {
        return buttons[checkButton(button)];
    }

    public boolean buttonPressed(int button) {
        checkButton(button);
        return buttons[button] && !oldButtons[button];
    }

    public boolean buttonReleased(int button) {
        checkButton(button);
        return !buttons[button] && oldButtons[button];
    }

    public float getAxis(int axis) {
        return axes[checkAxis(axis)];
    }

    public float getOldAxis(int axis) {
        return oldAxes[checkAxis(axis)];
    }

    public Vector2f getLeftStick(Vector2f result) {
        return result.set(axes[GLFW_GAMEPAD_AXIS_LEFT_X], axes[GLFW_GAMEPAD_AXIS_LEFT_Y]);
    }

    public Vector2f getRightStick(Vector2f result) {
        return result.set(axes[GLFW_GAMEPAD_AXIS_RIGHT_X], axes[GLFW_GAMEPAD_AXIS_RIGHT_Y]);
    }

    // True if any button is held or any axis is out of its dead zone : "press any button to join"
    public boolean isAnyInputActive() {
        for (boolean button : buttons) {
            if (button)
                return true;
        }

        for (float axis : axes) {
            if (axis != 0)
                return true;
        }

        return false;
    }

    public boolean anyButtonPressed() {
        for (int button = 0; button < BUTTONS; ++button) {
            if (buttons[button] && !oldButtons[button])
                return true;
        }

        return false;
    }

    // Press a button during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulateButton(int button, int ticks) {
        buttonSimulation.press(checkButton(button), ticks);
    }

    public void simulateButtonRelease(int button) {
        buttonSimulation.release(checkButton(button));
    }

    /**
     * Set an axis during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick, in the range
     * of getAxis (sticks from -1 to 1, triggers from 0 to 1). The dead zones still apply.
     */
    public void simulateAxis(int axis, float value, int ticks) {
        checkAxis(axis);
        float min = isTrigger(axis) ? 0 : -1;
        if (value < min || value > 1)
            throw new IllegalArgumentException("Axis value must be between " + min + " and 1 : " + value);

        axisSimulation.press(axis, ticks);
        pendingAxisValues.put(axis, value);
    }

    public void simulateAxisRelease(int axis) {
        axisSimulation.release(checkAxis(axis));
    }

    private static int checkButton(int button) {
        if (button < 0 || button >= BUTTONS)
            throw new IllegalArgumentException("Gamepad button must be between 0 and " + (BUTTONS - 1) + " : " + button);
        return button;
    }

    private static int checkAxis(int axis) {
        if (axis < 0 || axis >= AXES)
            throw new IllegalArgumentException("Gamepad axis must be between 0 and " + (AXES - 1) + " : " + axis);
        return axis;
    }

    @Override
    public String toString() {
        return "Gamepad " + id + (virtual ? " (virtual)" : "") + " : " + name
                + (player == NO_PLAYER ? "" : ", player " + player);
    }
}
