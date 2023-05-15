package MightyLibrary.project.lib;
import MightyLibrary.mightylib.inputs.InputManager;

public class ActionId {
    public static final int MOVE_RIGHT = InputManager.getAndIncrementId();
    public static final int MOVE_LEFT = InputManager.getAndIncrementId();
    public static final int MOVE_FORWARD = InputManager.getAndIncrementId();
    public static final int MOVE_BACKWARD = InputManager.getAndIncrementId();
    public static final int MOVE_UP = InputManager.getAndIncrementId();
    public static final int MOVE_DOWN = InputManager.getAndIncrementId();
    public static final int SHIFT = InputManager.getAndIncrementId();
    public static final int LEFT_CLICK = InputManager.getAndIncrementId();
    public static final int RIGHT_CLICK = InputManager.getAndIncrementId();
    public static final int ESCAPE = InputManager.getAndIncrementId();
    public static final int ENTER = InputManager.getAndIncrementId();

    public static final int SELECT_UP = InputManager.getAndIncrementId();
    public static final int SELECT_DOWN = InputManager.getAndIncrementId();
}
