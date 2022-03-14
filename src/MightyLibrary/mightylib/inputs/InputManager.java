package MightyLibrary.mightylib.inputs;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This class is the input manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class InputManager {

    private static final int NUMBER_LIBRARY_INPUTS = 2;

    public static final int COMMAND = 0;
    public static final int RELOAD_TEXTURE = 1;

    private static int CURRENT_ID = NUMBER_LIBRARY_INPUTS;

    public static int getAndIncrementId(){
        return CURRENT_ID++;
    }

    private final KeyboardManager keyManager;
    private final MouseManager mouseManager;

    private final Map<Integer, ActionConfigurations> actionConfigurations;


    /**
     * Input manager class.
     * Instance the class, set the input and its types.
     */
    public InputManager(KeyboardManager keyManager, MouseManager mouseManager){
        this.keyManager = keyManager;
        this.mouseManager = mouseManager;

        actionConfigurations = new HashMap<>();
    }


    public void init(ActionConfigurations[] actionConfigurations){
        initLibraryInputs();

        int index;

        for (int i = 0; i < actionConfigurations.length; ++i) {
            this.actionConfigurations.put(actionConfigurations[i].actionId(), actionConfigurations[i]);
        }
    }


    public boolean input(int inputID){
        ActionConfigurations actionConfiguration = actionConfigurations.get(inputID);
        if (actionConfiguration == null)
            return false;

        for (InputConfiguration inputConfiguration : actionConfiguration.configurations()){
            switch (inputConfiguration.type()){
                case Keyboard:
                    if (keyManager.getKeyState(inputConfiguration.id()))
                        return true;
                    break;
                case Mouse:
                    if (mouseManager.getState(inputConfiguration.id()))
                        return true;
                    break;
            }
        }

        return false;
    }


    public boolean inputPressed(int inputId){
        ActionConfigurations actionConfiguration = actionConfigurations.get(inputId);
        if (actionConfiguration == null)
            return false;

        for (InputConfiguration inputConfiguration : actionConfiguration.configurations()){
            switch (inputConfiguration.type()){
                case Keyboard:
                    if (keyManager.keyPressed(inputConfiguration.id()))
                        return true;
                    break;
                case Mouse:
                    if (mouseManager.buttonPressed(inputConfiguration.id()))
                        return true;
                    break;
            }
        }

        return false;

    }


    public boolean inputReleased(int inputID){
        ActionConfigurations actionConfiguration = actionConfigurations.get(inputID);
        if (actionConfiguration == null)
            return false;

        for (InputConfiguration inputConfiguration : actionConfiguration.configurations()){
            switch (inputConfiguration.type()){
                case Keyboard:
                    if (keyManager.keyReleased(inputConfiguration.id()))
                        return true;
                    break;
                case Mouse:
                    if (mouseManager.buttonReleased(inputConfiguration.id()))
                        return true;
                    break;
            }
        }

        return false;
    }


    public void dispose(){
        keyManager.dispose();
        mouseManager.dispose();
    }


    private void initLibraryInputs(){
        actionConfigurations.put(
                COMMAND,
                new ActionConfigurations(COMMAND, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_F1, EInputType.Keyboard)})
        );

        actionConfigurations.put(
                RELOAD_TEXTURE,
                new ActionConfigurations(RELOAD_TEXTURE, new InputConfiguration[]{new InputConfiguration(GLFW_KEY_F5, EInputType.Keyboard)})
        );
    }
}
