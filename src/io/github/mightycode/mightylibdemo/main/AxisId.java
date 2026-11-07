package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;

// Axes of the demo, bound in ProjectLoading (same id pool as the actions)
public class AxisId {
    // -1 to 1
    public static final int MOVE_RIGHT_3D = InputManager.getAndIncrementId();
    public static final int MOVE_FORWARD_3D = InputManager.getAndIncrementId();
    public static final int MOVE_UP_3D = InputManager.getAndIncrementId();

    // Degrees for this tick
    public static final int LOOK_RIGHT = InputManager.getAndIncrementId();
    public static final int LOOK_UP = InputManager.getAndIncrementId();

    // Inputs of the free 3D camera of the demo scenes
    public static FreeCamera3DComponent.Inputs freeCamera3DInputs() {
        return new FreeCamera3DComponent.Inputs()
                .setMove(MOVE_RIGHT_3D, MOVE_FORWARD_3D, MOVE_UP_3D)
                .setLook(LOOK_RIGHT, LOOK_UP)
                .setQuickSpeed(ActionId.SHIFT);
    }
}
