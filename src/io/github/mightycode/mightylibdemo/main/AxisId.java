package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.InputAxis;
import io.github.mightycode.mightylib.inputs.InputRegistry;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;

// Axes of the demo, bound in ProjectLoading
public class AxisId {
    // -1 to 1
    public static final InputAxis MOVE_RIGHT_3D = InputRegistry.axis("demo:move_right_3d");
    public static final InputAxis MOVE_FORWARD_3D = InputRegistry.axis("demo:move_forward_3d");
    public static final InputAxis MOVE_UP_3D = InputRegistry.axis("demo:move_up_3d");

    // Degrees for this tick
    public static final InputAxis LOOK_RIGHT = InputRegistry.axis("demo:look_right");
    public static final InputAxis LOOK_UP = InputRegistry.axis("demo:look_up");

    // Inputs of the free 3D camera of the demo scenes
    public static FreeCamera3DComponent.Inputs freeCamera3DInputs() {
        return new FreeCamera3DComponent.Inputs()
                .setMove(MOVE_RIGHT_3D, MOVE_FORWARD_3D, MOVE_UP_3D)
                .setLook(LOOK_RIGHT, LOOK_UP)
                .setQuickSpeed(ActionId.SHIFT);
    }
}
