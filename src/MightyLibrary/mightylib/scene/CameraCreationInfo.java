package MightyLibrary.mightylib.scene;

import org.joml.Vector3f;

public class CameraCreationInfo {
    public float fov;
    public Vector3f initialPosition;

    public CameraCreationInfo(){}

    public CameraCreationInfo(float fov, Vector3f initialPosition){
        this.fov = fov;
        this.initialPosition = initialPosition;
    }
}
