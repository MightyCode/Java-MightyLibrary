package MightyLibrary.mightylib.scenes.camera;

import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.utils.math.UUID;
import org.joml.Matrix4f;

public abstract class Camera extends UUID {
    protected final WindowInfo windowInfo;

    protected final Matrix4f projection, view;
    protected final ShaderValue projectionShaderValue, viewShaderValue;

    private boolean shouldUpdateProjection;
    private boolean shouldUpdateView;

    public Camera(WindowInfo windowInfo) {
        this.windowInfo = windowInfo;

        projection = new Matrix4f();
        view = new Matrix4f();

        projectionShaderValue = new ShaderValue("projection", Matrix4f.class, projection);
        viewShaderValue = new ShaderValue("view", Matrix4f.class, view);

        shouldUpdateProjection = true;
        shouldUpdateView = true;
    }

    public abstract void updateProjection();
    public abstract void updateView();

    public void superUpdateProjection() {
        if (shouldUpdateProjection) {
            updateProjection();
        }
    }

    public void superUpdateView() {
        if (shouldUpdateProjection) {
            updateView();
        }
    }

    public void setShouldUpdateProjection(boolean shouldUpdateProjection) {
        this.shouldUpdateProjection = shouldUpdateProjection;
    }

    public void setShouldUpdateView(boolean shouldUpdateView) {
        this.shouldUpdateView = shouldUpdateView;
    }

    public boolean shouldUpdateProjection() {
        return shouldUpdateProjection;
    }

    public boolean shouldUpdateView() {
        return shouldUpdateView;
    }

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
