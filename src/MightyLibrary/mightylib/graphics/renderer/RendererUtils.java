package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.math.EFlip;
import MightyLibrary.mightylib.util.math.ERotation;
import org.joml.Vector2f;
import org.joml.Vector4f;

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

    public static float [] texturePosition(Vector4f texturePosition, EFlip flip, ERotation rotation){
        float[] result = new float[]{
                texturePosition.x, texturePosition.w,
                texturePosition.x, texturePosition.z,
                texturePosition.y, texturePosition.z,
                texturePosition.y, texturePosition.w
        };

        // Apply flip transformation
        switch (flip) {
            case Horizontal:
            case HorizontalVertical:
                result = new float[]{
                        result[2], result[3],
                        result[0], result[1],
                        result[6], result[7],
                        result[4], result[5]
                };
                break;
            default:
                break;
        }

        switch(flip){
            case Vertical:
            case HorizontalVertical:
                result = new float[]{
                        result[6], result[7],
                        result[4], result[5],
                        result[2], result[3],
                        result[0], result[1],
                };
                break;
            default:
                break;
        }

        for (int i = rotation.ordinal(); i > 0; --i){
            result = new float[]{
                    result[6], result[7],
                    result[0], result[1],
                    result[2], result[3],
                    result[4], result[5],
            };
        }

        return result;
    }
}
