package io.github.mightycode.mightylib.inputs.inputType;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.GamepadManager;

public class InputSimple implements IAction {
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
           case Controller:
               return inputManager.getGamepadManager().getButton(inputId, GamepadManager.ANY_PLAYER);
       }

       return false;
    }

    @Override
    public boolean actionPressed(InputManager inputManager) {
        switch (inputType){
            case Keyboard:
                return inputManager.getKeyboardManager().keyPressed(inputId);
            case Mouse:
                return inputManager.getMouseManager().buttonPressed(inputId);
            case Controller:
                return inputManager.getGamepadManager().buttonPressed(inputId, GamepadManager.ANY_PLAYER);
        }

        return false;
    }

    @Override
    public boolean actionReleased(InputManager inputManager) {
        switch (inputType){
            case Keyboard:
                return inputManager.getKeyboardManager().keyReleased(inputId);
            case Mouse:
                return inputManager.getMouseManager().buttonReleased(inputId);
            case Controller:
                return inputManager.getGamepadManager().buttonReleased(inputId, GamepadManager.ANY_PLAYER);
        }

        return false;
    }
}
