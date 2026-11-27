package io.github.mightycode.mightylib.scenes.camera;

import org.joml.Vector3f;

public final class Camera3DCreationInfo {
    // Vertical view angle, in degrees
    public float fovDegrees;
    public Vector3f initialPosition;

    public Camera3DCreationInfo(){}

    public Camera3DCreationInfo(float fovDegrees, Vector3f initialPosition){
        this.fovDegrees = fovDegrees;
        this.initialPosition = initialPosition;
    }
}
