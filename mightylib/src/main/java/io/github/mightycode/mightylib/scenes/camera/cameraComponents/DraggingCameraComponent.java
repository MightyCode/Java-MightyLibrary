package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;

public final class DraggingCameraComponent implements IUpdatable {
    private IInputManager inputManager;
    private MouseManager mouseManager;

    private InputAction dragActionId;

    private Camera2D controlledCamera;

    private boolean isDragging;

    public DraggingCameraComponent(){
    }

    public void init(IInputManager inputManager, MouseManager mouseManager, Camera2D controlledCamera){
        this.inputManager = inputManager;
        this.mouseManager = mouseManager;
        this.controlledCamera = controlledCamera;
    }

    public void initActionId(InputAction dragActionId){
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
    public void postUpdate() {}

    @Override
    public void unload() {}

    public boolean isDragging() {
        return isDragging;
    }
}
