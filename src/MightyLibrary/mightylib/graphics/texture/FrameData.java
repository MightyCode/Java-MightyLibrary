package MightyLibrary.mightylib.graphics.texture;

import org.joml.Vector2i;
import org.joml.Vector4i;

public class FrameData {
    private static final int PART_NUMBER = 7;

    // 4 coords
    private final Vector4i framePosition;
    private final Vector2i hotPoint;
    private float frameTime;

    public FrameData(){
        framePosition = new Vector4i(0, 0, 0, 0);
        hotPoint = new Vector2i(0, 0);
        frameTime = 0f;
    }

    public void init(String data){
        String[] parts = data.split(",");
        if (parts.length != PART_NUMBER) {
            System.err.println("Error when trying to init Frame Data");
            System.err.println("-> : " + data);
            return;
        }

        framePosition.x = Integer.parseInt(parts[0].trim());
        framePosition.y = Integer.parseInt(parts[1].trim());
        framePosition.z = Integer.parseInt(parts[2].trim());
        framePosition.w = Integer.parseInt(parts[3].trim());

        hotPoint.x = Integer.parseInt(parts[4].trim());
        hotPoint.y = Integer.parseInt(parts[5].trim());

        frameTime = Float.parseFloat(parts[6].trim());
    }


    public Vector2i getSize(){
        return new Vector2i(framePosition.z, framePosition.w);
    }


    public Vector4i getFramePositionReference() {
        return framePosition;
    }
    public Vector4i getFramePositionCopy(){
        return new Vector4i(framePosition);
    }

    public Vector2i getHotPointReference() {
        return hotPoint;
    }
    public Vector2i getHotPointCopy(){
        return new Vector2i(hotPoint);
    }

    public float getFrameTime(){
        return frameTime;
    }
}
