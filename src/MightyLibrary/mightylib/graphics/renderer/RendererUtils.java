package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;

public abstract class RendererUtils {
    public static float [] calculatePositionForSquare(Vector2f size, EDirection reference){
        float [] temp = new float[]{
                0, size.y,
                0, 0,
                size.x, 0,
                size.x, size.y
        };


        switch(reference){
            case None:
            case Up:
            case Down:
                temp[0] = -0.5f * size.x; temp[2] = -0.5f* size.x; temp[4] = 0.5f* size.x; temp[6] = 0.5f * size.x;
                break;
            case RightDown:
            case Right:
            case RightUp:
                temp[0] = -1 * size.x; temp[2] = -1 * size.x; temp[4] = 0f * size.x; temp[6] = 0f * size.x;
                break;
        }

        switch(reference){
            case None:
            case Left:
            case Right:
                temp[1] = 0.5f * size.y; temp[3] = -0.5f * size.y; temp[5] = -0.5f * size.y; temp[7] = 0.5f * size.y;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                temp[1] = -1 * size.y; temp[3] = 0f * size.y; temp[5] = 0f * size.y; temp[7] = -1f * size.y;
                break;
        }

        return temp;
    }

    public static int[] indicesForSquare (){
        return new int[] { 0, 1, 2, 2, 0, 3 };
    }
}
