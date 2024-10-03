package MightyLibrary.mightylib.scenes.camera.cameraComponents;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.utils.IUpdatable;
import MightyLibrary.mightylib.scenes.camera.Camera2D;

public class DraggingCameraComponent implements IUpdatable {
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

    @Override
    public void dispose() {}

    @Override
    public void unload() {}

    public boolean isDragging() {
        return isDragging;
    }
}
