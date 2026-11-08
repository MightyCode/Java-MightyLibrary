package io.github.mightycode.mightylibdemo.scenes.Platformer2D;

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

    private int groundCount;

    // Camera follow smoothness
    private final float cameraFollowLerp = 0.1f;

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

    @Override
    public void initializePhysics(World world) {
        super.initializePhysics(world);

       /* FixtureDef bottomSensor = new FixtureDef();
        FixtureDef rightSensor = new FixtureDef();
        FixtureDef leftSensor = new FixtureDef();

        bottomSensor.isSensor = true;
        rightSensor.isSensor = true;
        leftSensor.isSensor = true;

        PolygonShape sensorShape = new PolygonShape();

        // bottom fixture
        sensorShape.setAsBox((getWidth() - 8)/200, getHeight()/200, new Vector2(0, -20/200f), 0);
        bottomSensor.shape = sensorShape;
        Fixture bottomFixture = body.createFixture(bottomSensor);
        bottomFixture.setUserData("bottom");

        // right fixture
        sensorShape.setAsBox(getWidth()/200, (getHeight() - 8)/200, new Vector2(20/200f, 0), 0);
        rightSensor.shape = sensorShape;
        Fixture rightFixture = body.createFixture(rightSensor);
        rightFixture.setUserData("right");

        // left fixture
        sensorShape.setAsBox(getWidth()/200, (getHeight() - 8)/200, new Vector2(-20/200f, 0), 0);
        leftSensor.shape = sensorShape;
        Fixture leftFixture = body.createFixture(leftSensor);
        leftFixture.setUserData("left");*/
    }

    /** Update player state (no argument version) */
    public void update() {
        //this.setMaxSpeedX(3);

        this.applyForce(new Vec2(values.moveSpeed * controller.getMoveAxis(), 0));

        // Jumping
        if (controller.wantsJump() && values.onGround()) {
            Vec2 jumpVec = new Vec2(0, values.jumpForce);
            applyImpulse(jumpVec);
        }

        super.update();

        // Update cached player position
        Vec2 pos = body.getPosition();
        values.position.set(pos.x, pos.y);

        // === Smooth camera follow ===
        Vector2f camPos = camera.getCamPosRef();

        Vector2f newCameraPos = new Vector2f(B2Utils.b2ToEngine(values.position));
        newCameraPos.x -= windowSize.x / (2f * camera.getZoomLevel().x);
        newCameraPos.y -= windowSize.y /  (2f * camera.getZoomLevel().y);

        camPos.lerp(newCameraPos, cameraFollowLerp);
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