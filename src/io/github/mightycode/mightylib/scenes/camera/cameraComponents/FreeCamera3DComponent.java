package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputAxis;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;

/**
 * Free flying 3D camera driven by axes (InputManager.getAxis), so by any binding : keys, mouse, gamepad.
 * - Moves : MoveRight, MoveForward, MoveUp from -1 to 1 (AxisButtons, AxisGamepad...), at camera.getSpeed()
 *   units per second, multiplied by quickFactor while QuickSpeed (an action) is held ; no faster diagonal.
 * - Look : LookRight and LookUp in degrees for this tick (sensitivity, curve and per second are in their
 *   bindings), ignored while the look is locked (menu, cursor shown).
 * The view is updated at the end of update : add it with Scene.addUpdatable, the scene sees the new view.
 */
public class FreeCamera3DComponent implements IUpdatable {
    public static final float DEFAULT_QUICK_FACTOR = 3;

    // Axes, except QuickSpeed (an action) ; null : not used
    public static class Inputs {
        public InputAxis MoveRight, MoveForward, MoveUp;
        public InputAxis LookRight, LookUp;
        public InputAction QuickSpeed;

        public Inputs setMove(InputAxis moveRight, InputAxis moveForward, InputAxis moveUp) {
            MoveRight = moveRight;
            MoveForward = moveForward;
            MoveUp = moveUp;
            return this;
        }

        public Inputs setLook(InputAxis lookRight, InputAxis lookUp) {
            LookRight = lookRight;
            LookUp = lookUp;
            return this;
        }

        public Inputs setQuickSpeed(InputAction quickSpeed) {
            QuickSpeed = quickSpeed;
            return this;
        }
    }

    private final InputManager inputManager;
    private final Camera3D camera;
    private final Inputs inputs;

    private float quickFactor = DEFAULT_QUICK_FACTOR;
    private boolean lookLocked;

    public FreeCamera3DComponent(InputManager inputManager, Camera3D camera, Inputs inputs) {
        this.inputManager = inputManager;
        this.camera = camera;
        this.inputs = inputs;
    }

    @Override
    public void update() {
        if (!lookLocked)
            camera.rotate(axis(inputs.LookRight), axis(inputs.LookUp));

        float right = axis(inputs.MoveRight);
        float forward = axis(inputs.MoveForward);
        float length = (float) Math.sqrt(right * right + forward * forward);
        if (length > 1) {
            right /= length;
            forward /= length;
        }
        float up = axis(inputs.MoveUp);

        float factor = GameTime.DeltaTime();
        if (inputManager.getState(inputs.QuickSpeed))
            factor *= quickFactor;

        // speedAngX : positive to the left, speedAngZ : positive backward
        if (right != 0)
            camera.speedAngX(-right * camera.getSpeed().x * factor);

        if (forward != 0)
            camera.speedAngZ(-forward * camera.getSpeed().z * factor);

        if (up != 0)
            camera.setY(camera.getCamPosRef().y + up * camera.getSpeed().y * factor);

        camera.updateView();
    }

    private float axis(InputAxis axis) {
        return axis == null ? 0 : inputManager.getAxis(axis);
    }

    public boolean isLookLocked() { return lookLocked; }

    // Locked : the look axes don't turn the camera (a menu uses the same stick, the cursor is shown)
    public void setLookLocked(boolean lookLocked) {
        this.lookLocked = lookLocked;
    }

    public void toggleLookLocked() {
        lookLocked = !lookLocked;
    }

    public float getQuickFactor() { return quickFactor; }

    public void setQuickFactor(float quickFactor) {
        this.quickFactor = quickFactor;
    }

    @Override
    public void postUpdate() {}

    @Override
    public void unload() {}
}
