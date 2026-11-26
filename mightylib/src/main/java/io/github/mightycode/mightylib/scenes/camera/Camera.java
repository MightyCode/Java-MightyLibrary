package io.github.mightycode.mightylib.scenes.camera;

import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.utils.math.UUID;
import org.joml.Matrix4f;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Camera extends UUID {
    // Ids of the cameras only (UUID ids are shared with renderers), to target a camera with the camera command
    private static final AtomicInteger CAMERA_COUNT = new AtomicInteger();
    private final int cameraId;
    private String name;

    protected final WindowInfo windowInfo;

    // Written only by writeProjection and writeView: their values are tracked (the version marks each change)
    protected final Matrix4f projection, view;
    protected final ShaderValue projectionShaderValue, viewShaderValue;

    // Package-private: the hierarchy is closed, its subclasses are in this package
    Camera(WindowInfo windowInfo) {
        cameraId = CAMERA_COUNT.getAndIncrement();
        name = "camera" + cameraId;
        this.windowInfo = windowInfo;

        projection = new Matrix4f();
        view = new Matrix4f();

        projectionShaderValue = new ShaderValue("projection", Matrix4f.class, projection).trackChanges();
        viewShaderValue = new ShaderValue("view", Matrix4f.class, view).trackChanges();
    }

    public int getCameraId() {
        return cameraId;
    }

    public String getName() {
        return name;
    }

    public Camera setName(String name) {
        this.name = name;
        return this;
    }

    /** Computes the projection again (writeProjection), then marks its value changed. */
    public final void updateProjection() {
        writeProjection();
        projectionShaderValue.markChanged();
    }

    /** Computes the view again (writeView), then marks its value changed. */
    public final void updateView() {
        writeView();
        viewShaderValue.markChanged();
    }

    // Hook called by updateProjection: replaces the matrix projection (the only place it is written)
    protected abstract void writeProjection();

    // Hook called by updateView: writes the matrix view (the only place it is written)
    protected abstract void writeView();

    // The value of the matrix itself: read it, don't change it (its changes are marked by updateProjection)
    public ShaderValue getProjection(){
        // A new version only if another matrix was put in it
        if (projectionShaderValue.getObject() != projection)
            projectionShaderValue.setObject(projection);
        return projectionShaderValue;
    }

    public Matrix4f getProjectionMatrix(){
        return new Matrix4f(projection);
    }

    // The value of the matrix itself: read it, don't change it (its changes are marked by updateView)
    public ShaderValue getView(){
        if (viewShaderValue.getObject() != view)
            viewShaderValue.setObject(view);
        return viewShaderValue;
    }

    public Matrix4f getViewMatrix(){
        return new Matrix4f(view);
    }
}
