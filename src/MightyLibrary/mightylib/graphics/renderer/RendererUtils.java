package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.math.EFlip;
import MightyLibrary.mightylib.util.math.ERotation;
import org.joml.Vector2f;
import org.joml.Vector4f;

public abstract class RendererUtils {
    public static Vector4f BasicTexturePosition(){ return new Vector4f(0f, 1f, 0f,1f); }

    public static float [] calculatePositionForSquare(Vector2f size, EDirection reference){
        float [] temp = new float[]{
                0, size.y,
                0, 0,
                size.x, 0,
                size.x, size.y,
        };

        switch(reference){
            case None:
            case Up:
            case Down:
                for (int i = 0; i < 8; i += 2)
                    temp[i] -= 0.5f * size.x;
                break;
            case RightDown:
            case Right:
            case RightUp:
                for (int i = 0; i < 8; i += 2)
                    temp[i] -= size.x;
                break;
        }

        switch(reference){
            case None:
            case Left:
            case Right:
                for (int i = 1; i < 8; i += 2)
                    temp[i] -= 0.5f * size.y;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                for (int i = 1; i < 8; i += 2)
                    temp[i] -= size.y;
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
                texturePosition.y, texturePosition.w,
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
