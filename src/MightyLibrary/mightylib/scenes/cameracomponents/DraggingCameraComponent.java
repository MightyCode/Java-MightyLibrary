package MightyLibrary.mightylib.scenes.cameracomponents;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.scenes.Camera2D;

public class DraggingCameraComponent {

    protected InputManager inputManager;
    protected MouseManager mouseManager;

    protected int dragActionId;

    protected Camera2D controlledCamera;

    protected boolean isDragging;

    public DraggingCameraComponent(){
    }

    public void init(InputManager inputManager, MouseManager mouseManager, Camera2D controlledCamera){
        this.inputManager = inputManager;
        this.mouseManager = mouseManager;
        this.controlledCamera = controlledCamera;
    }

    public void initActionId(int dragActionId){
        this.dragActionId = dragActionId;
    }

    public void update(){
        if (inputManager.inputReleased(dragActionId))
            isDragging = false;

        if (isDragging) {
            controlledCamera.moveXinZoom( -mouseManager.posX() + mouseManager.oldPosX());
            controlledCamera.moveYinZoom( -mouseManager.posY() + mouseManager.oldPosY());
        }

        if (inputManager.inputPressed(dragActionId))
            isDragging = true;
    }

    public boolean isDragging() {
        return isDragging;
    }
}
