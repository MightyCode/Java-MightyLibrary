package MightyLibrary.project.scenes.Platformer2D;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.project.main.ActionId;

public class PlayerController {
    private final InputManager input;

    public PlayerController(InputManager input) {
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
