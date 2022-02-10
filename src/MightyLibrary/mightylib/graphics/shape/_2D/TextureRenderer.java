package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.MightyMath;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class TextureRenderer extends Renderer {
    protected final WindowInfo windowInfo;
    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    public TextureRenderer(WindowInfo info, String shaderName) {
        super(shaderName, true, true);

        windowInfo = info;

        texturePosition = new Vector4f(0f, 1f, 0f,1f);

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.STATIC_STORE);
    }


    private float[] calculatePosition(){
        return  new float[]{
                0, 1,
                0, 0,
                1, 0,
                1, 1
        };
    }


    public void setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;
    }


    private float[] texturePos(){
        return new float[]{
                texturePosition.x, texturePosition.w,
                texturePosition.x, texturePosition.z,
                texturePosition.y, texturePosition.z,
                texturePosition.y, texturePosition.w
        };
    }

    // Set size with size of pixel
    public void setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));
    }


    public void setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));
    }

    public void updateShape(){
        //shape.updateVbo(calculatePosition(), positionIndex);
        shape.updateVbo(texturePos(), textureIndex);
    }
}
