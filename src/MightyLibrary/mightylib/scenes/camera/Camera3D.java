package MightyLibrary.mightylib.scenes.camera;

import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.utils.math.MightyMath;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class Camera3D extends Camera {
    public enum EViewMode {
        Perspective,
        Orthographic
    }

    private final MouseManager mouseManager;

    private final Vector3f camPos, camFront, camUp;

    private boolean lockViewCursor = false;

    private Vector3f speed;

    private float sensitivity;

    private float yaw = 180.0f, pitch = 0.0f;
    private float yawCos, yawSin;
    private float fov;

    private EViewMode viewMode;

    public Camera3D(WindowInfo windowInfo, MouseManager mouseManager, float fov, Vector3f pos) {
        super(windowInfo);
        this.mouseManager = mouseManager;

        this.viewMode = EViewMode.Perspective;
        camPos = new Vector3f();

        setViewAngle(fov);
        camFront = new Vector3f(0.0f, 0.0f, -1.0f);
        camUp = new Vector3f(0.0f, 1.0f, 0.0f);
        setPos(pos);

        speed = new Vector3f(1);
        sensitivity = 0.05f;
        updateView();
    }


    public void setToCursor() {
        float offsetX = (mouseManager.posX() - mouseManager.oldPosX()) * sensitivity;
        float offsetY = (-mouseManager.posY() + mouseManager.oldPosY()) * sensitivity;

        yaw += offsetX;
        pitch += offsetY;

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
        if (!lockViewCursor)
            setToCursor();

        view.identity();
        view.lookAt(camPos, camPos.add(camFront, new Vector3f()), camUp);
    }

    public void setLockViewCursor(boolean state) {
        lockViewCursor = state;
    }

    public void lookAt(Vector3f target) {
        camFront.set(target.sub(camPos, new Vector3f()).normalize());
        shouldUpdateView();
    }

    public void invertLockViewCursor() {
        setLockViewCursor(!lockViewCursor);
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

        shouldUpdateView();
    }

    public void setX(float x) {
        camPos.x = x;

        shouldUpdateView();
    }

    public void setY(float y) {
        camPos.y = y;

        shouldUpdateView();
    }

    public void setZ(float z) {
        camPos.z = z;

        shouldUpdateView();
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

        shouldUpdateView();
    }

    public void speedAngZ(float speed) {
        camPos.x += speed * -yawCos;
        camPos.z += speed * -yawSin;

        shouldUpdateView();
    }

    public Camera3D setSensitivity(float value) {
        sensitivity = value;

        return this;
    }

    public float getSensitivity() {
        return sensitivity;
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
