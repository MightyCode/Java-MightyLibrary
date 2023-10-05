package MightyLibrary.project.main;

import static org.lwjgl.glfw.GLFW.*;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.inputType.ActionInput;
import MightyLibrary.mightylib.inputs.inputType.EInputType;
import MightyLibrary.mightylib.inputs.inputType.InputSimple;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.procedures.IProjectLoading;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.map.TileMapLoader;
import MightyLibrary.mightylib.resources.map.TileSetLoader;
import MightyLibrary.project.lib.ActionId;

class ProjectLoading implements IProjectLoading {
    @Override
    public void init(){
        Resources resources = Resources.getInstance();
        resources.Loaders.add(new TileSetLoader());
        resources.Loaders.add(new TileMapLoader());
    }

    @Override
    public void contextLoading(Context context){
        InputManager inputManager = context.getInputManager();

        ActionInput[] configurations = {
                new ActionInput(ActionId.ESCAPE, "ESCAPE", new InputSimple(GLFW_KEY_ESCAPE, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_LEFT, "ESCAPE", new InputSimple(GLFW_KEY_A, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_RIGHT, "ESCAPE", new InputSimple(GLFW_KEY_D, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_FORWARD, "ESCAPE", new InputSimple(GLFW_KEY_W, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_BACKWARD, "ESCAPE", new InputSimple(GLFW_KEY_S, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_UP, "ESCAPE", new InputSimple(GLFW_KEY_SPACE, EInputType.Keyboard)),
                new ActionInput(ActionId.MOVE_DOWN, "ESCAPE", new InputSimple(GLFW_KEY_LEFT_CONTROL, EInputType.Keyboard)),
                new ActionInput(ActionId.SHIFT, "ESCAPE", new InputSimple(GLFW_KEY_LEFT_SHIFT, EInputType.Keyboard)),
                new ActionInput(ActionId.ENTER, "ESCAPE", new InputSimple(GLFW_KEY_ENTER, EInputType.Keyboard)),
                new ActionInput(ActionId.SELECT_UP, "ESCAPE", new InputSimple(GLFW_KEY_W, EInputType.Keyboard)),
                new ActionInput(ActionId.SELECT_DOWN, "ESCAPE", new InputSimple(GLFW_KEY_S, EInputType.Keyboard)),
                new ActionInput(ActionId.LEFT_CLICK, "ESCAPE", new InputSimple(GLFW_MOUSE_BUTTON_1, EInputType.Mouse)),
                new ActionInput(ActionId.RIGHT_CLICK, "ESCAPE", new InputSimple(GLFW_MOUSE_BUTTON_2, EInputType.Mouse)),
        };

        inputManager.init(configurations);
    }
}