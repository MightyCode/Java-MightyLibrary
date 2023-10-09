package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.util.math.EFlip;
import MightyLibrary.mightylib.util.math.ERotation;
import org.joml.Vector4f;

public class RectangularFace {
    private EFlip flip;
    private ERotation rotation;

    public RectangularFace(){
        flip = EFlip.None;
        rotation = ERotation.None;
    }

    public EFlip getFlip() { return flip; }

    public RectangularFace setFlip(EFlip flip) {
        this.flip = flip;

        return  this;
    }

    public ERotation getRotation() { return rotation; }

    public RectangularFace setRotation(ERotation rotation) {
        this.rotation = rotation;

        return  this;
    }

    public static Vector4f BasicTexturePosition(){ return new Vector4f(0f, 1f, 0f,1f); }

    public static int[] IndicesForSquare (){
        return new int[] { 0, 3, 2, 0, 2, 1 };
    }

    public float [] applyRotationFlip(float[] cornersPosition) {
        // Apply flip transformation
        float[] result = new float[cornersPosition.length];
        System.arraycopy(cornersPosition, 0, result, 0, cornersPosition.length);

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

    public float [] textureCornerPosition(Vector4f texturePosition){
        return applyRotationFlip(new float[]{
                texturePosition.x, texturePosition.w,
                texturePosition.x, texturePosition.z,
                texturePosition.y, texturePosition.z,
                texturePosition.y, texturePosition.w,
        });
    }
}
