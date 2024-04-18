package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.utils.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class RendererUtils {
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
                for (int i = 0; i < temp.length; i += 2)
                    temp[i] -= 0.5f * size.x;
                break;
            case RightDown:
            case Right:
            case RightUp:
                for (int i = 0; i < temp.length; i += 2)
                    temp[i] -= size.x;
                break;
        }

        switch(reference){
            case None:
            case Left:
            case Right:
                for (int i = 1; i < temp.length; i += 2)
                    temp[i] -= 0.5f * size.y;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                for (int i = 1; i < temp.length; i += 2)
                    temp[i] -= size.y;
                break;
        }

        return temp;
    }

    public static float [] calculatePositionForHexagon(Vector2f size, EDirection reference){
        float [] temp = new float[]{
                0.25f * size.x, 0f,         // TOP LEFT
                0.75f * size.x, 0f,         // TOP RIGHT
                size.x, size.y * 0.5f,      // MIDDLE RIGHT
                0.75f * size.x, size.y,     // BOTTOM RIGHT
                0.25f * size.x, size.y,     // BOTTOM LEFT
                0, size.y * 0.5f            // MIDDLE LEFT
        };

        switch(reference){
            case None:
            case Up:
            case Down:
                for (int i = 0; i < temp.length; i += 2)
                    temp[i] -= 0.5f * size.x;
                break;
            case RightDown:
            case Right:
            case RightUp:
                for (int i = 0; i < temp.length; i += 2)
                    temp[i] -= size.x;
                break;
        }

        switch(reference){
            case None:
            case Left:
            case Right:
                for (int i = 1; i < temp.length; i += 2)
                    temp[i] -= 0.5f * size.y;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                for (int i = 1; i < temp.length; i += 2)
                    temp[i] -= size.y;
                break;
        }

        return temp;
    }


    public static Vector4f ToLightDirection(Vector3f lightVector) {
        return new Vector4f(lightVector.x, lightVector.y, lightVector.z, 1.0f);
    }

    public static Vector4f ToLightPosition(Vector3f lightVector) {
        return new Vector4f(lightVector.x, lightVector.y, lightVector.z, 0.0f);
    }
}
