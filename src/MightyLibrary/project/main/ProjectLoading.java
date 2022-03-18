package MightyLibrary.project.main;

import static org.lwjgl.glfw.GLFW.*;

import MightyLibrary.mightylib.inputs.ActionConfigurations;
import MightyLibrary.mightylib.inputs.EInputType;
import MightyLibrary.mightylib.inputs.InputConfiguration;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.map.TileMapLoader;
import MightyLibrary.mightylib.resources.map.TileSetLoader;
import MightyLibrary.project.lib.ActionId;

class ProjectLoading {

    static void init(){
        Resources resources = Resources.getInstance();
        resources.Loaders.add(new TileSetLoader());
        resources.Loaders.add(new TileMapLoader());
    }

    static void ContextLoading(Context context){
        InputManager inputManager = context.getInputManager();

        ActionConfigurations[] configurations = {
                new ActionConfigurations(ActionId.ESCAPE, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_ESCAPE, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.MOVE_LEFT, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_A, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.MOVE_RIGHT, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_D, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.MOVE_FORWARD, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_W, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.MOVE_BACKWARD, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_S, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.MOVE_UP, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_SPACE, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.MOVE_DOWN, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_LEFT_CONTROL, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.SHIFT, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_LEFT_SHIFT, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.ENTER, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_ENTER, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.SELECT_UP, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_W, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.SELECT_DOWN, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_S, EInputType.Keyboard)}),
                new ActionConfigurations(ActionId.LEFT_CLICK, new InputConfiguration[]{new InputConfiguration(GLFW_MOUSE_BUTTON_1, EInputType.Mouse)}),
                new ActionConfigurations(ActionId.RIGHT_CLICK, new InputConfiguration[]{new InputConfiguration(GLFW_MOUSE_BUTTON_2, EInputType.Mouse)}),
        };

        inputManager.init(configurations);
    }
}
