package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.joml.Vector2f;

public class MovingCameraComponent implements IUpdatable {
    public static class Inputs {
        public InputAction MoveLeft, MoveRight, MoveUp, MoveDown, QuickSpeed;

        public Inputs setMoveLeft(InputAction moveLeft) {
            MoveLeft = moveLeft;
            return this;
        }

        public Inputs setMoveRight(InputAction moveRight) {
            MoveRight = moveRight;
            return this;
        }

        public Inputs setMoveUp(InputAction moveUp) {
            MoveUp = moveUp;
            return this;
        }

        public Inputs setMoveDown(InputAction moveDown) {
            MoveDown = moveDown;
            return this;
        }

        public Inputs setQuickSpeed(InputAction quickSpeed) {
            QuickSpeed = quickSpeed;
            return this;
        }
    }

    public static final float MOVE_SPEED = 600;
    public static final float QUICK_SPEED = MOVE_SPEED * 2f;

    protected float moveSpeed, quickSpeed;

    protected InputManager inputManager;
    protected Camera2D controlledCamera;

    protected Inputs inputs;

    private final Vector2f direction = new Vector2f();

    public MovingCameraComponent(float moveSpeed, float quickSpeed){
        this.moveSpeed = moveSpeed;
        this.quickSpeed = quickSpeed;
    }

    public MovingCameraComponent(){
        this(MOVE_SPEED, QUICK_SPEED);
    }

    public void init(InputManager inputManager, Camera2D controlledCamera){
        this.inputManager = inputManager;
        this.controlledCamera = controlledCamera;
    }

    public void initActionIds(Inputs inputs){
        this.inputs = inputs;
    }

    public void update(){
        float speed = inputManager.getState(inputs.QuickSpeed) ? quickSpeed : moveSpeed;

        // Progressive with a stick, 1 with a key
        direction.set(
                inputManager.getValue(inputs.MoveRight) - inputManager.getValue(inputs.MoveLeft),
                inputManager.getValue(inputs.MoveDown) - inputManager.getValue(inputs.MoveUp));
        ClampLength(direction);
        direction.mul(speed * GameTime.DeltaTime());

        if (direction.x != 0)
            controlledCamera.moveXinZoom(direction.x);

        if (direction.y != 0)
            controlledCamera.moveYinZoom(direction.y);
    }

    // A diagonal is not faster than a straight move : length limited to 1
    static Vector2f ClampLength(Vector2f direction) {
        float length = direction.length();
        if (length > 1)
            direction.div(length);

        return direction;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getQuickSpeed() {
        return quickSpeed;
    }

    public void setQuickSpeed(float quickSpeed) {
        this.quickSpeed = quickSpeed;
    }

    @Override
    public void postUpdate() {}

    @Override
    public void unload() {}
}
