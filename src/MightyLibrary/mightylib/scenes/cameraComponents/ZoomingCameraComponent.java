package MightyLibrary.mightylib.scenes.cameraComponents;

import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.utils.IUpdatable;
import MightyLibrary.mightylib.scenes.Camera2D;
import MightyLibrary.project.main.ActionId;
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

            if (inputManager.getState(ActionId.SHIFT))
                zoom(new Vector2f(1 + mouseManager.getMouseScroll().y * 0.1f, 1));
            else
                zoom(new Vector2f(1 + mouseManager.getMouseScroll().y * 0.1f));
        }

       /* if (inputManager.inputPressed(ActionId.SHIFT)) {
            mapCamera.setZoomReference(EDirection.None);
            zoom(new Vector2f(1.01f));
        }*/
    }

    @Override
    public void dispose() {}

    @Override
    public void unload() {}
}
