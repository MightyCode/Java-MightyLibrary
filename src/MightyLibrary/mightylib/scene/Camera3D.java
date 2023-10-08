package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.util.math.MightyMath;
import org.joml.Vector3f;


public class Camera3D extends Camera {
    private final MouseManager mouseManager;

    private final Vector3f camPos, camFront, camUp;

    private boolean lockViewCursor = false;

    private Vector3f speed;

    private float sensitivity;

    private float yaw = 180.0f, pitch = 0.0f;
    private float yawCos, yawSin;
    private float fov;

    public Camera3D(WindowInfo windowInfo, MouseManager mouseManager, float fov, Vector3f pos) {
        super(windowInfo);
        this.mouseManager = mouseManager;

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

    public void invertLockViewCursor() {
        setLockViewCursor(!lockViewCursor);
    }


    public void setViewAngle(float fov) {
        this.fov = fov;
        updateProjection();
    }

    public void updateProjection() {
        projection.perspective(fov, windowInfo.getVirtualRatio(), 0.01f, 10000f);
        //projectionOrtho.ortho(0, windowInfo.getVirtualSizeRef().x, windowInfo.getVirtualSizeRef().y, 0 , -1, 1);
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
    }

    public void setX(float x) {
        camPos.x = x;
    }

    public void setY(float y) {
        camPos.y = y;
    }

    public void setZ(float z) {
        camPos.z = z;
    }


    public void speedAngX(float speed) {
        camPos.x += speed * yawSin;
        camPos.z += speed * -yawCos;
    }

    public void speedAngZ(float speed) {
        camPos.x += speed * -yawCos;
        camPos.z += speed * -yawSin;
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

    public Vector3f getSpeed() {
        return speed;
    }
}
