package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.utils.IUpdatable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.joml.Vector2i;

import java.util.Objects;

/**
 * A 2D camera moved by the player, in one component: move actions (keys, arrows, stick), the camera dragged with the
 * mouse while an action is held, and zoomed around the mouse by the wheel. It adds MovingCameraComponent,
 * DraggingCameraComponent and ZoomingCameraComponent, each used only when its actions are given, the 2D counterpart of
 * FreeCamera3DComponent:
 * <pre>
 * addUpdatable(new FreeCamera2DComponent(inputs, mouse, camera2D(), windowSize, shift)
 *         .setMove(left, right, up, down).setDrag(rightClick));
 * </pre>
 * Add it to a scene (Scene.addUpdatable): it is updated with the scene.
 */
public final class FreeCamera2DComponent implements IUpdatable {
    private final IInputManager inputManager;
    private final MouseManager mouseManager;
    private final MovingCameraComponent moving = new MovingCameraComponent();
    private final DraggingCameraComponent dragging = new DraggingCameraComponent();
    private final ZoomingCameraComponent zooming = new ZoomingCameraComponent();

    private final InputAction quick;
    private InputAction moveLeft, moveRight, moveUp, moveDown;
    private boolean drags;
    private boolean zoomsWithWheel = true;
    private boolean interacting;

    /**
     * @param windowSize the size of the window in pixels (the reference object of its WindowInfo: the wheel zooms
     *                   around the mouse, in parts of it)
     * @param quick      held, this action doubles the speed of the moves and makes the wheel zoom the width only
     */
    public FreeCamera2DComponent(IInputManager inputManager, MouseManager mouseManager, Camera2D camera,
                                 Vector2i windowSize, InputAction quick) {
        this.inputManager = Objects.requireNonNull(inputManager, "inputManager");
        this.mouseManager = Objects.requireNonNull(mouseManager, "mouseManager");
        Objects.requireNonNull(camera, "camera");
        moving.init(inputManager, camera);
        dragging.init(inputManager, mouseManager, camera);
        zooming.init(inputManager, mouseManager, camera, Objects.requireNonNull(windowSize, "windowSize"));
        this.quick = Objects.requireNonNull(quick, "quick");
        zooming.initActionId(quick);
    }

    /** The actions that move the camera along the screen, at the same speed on screen whatever the zoom. */
    public FreeCamera2DComponent setMove(InputAction left, InputAction right, InputAction up, InputAction down) {
        moveLeft = Objects.requireNonNull(left, "left");
        moveRight = Objects.requireNonNull(right, "right");
        moveUp = Objects.requireNonNull(up, "up");
        moveDown = Objects.requireNonNull(down, "down");
        moving.initActionIds(new MovingCameraComponent.Inputs().setMoveLeft(left).setMoveRight(right)
                .setMoveUp(up).setMoveDown(down).setQuickSpeed(quick));
        return this;
    }

    /** The camera follows the mouse while this action is held (the world is dragged). */
    public FreeCamera2DComponent setDrag(InputAction drag) {
        dragging.initActionId(Objects.requireNonNull(drag, "drag"));
        drags = true;
        return this;
    }

    // Without a wheel zoom (a scene that uses the wheel itself)
    public FreeCamera2DComponent setWheelZoom(boolean wheelZoom) {
        zoomsWithWheel = wheelZoom;
        return this;
    }

    public MovingCameraComponent moving() {
        return moving;
    }

    /** The camera was moved, dragged or zoomed by the player in the last update (to stop an automatic camera). */
    public boolean isInteracting() {
        return interacting;
    }

    @Override
    public void update() {
        boolean wheel = zoomsWithWheel && mouseManager.getMouseScroll().y != 0f;
        boolean keys = false;
        if (moveLeft != null) {
            keys = inputManager.getValue(moveLeft) != 0f || inputManager.getValue(moveRight) != 0f
                    || inputManager.getValue(moveUp) != 0f || inputManager.getValue(moveDown) != 0f;
            moving.update();
        }
        if (drags)
            dragging.update();
        if (zoomsWithWheel)
            zooming.update();

        interacting = keys || wheel || (drags && dragging.isDragging());
    }

    @Override
    public void postUpdate() {}

    @Override
    public void unload() {}
}
