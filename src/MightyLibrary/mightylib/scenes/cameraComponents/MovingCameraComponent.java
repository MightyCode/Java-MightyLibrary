package MightyLibrary.mightylib.scenes.cameraComponents;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.utils.GameTime;
import MightyLibrary.mightylib.main.utils.IUpdatable;
import MightyLibrary.mightylib.scenes.Camera2D;

public class MovingCameraComponent implements IUpdatable {
    public static class Inputs {
        public int MoveLeft, MoveRight, MoveUp, MoveDown, QuickSpeed;

        public Inputs setMoveLeft(int moveLeft) {
            MoveLeft = moveLeft;
            return this;
        }

        public Inputs setMoveRight(int moveRight) {
            MoveRight = moveRight;
            return this;
        }

        public Inputs setMoveUp(int moveUp) {
            MoveUp = moveUp;
            return this;
        }

        public Inputs setMoveDown(int moveDown) {
            MoveDown = moveDown;
            return this;
        }

        public Inputs setQuickSpeed(int quickSpeed) {
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

    public MovingCameraComponent(float moveSpeed, float quickSpeed){
        this.moveSpeed = moveSpeed;
        this.quickSpeed = quickSpeed;
    }

    public MovingCameraComponent(){
        this(MOVE_SPEED, QUICK_SPEED);
    }

    public void init(InputManager inputManager, MouseManager mouseManager, Camera2D controlledCamera){
        this.inputManager = inputManager;
        this.controlledCamera = controlledCamera;
    }

    public void initActionIds(Inputs inputs){
        this.inputs = inputs;
    }

    public void update(){
        float speed = MOVE_SPEED;
        if (inputManager.getState(inputs.QuickSpeed))
            speed = quickSpeed;

        speed *= GameTime.DeltaTime();

        if (inputManager.getState(inputs.MoveLeft)){
            controlledCamera.moveXinZoom(-speed);
        }

        if (inputManager.getState(inputs.MoveRight)){
            controlledCamera.moveXinZoom(speed);
        }

        if (inputManager.getState(inputs.MoveUp)){
            controlledCamera.moveYinZoom(-speed);
        }

        if (inputManager.getState(inputs.MoveDown)){
            controlledCamera.moveYinZoom(speed);
        }
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
    public void dispose() {}

    @Override
    public void unload() {}
}
