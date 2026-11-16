package io.github.mightycode.mightylibdemo.scenes.platformer2d;

import io.github.mightycode.mightylib.physics.B2Utils;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class PlayerManager extends Box2DActor{
    private final PlayerValues values;
    private final PlayerController controller;
    private final PlayerRenderer renderer;
    private final Camera2D camera;
    private final Vector2i windowSize;

    // Camera follow smoothness
    private static final float CAMERA_FOLLOW_LERP = 0.1f;

    public PlayerManager(World world, PlayerController controller, Vec2 spawnPos, Camera2D baseCamera, Vector2i windowSize) {
        this.windowSize = windowSize;
        this.controller = controller;
        this.values = new PlayerValues();
        this.values.position.set(spawnPos);

        this.setDynamic();
        this.setShapeRectangle(spawnPos, values.size);
        this.setPhysicsProperties(1, 1, 0.0f);
        this.setFixedRotation();
        this.initializePhysics(world);

        // === CAMERA ===
        this.camera = baseCamera;

        // === RENDERER ===
        this.renderer = new PlayerRenderer(values);
    }

    /** Update player state (no argument version) */
    public void update() {
        this.applyForce(new Vec2(PlayerValues.MOVE_SPEED * controller.getMoveAxis(), 0));

        // Jumping
        if (controller.wantsJump() && values.onGround()) {
            Vec2 jumpVec = new Vec2(0, PlayerValues.JUMP_FORCE);
            applyImpulse(jumpVec);
        }

        // Update cached player position
        Vec2 pos = body.getPosition();
        values.position.set(pos.x, pos.y);

        // === Smooth camera follow ===
        Vector2f camPos = camera.getCamPosRef();

        Vector2f newCameraPos = new Vector2f(B2Utils.b2ToEngine(values.position));
        newCameraPos.x -= windowSize.x / (2f * camera.getZoomLevel().x);
        newCameraPos.y -= windowSize.y /  (2f * camera.getZoomLevel().y);

        camPos.lerp(newCameraPos, CAMERA_FOLLOW_LERP);
        camera.setPos(camPos);

        renderer.update();
    }

    public void display() {
        renderer.display();
    }

    public PlayerValues getValues() {
        return values;
    }

    /** Free rendering resources */
    public void unload() {
        renderer.unload();
    }
}