package MightyLibrary.mightylib.scene;

import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.main.WindowInfo;
import org.joml.Matrix4f;

public abstract class Camera {
    protected final WindowInfo windowInfo;

    protected final Matrix4f projection, view;
    protected final ShaderValue projectionShaderValue, viewShaderValue;

    public Camera(WindowInfo windowInfo) {
        this.windowInfo = windowInfo;

        projection = new Matrix4f();
        view = new Matrix4f();

        projectionShaderValue = new ShaderValue("projection", Matrix4f.class, projection);
        viewShaderValue = new ShaderValue("view", Matrix4f.class, view);
    }

    public abstract void updateProjection();
    public abstract void updateView();

    public ShaderValue getProjection(){
        projectionShaderValue.setObject(projection);
        return projectionShaderValue;
    }

    public Matrix4f getProjectionMatrix(){
        return new Matrix4f(projection);
    }

    public ShaderValue getView(){
        viewShaderValue.setObject(view);
        return viewShaderValue;
    }

    public Matrix4f getViewMatrix(){
        return new Matrix4f(view);
    }
}
