package MightyLibrary.mightylib.inputs.inputType;

import MightyLibrary.mightylib.inputs.InputManager;

public class InputSimple implements IInput  {
    private final int inputId;
    private final EInputType inputType;

    public InputSimple(int inputId, EInputType type){
        this.inputId = inputId;
        this.inputType = type;
    }

    public int id() { return inputId; }
    public EInputType type() { return inputType; }

    @Override
    public boolean getState(InputManager inputManager) {
       switch (inputType){
           case Keyboard:
               return inputManager.getKeyboardManager().getKeyState(inputId);
           case Mouse:
               return inputManager.getMouseManager().getState(inputId);
       }

       return false;
    }

    @Override
    public boolean inputPressed(InputManager inputManager) {
        switch (inputType){
            case Keyboard:
                return inputManager.getKeyboardManager().keyPressed(inputId);
            case Mouse:
                return inputManager.getMouseManager().buttonPressed(inputId);
        }

        return false;
    }

    @Override
    public boolean inputReleased(InputManager inputManager) {
        switch (inputType){
            case Keyboard:
                return inputManager.getKeyboardManager().keyReleased(inputId);
            case Mouse:
                return inputManager.getMouseManager().buttonReleased(inputId);
        }

        return false;
    }
}
