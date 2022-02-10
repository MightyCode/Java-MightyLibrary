package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.util.math.MightyMath;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;


public class Camera {
    private final WindowInfo windowInfo;
    private final MouseManager mouseManager;

    private boolean usePerspective;
    private final Matrix4f projectionOrtho, projectionPerpec, view;
    private final Vector3f camPos;

    private final Vector3f camFront, camUp;
    private FloatBuffer projectionBuffer, viewBuffer;
    private boolean lockViewCursor = false;

    public static Vector3f speed;

    public static float sensitivity;

    private float yaw = 180.0f, pitch = 0.0f;
    private float yawCos, yawSin;

    public Camera(WindowInfo windowInfo,  MouseManager mouseManager, float fov, Vector3f pos){
        this.windowInfo = windowInfo;
        this.mouseManager = mouseManager;

        camPos = new Vector3f();

        projectionOrtho = new Matrix4f();
        projectionPerpec = new Matrix4f();
        usePerspective = true;

        view = new Matrix4f();
        projectionBuffer = BufferUtils.createFloatBuffer(16);
        viewBuffer = BufferUtils.createFloatBuffer(16);

        setViewAngle(fov);
        camFront = new Vector3f(0.0f, 0.0f, -1.0f);
        camUp = new Vector3f(0.0f, 1.0f, 0.0f);
        setPos(pos);

        speed = new Vector3f(0.25f,0.25f,0.25f);
        sensitivity = 0.05f;
        updateView();
    }


    public void setToCursor(){
        float offsetX = (mouseManager.posX() - mouseManager.oldPosX()) * sensitivity;
        float offsetY = (-mouseManager.posY() + mouseManager.oldPosY()) * sensitivity;

        yaw += offsetX;
        pitch += offsetY;

        if(yaw > 360.0f) yaw -= 360.0f;
        if(yaw < -360.0f) yaw += 360.0f;

        if(pitch > 89.0f)
            pitch =  89.0f;
        if(pitch < -89.0f)
            pitch = -89.0f;

        yawCos = (float)java.lang.Math.cos(MightyMath.rads(yaw));
        yawSin = (float)java.lang.Math.sin(MightyMath.rads(yaw));

        camFront.x = (float)(java.lang.Math.cos(MightyMath.rads(pitch)) * yawCos);
        camFront.y = (float)(java.lang.Math.sin(MightyMath.rads(pitch)));
        camFront.z = (float)(java.lang.Math.cos(MightyMath.rads(pitch)) * java.lang.Math.sin(MightyMath.rads(yaw)));
        camFront.normalize();
    }


    public void updateView(){
        if (!lockViewCursor) {
            setToCursor();
        }

        view.identity();
        view.lookAt(camPos, camPos.add(camFront, new Vector3f()), camUp);
        viewBuffer = view.get(viewBuffer);
    }

    public void setLockViewCursor(boolean state){
        lockViewCursor = state;
    }


    public void invertLockViewCursor(){
        setLockViewCursor(!lockViewCursor);
    }


    public void setViewAngle(float fov){
        projectionPerpec.perspective(fov, windowInfo.getVirtualRatio(), 0.01f, 10000f);
        projectionOrtho.ortho(0, windowInfo.getVirtualSizeRef().x, windowInfo.getVirtualSizeRef().y, 0 , -1, 1);

        if (usePerspective)
            projectionBuffer = projectionPerpec.get(projectionBuffer);
        else
            projectionBuffer  = projectionOrtho.get(projectionBuffer);
    }

    public Vector3f getCamPosRef() { return camPos; }
    public Vector3f getCamPosCopy() { return new Vector3f(camPos); }
    public void setPos(Vector3f newPos){
        camPos.x = newPos.x;
        camPos.y = newPos.y;
        camPos.z = newPos.z;
    }

    public void setX(float x){
        camPos.x = x;
    }

    public void setY(float y){
        camPos.y = y;
    }

    public void setZ(float z){
        camPos.z = z;
    }

    public FloatBuffer getProjection(){
        return projectionBuffer;
    }

    public FloatBuffer getView(){
        return viewBuffer;
    }


    public void speedAngX(float speed){
        camPos.x += speed * yawSin;
        camPos.z += speed * -yawCos;
    }

    public void speedAngZ(float speed){
        camPos.x += speed * -yawCos;
        camPos.z += speed * -yawSin;
    }
}
