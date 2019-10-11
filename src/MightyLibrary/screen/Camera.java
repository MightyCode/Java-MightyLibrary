package MightyLibrary.screen;

import MightyLibrary.util.ManagerContainer;
import MightyLibrary.util.math.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;


public class Camera {
    private ManagerContainer manContainer;
    private Matrix4f projection, view;
    public Vector3f camPos;
    private Vector3f camFront, camUp;
    private FloatBuffer projectionf, viewf;

    public static Vector3f speed;

    public static float sensitivity;

    private float yaw = 180.0f, pitch = 0.0f;
    private float yawCos, yawSin;

    public Camera(ManagerContainer manContainer, float fov, Vector3f pos){
        this.manContainer = manContainer;

        projection = new Matrix4f();
        view = new Matrix4f();
        projectionf = BufferUtils.createFloatBuffer(16);
        viewf = BufferUtils.createFloatBuffer(16);

        setViewAngle(fov);
        camFront = new Vector3f(0.0f, 0.0f, -1.0f);
        camUp = new Vector3f(0.0f, 1.0f, 0.0f);
        setPos(pos);

        speed = new Vector3f(0.25f,0.25f,0.25f);
        sensitivity = 0.05f;
        updateView();
    }

    public void setToCursor(){
        float offsetX = (manContainer.mouseManager.pos.x - manContainer.mouseManager.oldPos.x) * sensitivity;
        float offsetY = (-manContainer.mouseManager.pos.y + manContainer.mouseManager.oldPos.y) * sensitivity;

        yaw += offsetX;
        pitch += offsetY;

        if(yaw > 360.0f) yaw -= 360.0f;
        if(yaw < -360.0f) yaw += 360.0f;

        if(pitch > 89.0f)
            pitch =  89.0f;
        if(pitch < -89.0f)
            pitch = -89.0f;

        yawCos = (float)java.lang.Math.cos(Math.rads(yaw));
        yawSin = (float)java.lang.Math.sin(Math.rads(yaw));

        camFront.x = (float)(java.lang.Math.cos(Math.rads(pitch)) * yawCos);
        camFront.y = (float)(java.lang.Math.sin(Math.rads(pitch)));
        camFront.z = (float)(java.lang.Math.cos(Math.rads(pitch)) * java.lang.Math.sin(Math.rads(yaw)));
        camFront.normalize();
    }

    public void updateView(){
        view.identity();
        view.lookAt(camPos, camPos.add(camFront, new Vector3f()), camUp);
        viewf = view.get(viewf);
    }

    public Camera setViewAngle(float fov){
        projection.perspective(fov, manContainer.wParams.ratio, 0.01f, 1000f);
        projectionf = projection.get(projectionf);
        return this;
    }

    public Camera setPos(Vector3f newPos){
        camPos = newPos;
        return this;
    }

    public Camera setX(float x){
        camPos.x = x;
        return this;
    }

    public Camera setY(float y){
        camPos.y = y;
        return this;
    }

    public Camera setZ(float z){
        camPos.z = z;
        return this;
    }

    public FloatBuffer getProjection(){
        return projectionf;
    }

    public FloatBuffer getView(){
        return viewf;
    }


    public Camera speedAngX(float speed){
        camPos.x += speed * yawSin;
        camPos.z += speed * -yawCos;
        return this;
    }

    public Camera speedAngZ(float speed){
        camPos.x += speed * -yawCos;
        camPos.z += speed * -yawSin;
        return this;
    }
}
