package MightyLibrary.mightylib.inputs;

/**
 * This class is the input manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class InputManager {
    public final static int ESCAPE = 0;

    private KeyboardManager keyManager;
    private MouseManager mouseManager;

    private int[] type;
    private int[] inputs;

    /**
     * Input manager class.
     * Instance the class, set the input and its type.
     */
    public InputManager(KeyboardManager keyManager, MouseManager mouseManager, int[][] input){
        this.keyManager = keyManager;
        this.mouseManager = mouseManager;

        inputs = new int[input.length];
        type = new int[input.length];

        for(int i = 0; i < input.length; ++i){
            if(input[i][0] == -1){
                inputs[i] = input[i][1];
                type[i] = 1;
            } else {
                inputs[i] = input[i][0];
                type[i] = 0;
            }
        }
    }

    public boolean input(int inputs){
        if(type[inputs] == 0) return keyManager.getKeyState(this.inputs[inputs]);
        else return mouseManager.getState(this.inputs[inputs]);
    }

    public boolean inputPressed(int inputs){
        if(type[inputs] == 0) return keyManager.keyPressed(this.inputs[inputs]);
        else return mouseManager.buttonPressed(this.inputs[inputs]);
    }

    public boolean inputReleased(int inputs){
        if(type[inputs] == 0) return keyManager.keyReleased(this.inputs[inputs]);
        else return mouseManager.buttonReleased(this.inputs[inputs]);
    }

    public void dispose(){
        keyManager.dispose();
        mouseManager.dispose();
    }
}
