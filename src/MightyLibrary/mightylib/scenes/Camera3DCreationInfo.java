package MightyLibrary.mightylib.scenes;

import org.joml.Vector3f;

public class Camera3DCreationInfo {
    public float fov;
    public Vector3f initialPosition;

    public Camera3DCreationInfo(){}

    public Camera3DCreationInfo(float fov, Vector3f initialPosition){
        this.fov = fov;
        this.initialPosition = initialPosition;
    }
}
