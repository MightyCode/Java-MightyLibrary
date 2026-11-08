package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.joml.Vector2f;
import org.joml.Vector2i;

// Same as MovingSceneComponent and DraggingSceneComponent
public class ZoomingCameraComponent implements IUpdatable {

    protected InputManager inputManager;
    protected MouseManager mouseManager;

    protected Camera2D controlledCamera;

    protected Vector2i windowSize;

    protected int quickZoomActionId;

    public ZoomingCameraComponent(){
        // No quick zoom action until initActionId is called
        quickZoomActionId = -1;
    }

    public void init(InputManager inputManager, MouseManager mouseManager, Camera2D controlledCamera,
                     Vector2i windowSize){
        this.inputManager = inputManager;
        this.mouseManager = mouseManager;
        this.controlledCamera = controlledCamera;
        this.windowSize = windowSize;
    }

    public void initActionId(int quickZoomActionId){
        this.quickZoomActionId = quickZoomActionId;
    }

    public void zoom(Vector2f factor){
        controlledCamera.setZoomLevel(new Vector2f(controlledCamera.getZoomLevel()).mul(factor));
    }

    public void update(){
        if (mouseManager.getMouseScroll().y != 0) {
            controlledCamera.setZoomReference(
                    new Vector2f(
                            mouseManager.posX() / windowSize.x,
                            mouseManager.posY() / windowSize.y
                    ));

            if (inputManager.getState(quickZoomActionId))
                zoom(new Vector2f(1 + mouseManager.getMouseScroll().y * 0.1f, 1));
            else
                zoom(new Vector2f(1 + mouseManager.getMouseScroll().y * 0.1f));
        }
    }

    @Override
    public void postUpdate() {}

    @Override
    public void unload() {}
}
