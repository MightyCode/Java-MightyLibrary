package MightyLibrary.mightylib.inputs;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This class is the input manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class InputManager {
    private static final int INPUT_DATA_INDEX = 0;
    private static final int INPUT_DATA_INPUT = 1;
    private static final int INPUT_DATA_TYPE = 2;


    public static final int ID_KEYBOARD = 0;
    public static final int ID_MOUSE = 1;


    private static final int NUMBER_LIBRARY_INPUTS = 2;

    public static final int COMMAND = 0;
    public static final int RELOAD_TEXTURE = 1;

    private static int CURRENT_ID = NUMBER_LIBRARY_INPUTS;

    public static int getAndIncrementId(){
        return CURRENT_ID++;
    }

    private final KeyboardManager keyManager;
    private final MouseManager mouseManager;

    //TODO
    private int[] types;
    private int[] inputs;


    /**
     * Input manager class.
     * Instance the class, set the input and its types.
     */
    public InputManager(KeyboardManager keyManager, MouseManager mouseManager){
        this.keyManager = keyManager;
        this.mouseManager = mouseManager;
    }


    public void init(int[][] inputData){
        inputs = new int[CURRENT_ID];
        types = new int[inputs.length];
        initLibraryInputs();

        int index;

        for (int[] inputDatum : inputData) {
            index = inputDatum[INPUT_DATA_INDEX];

            inputs[index] = inputDatum[INPUT_DATA_INPUT];
            types[index] = inputDatum[INPUT_DATA_TYPE];
        }
    }


    public boolean input(int inputID){
        if(types[inputID] == ID_KEYBOARD) return keyManager.getKeyState(this.inputs[inputID]);
        else return mouseManager.getState(this.inputs[inputID]);
    }


    public boolean inputPressed(int inputId){
        if(types[inputId] == ID_KEYBOARD) return keyManager.keyPressed(this.inputs[inputId]);
        else return mouseManager.buttonPressed(this.inputs[inputId]);
    }


    public boolean inputReleased(int inputID){
        if(types[inputID] == ID_KEYBOARD) return keyManager.keyReleased(this.inputs[inputID]);
        else return mouseManager.buttonReleased(this.inputs[inputID]);
    }


    public void dispose(){
        keyManager.dispose();
        mouseManager.dispose();
    }


    private void initLibraryInputs(){
        inputs[COMMAND] = GLFW_KEY_F1;
        types[COMMAND] = ID_KEYBOARD;

        inputs[RELOAD_TEXTURE] = GLFW_KEY_F5;
        types[RELOAD_TEXTURE] = ID_KEYBOARD;
    }

}
