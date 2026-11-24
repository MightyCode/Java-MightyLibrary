package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera2DComponent;

/**
 * The camera controls of the 2D scenes of the demo: Z Q S D (on an AZERTY keyboard) and the arrows move the camera,
 * Shift held doubles the speed, the right button drags the world, the wheel zooms around the mouse. The keys are the
 * move actions of the demo (ActionId.MOVE_*_2D), so a scene that uses them for something else gives them up.
 */
public final class DemoCamera {
    private DemoCamera() {}

    /** The free camera on the 2D camera of this scene (call it in the launch of the scene), updated with it. */
    public static FreeCamera2DComponent free(Scene scene) {
        return free(scene, ActionId.RIGHT_CLICK);
    }

    /** The same, the world dragged with this mouse button (a scene that uses the right one itself). */
    public static FreeCamera2DComponent free(Scene scene, InputAction drag) {
        FreeCamera2DComponent camera = new FreeCamera2DComponent(scene.context().getInputManager(),
                scene.context().getMouseManager(), scene.camera2D(), scene.context().getWindow().getInfo().getSizeRef(),
                ActionId.SHIFT)
                .setMove(ActionId.MOVE_LEFT_2D, ActionId.MOVE_RIGHT_2D, ActionId.MOVE_UP_2D, ActionId.MOVE_DOWN_2D)
                .setDrag(drag);
        scene.addUpdatable(camera);
        return camera;
    }
}
