package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.main.WindowInfo;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public abstract class Camera {
    protected final WindowInfo windowInfo;

    protected final Matrix4f projection, view;

    protected FloatBuffer projectionBuffer, viewBuffer;
    protected final ShaderValue projectionShaderValue, viewShaderValue;

    public Camera(WindowInfo windowInfo) {
        this.windowInfo = windowInfo;

        projection = new Matrix4f();
        view = new Matrix4f();

        projectionBuffer = BufferUtils.createFloatBuffer(16);
        viewBuffer = BufferUtils.createFloatBuffer(16);

        projectionShaderValue = new ShaderValue("projection", FloatBuffer.class, projectionBuffer);
        viewShaderValue = new ShaderValue("view", FloatBuffer.class, viewBuffer);
    }

    public abstract void updateProjection();
    public abstract void updateView();

    public ShaderValue getProjection(){
        projectionShaderValue.setObject(projectionBuffer);
        return projectionShaderValue;
    }

    public Matrix4f getProjectionMatrix(){
        return new Matrix4f(projection);
    }

    public ShaderValue getView(){
        viewShaderValue.setObject(viewBuffer);
        return viewShaderValue;
    }

    public Matrix4f getViewMatrix(){
        return new Matrix4f(view);
    }
}
