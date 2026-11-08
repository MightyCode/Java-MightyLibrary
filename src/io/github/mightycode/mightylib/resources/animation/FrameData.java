package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.resources.TextFormat;
import org.joml.Vector2i;
import org.joml.Vector4i;

public class FrameData {
    private static final int PART_NUMBER = 7;

    // 4 coordinates
    private final Vector4i framePosition;
    private final Vector2i hotPoint;
    private float frameTime;

    public FrameData(){
        framePosition = new Vector4i(0, 0, 0, 0);
        hotPoint = new Vector2i(0, 0);
        frameTime = 0f;
    }

    // String data is composed of : x, y, width, height, hotPointX, hotPointY, frameTime
    // Throws a ResourceException (with path and line) when the frame is malformed
    public void init(String data, String path, int line){
        String[] parts = data.split(",");
        if (parts.length != PART_NUMBER)
            throw TextFormat.error(path, line, "a frame needs " + PART_NUMBER
                    + " comma-separated values, got " + parts.length + " ('" + data + "')");

        framePosition.x = TextFormat.integer(parts, 0, path, line, "frame x");
        framePosition.y = TextFormat.integer(parts, 1, path, line, "frame y");
        framePosition.z = TextFormat.integer(parts, 2, path, line, "frame width");
        framePosition.w = TextFormat.integer(parts, 3, path, line, "frame height");

        hotPoint.x = TextFormat.integer(parts, 4, path, line, "hot point x");
        hotPoint.y = TextFormat.integer(parts, 5, path, line, "hot point y");

        frameTime = TextFormat.decimal(parts, 6, path, line, "frame time");
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
