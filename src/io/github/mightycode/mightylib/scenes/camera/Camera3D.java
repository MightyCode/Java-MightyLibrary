package io.github.mightycode.mightylib.scenes.camera;

import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class Camera3D extends Camera {
    public enum EViewMode {
        Perspective,
        Orthographic
    }

    // The camera doesn't read the inputs : FreeCamera3DComponent (or the game) moves and turns it
    private final Vector3f camPos, camFront, camUp;

    private Vector3f speed;

    private float yaw = 180.0f, pitch = 0.0f;
    private float yawCos, yawSin;
    private float fov;

    private EViewMode viewMode;

    public Camera3D(WindowInfo windowInfo, float fov, Vector3f pos) {
        super(windowInfo);

        this.viewMode = EViewMode.Perspective;
        camPos = new Vector3f();

        setViewAngle(fov);
        camFront = new Vector3f(0.0f, 0.0f, -1.0f);
        camUp = new Vector3f(0.0f, 1.0f, 0.0f);
        setPos(pos);

        speed = new Vector3f(1);
        applyAngles();
        updateView();
    }


    /**
     * Turn the camera, in degrees : deltaYaw positive to the right, deltaPitch positive upward
     * (pitch limited to [-89, 89]). Seen by the view at the next updateView.
     */
    public void rotate(float deltaYaw, float deltaPitch) {
        if (deltaYaw == 0 && deltaPitch == 0)
            return;

        yaw += deltaYaw;
        pitch += deltaPitch;
        applyAngles();
    }

    private void applyAngles() {
        if (yaw > 360.0f) yaw -= 360.0f;
        if (yaw < -360.0f) yaw += 360.0f;

        if (pitch > 89.0f)
            pitch = 89.0f;

        if (pitch < -89.0f)
            pitch = -89.0f;

        yawCos = (float) Math.cos(MightyMath.toRads(yaw));
        yawSin = (float) Math.sin(MightyMath.toRads(yaw));

        camFront.x = (float)(Math.cos(MightyMath.toRads(pitch)) * yawCos);
        camFront.y = (float)(Math.sin(MightyMath.toRads(pitch)));
        camFront.z = (float)(Math.cos(MightyMath.toRads(pitch)) * Math.sin(MightyMath.toRads(yaw)));
        camFront.normalize();
    }

    @Override
    public void updateView() {
        computeView();
    }

    private void computeView() {
        view.identity();
        view.lookAt(camPos, camPos.add(camFront, new Vector3f()), camUp);
    }

    // Yaw and pitch are updated too: otherwise the next rotate would restore the previous direction
    public void lookAt(Vector3f target) {
        Vector3f direction = target.sub(camPos, new Vector3f());
        if (direction.lengthSquared() == 0)
            return;

        direction.normalize();
        pitch = Math.max(-89.0f, Math.min(89.0f, (float) Math.toDegrees(Math.asin(direction.y))));
        yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));

        yawCos = (float) Math.cos(MightyMath.toRads(yaw));
        yawSin = (float) Math.sin(MightyMath.toRads(yaw));

        camFront.x = (float) (Math.cos(MightyMath.toRads(pitch)) * yawCos);
        camFront.y = (float) (Math.sin(MightyMath.toRads(pitch)));
        camFront.z = (float) (Math.cos(MightyMath.toRads(pitch)) * yawSin);
        camFront.normalize();

        computeView();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getViewAngle() {
        return fov;
    }

    public void setViewAngle(float fov) {
        this.fov = fov;

        superUpdateProjection();
    }

    public void updateProjection() {
        if (viewMode == EViewMode.Perspective)
            projection.perspective(fov, windowInfo.getVirtualRatio(), 0.01f, 10000f);
        else
            projection.ortho(-windowInfo.getVirtualSizeRef().x / 2f, windowInfo.getVirtualSizeRef().x / 2f,
                    -windowInfo.getVirtualSizeRef().y / 2f, windowInfo.getVirtualSizeRef().y / 2f,
                    -10000f, 10000f);
    }

    public Vector3f getCamPosRef() { return camPos; }
    public Vector3f getCamPosCopy() { return new Vector3f(camPos); }

    public Vector3f getLookAtVector() {
        return camFront;
    }

    public void setPos(Vector3f newPos) {
        camPos.x = newPos.x;
        camPos.y = newPos.y;
        camPos.z = newPos.z;

        computeView();
    }

    public void setX(float x) {
        camPos.x = x;

        computeView();
    }

    public void setY(float y) {
        camPos.y = y;

        computeView();
    }

    public void setZ(float z) {
        camPos.z = z;

        computeView();
    }

    public void moveX(float value){
        setX(camPos.x + value);
    }

    public void moveY(float value){
        setY(camPos.y + value);
    }

    public void moveZ(float value){
        setZ(camPos.z + value);
    }

    public void speedAngX(float speed) {
        camPos.x += speed * yawSin;
        camPos.z += speed * -yawCos;

        computeView();
    }

    public void speedAngZ(float speed) {
        camPos.x += speed * -yawCos;
        camPos.z += speed * -yawSin;

        computeView();
    }

    public Camera3D setSpeed(Vector3f value) {
        speed = value;

        return this;
    }

    public Vector3f worldPositionToScreen(Vector3f position, Camera2D viewCamera) {
        Vector4f clipSpacePos = new Vector4f(position.x, position.y, position.z, 1.0f);
        clipSpacePos.mul(view).mul(projection);

        if (clipSpacePos.w == 0) {
            return new Vector3f(0, 0, 0);
        }

        clipSpacePos.x /= clipSpacePos.w;
        clipSpacePos.y /= clipSpacePos.w;

        // getView is matrix
        float halfWidth = viewCamera.windowInfo.getVirtualSizeRef().x / 2f;
        float halfHeight = viewCamera.windowInfo.getVirtualSizeRef().y / 2f;

        Vector2f zoom = viewCamera.getZoomLevel();

        float x = (clipSpacePos.x + 1) * halfWidth * zoom.x;
        float y = (1 - clipSpacePos.y) * halfHeight * zoom.y;

        return new Vector3f(x, y, clipSpacePos.z);
    }


    public Vector3f getSpeed() {
        return speed;
    }

    public EViewMode getViewMode() { return viewMode; }

    public void setViewMode(EViewMode eViewMode) {
        this.viewMode = eViewMode;

       superUpdateProjection();
    }
}
