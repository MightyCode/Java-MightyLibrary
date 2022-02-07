package MightyLibrary.project.main;

import static org.lwjgl.glfw.GLFW.*;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.project.lib.ActionId;

class ProjectLoading {
    static void ContextLoading(Context context){
        InputManager inputManager = context.getInputManager();
        int[][] inputData = {
                { ActionId.ESCAPE, GLFW_KEY_ESCAPE , InputManager.ID_KEYBOARD },
                { ActionId.MOVE_LEFT, GLFW_KEY_A , InputManager.ID_KEYBOARD },
                { ActionId.MOVE_RIGHT, GLFW_KEY_D , InputManager.ID_KEYBOARD },
                { ActionId.MOVE_FORWARD, GLFW_KEY_W , InputManager.ID_KEYBOARD },
                { ActionId.MOVE_BACKWARD, GLFW_KEY_S , InputManager.ID_KEYBOARD },
                { ActionId.MOVE_UP, GLFW_KEY_SPACE , InputManager.ID_KEYBOARD },
                { ActionId.MOVE_DOWN, GLFW_KEY_LEFT_CONTROL , InputManager.ID_KEYBOARD },
                { ActionId.SHIFT, GLFW_KEY_LEFT_SHIFT , InputManager.ID_KEYBOARD },
                { ActionId.ENTER, GLFW_KEY_ENTER , InputManager.ID_KEYBOARD },
                { ActionId.SELECT_UP, GLFW_KEY_W , InputManager.ID_KEYBOARD },
                { ActionId.SELECT_DOWN, GLFW_KEY_S , InputManager.ID_KEYBOARD },
                { ActionId.LEFT_CLICK, GLFW_MOUSE_BUTTON_1 , InputManager.ID_MOUSE },
                { ActionId.RIGHT_CLICK, GLFW_MOUSE_BUTTON_1 , InputManager.ID_MOUSE }
        };

        inputManager.init(inputData);
    }
}
