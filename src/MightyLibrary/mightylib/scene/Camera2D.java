package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.main.WindowInfo;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class Camera2D {
    private final WindowInfo windowInfo;

    private final Matrix4f projectionOrtho, view;
    private final Vector2f camPos;

    private final FloatBuffer projectionOrthoBuffer, viewBuffer;

    public Camera2D(WindowInfo windowInfo, Vector2f pos){
        this.windowInfo = windowInfo;

        camPos = new Vector2f();

        projectionOrtho = new Matrix4f();

        view = new Matrix4f();
        projectionOrthoBuffer = BufferUtils.createFloatBuffer(16);

        viewBuffer = BufferUtils.createFloatBuffer(16);

        SetProjection();
        setPos(pos);
    }


    private void SetProjection(){
        projectionOrtho.ortho(0, windowInfo.getVirtualSizeRef().x, windowInfo.getVirtualSizeRef().y, 0 , -1, 1);
        projectionOrtho.get(projectionOrthoBuffer);
    }

    public Vector2f getCamPosRef() { return camPos; }
    public Vector2f getCamPosCopy() { return new Vector2f(camPos); }

    public void moveX(float value){
        setX(camPos.x + value);
    }

    public void moveY(float value){
        setX(camPos.y + value);
    }

    public void setPos(Vector2f newPos){
        camPos.x = newPos.x;
        camPos.y = newPos.y;

        view.identity();
        view.translate(new Vector3f(camPos.x, camPos.y, 0));
        view.get(viewBuffer);
    }

    public void setX(float x){
        camPos.x = x;

        view.identity();
        view.translate(new Vector3f(camPos.x, camPos.y, 0));
        view.get(viewBuffer);
    }

    public void setY(float y){
        camPos.y = y;

        view.identity();
        view.translate(new Vector3f(camPos.x, camPos.y, 0));
        view.get(viewBuffer);
    }

    public FloatBuffer getProjection(){
        return projectionOrthoBuffer;
    }

    public FloatBuffer getView(){
        return viewBuffer;
    }
}
