package MightyLibrary.mightylib.scenes;

import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.utils.math.EDirection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Camera2D extends Camera {
    private final Vector2f camPos;

    private final Vector2f zoomReference;
    private Vector2f zoomLevel = new Vector2f(1, 1);
    private float minZoomLevel = 0.0001f;
    private float maxZoomLevel = 1000.0f;
    private float rotation;

    private boolean invertView; // y-axis

    public Camera2D(WindowInfo windowInfo, Vector2f pos){
        this(windowInfo, pos, false);
    }

    public Camera2D(WindowInfo windowInfo, Vector2f pos, boolean invertView){
        super(windowInfo);
        camPos = new Vector2f();
        zoomReference = new Vector2f();

        rotation = 0;
        this.invertView = invertView;

        updateProjection();
        setPos(pos);
        setZoomReference(EDirection.None);
    }

    public Camera2D(WindowInfo windowInfo, Vector2f pos, int minZoomLevel, int maxZoomLevel){
        this(windowInfo, pos);

        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
    }

    public void invertView(){
        invertView = !invertView;

        updateProjection();
    }

    public void setZoomReference(Vector2f reference){
        setZoomReferenceX(reference.x);
        setZoomReferenceY(reference.y);
    }

    public void setZoomReferenceX(float x){
        zoomReference.x = x;
    }

    public void setZoomReferenceY(float y){
        zoomReference.y = y;
    }

    public void setZoomReference(EDirection direction){
        switch (direction){
            case Left:
            case LeftUp:
            case LeftDown:
                setZoomReferenceX(0);
                break;
            case None:
            case Up:
            case Down:
                setZoomReferenceX(0.5f);
                break;
            case Right:
            case RightUp:
            case RightDown:
                setZoomReferenceX(1f);
                break;
        }

        switch (direction){
            case LeftUp:
            case RightUp:
            case Up:
                setZoomReferenceY(0);
                break;
            case None:
            case Left:
            case Right:
                setZoomReferenceY(0.5f);
                break;
            case RightDown:
            case LeftDown:
            case Down:
                setZoomReferenceY(1f);
                break;
        }
    }

    @Override
    public void updateProjection() {
        float left = 0;
        float right = windowInfo.getVirtualSizeRef().x;
        float bottom = (invertView) ? windowInfo.getVirtualSizeRef().y : 0;
        float top = (invertView) ? 0 : windowInfo.getVirtualSizeRef().y;

        projection.ortho(left, right, top, bottom, -10, 10);
    }

    @Override
    public void updateView(){
        view.identity();
        view.translate(-camPos.x, -camPos.y, 0f);
        view.scaleLocal(zoomLevel.x, zoomLevel.y, 1);
        view.rotate(rotation, new Vector3f(0, 0, 1));
    }

    public Vector2f getCamPosRef() { return camPos; }
    public Vector2f getCamPosCopy() { return new Vector2f(camPos); }

    public void moveX(float value){
        setX(camPos.x + value);
    }

    public void moveY(float value){
        setX(camPos.y + value);
    }

    public void moveXinZoom(float value){
        setX(camPos.x + value / zoomLevel.x);
    }

    public void moveYinZoom(float value){
        setY(camPos.y + value / zoomLevel.y);
    }

    public void setPos(Vector2f newPos){
        camPos.x = newPos.x;
        camPos.y = newPos.y;

        updateView();
    }

    public void setX(float x){
        camPos.x = x;

        updateView();
    }

    public void setY(float y){
        camPos.y = y;

        updateView();
    }

    public void setZoomLevel(float zoomLevel) {
        setZoomLevel(new Vector2f(zoomLevel, zoomLevel));
    }

    public void setZoomLevelX(float zoomLevel) {
        setZoomLevel(new Vector2f(zoomLevel, this.zoomLevel.y));
    }

    public void setZoomLevelY(float zoomLevel) {
        setZoomLevel(new Vector2f(this.zoomLevel.x, zoomLevel));
    }


    public void setZoomLevel(Vector2f zoomLevel) {
        Vector2f previousZoom = new Vector2f(this.zoomLevel);

        if (zoomLevel.x < minZoomLevel)
            zoomLevel.x = minZoomLevel;
        if (zoomLevel.x > maxZoomLevel)
            zoomLevel.x = maxZoomLevel;

        if (zoomLevel.y < minZoomLevel)
            zoomLevel.y = minZoomLevel;
        if (zoomLevel.y > maxZoomLevel)
            zoomLevel.y = maxZoomLevel;

        this.zoomLevel = zoomLevel;

        float previousWidth = windowInfo.getVirtualSizeRef().x / previousZoom.x;
        float previousHeight = windowInfo.getVirtualSizeRef().y / previousZoom.y;

        float newWidth = windowInfo.getVirtualSizeRef().x / zoomLevel.x;
        float newHeight = windowInfo.getVirtualSizeRef().y / zoomLevel.y;

        float dx = previousWidth - newWidth;
        float dy = previousHeight - newHeight;

        setPos(new Vector2f(camPos.x + dx * zoomReference.x, camPos.y + dy * zoomReference.y));

        updateView();
    }

    public Vector2f getZoomLevel() {
        return zoomLevel;
    }

    public void setRotation(float newRotation){
        rotation = newRotation;
        updateView();
    }

    public float getRotation(){
        return rotation;
    }

    public Vector2f getPosition(Vector2f position){
        Vector4f computedPosition = view.invert(new Matrix4f())
                .transform(new Vector4f(position, 0.0f, 1.0f), new Vector4f());

        return new Vector2f(computedPosition.x, computedPosition.y);
    }
}
