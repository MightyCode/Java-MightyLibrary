package io.github.mightycode.mightylibdemo.scenes.platformer2d;

import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylibdemo.main.ActionId;

public class PlayerController {
    private final IInputManager input;

    public PlayerController(IInputManager input) {
        this.input = input;
    }

    public float getMoveAxis() {
        float move = 0;
        if (input.getState(ActionId.MOVE_LEFT_2D)) move -= 1;
        if (input.getState(ActionId.MOVE_RIGHT_2D)) move += 1;
        return move;
    }

    public boolean wantsJump() {
        return input.getState(ActionId.MOVE_UP);
    }
}
