package MightyLibrary.mightylib.graphics.shape._2D;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.util.math.MightyMath;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class TextureRenderer extends Renderer {
    protected EDirection reference;
    protected final WindowInfo windowInfo;
    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    public TextureRenderer(WindowInfo info, String shaderName) {
        super(shaderName, true, true);

        windowInfo = info;
        reference = EDirection.LeftUp;

        texturePosition = new Vector4f(0f, 1f, 0f,1f);

        int[] indices = { 0, 1, 2, 2, 0, 3 };
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVbo(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVbo(texturePos(), 2, Shape.STATIC_STORE);
    }


    private float[] calculatePosition(){
        float [] temp = new float[]{
                0, 1,
                0, 0,
                1, 0,
                1, 1
        };

        switch(this.reference){
            case None:
            case Up:
            case Down:
                temp[0] = -0.5f; temp[2] = -0.5f; temp[4] = 0.5f; temp[6] = 0.5f;
                break;
            case RightDown:
            case Right:
            case RightUp:
                temp[0] = -1; temp[2] = -1; temp[4] = 0f; temp[6] = 0f;
                break;
        }

        switch(this.reference){
            case None:
            case Left:
            case Right:
                temp[1] = 0.5f; temp[3] = -0.5f; temp[5] = -0.5f; temp[7] = 0.5f;
                break;
            case LeftDown:
            case Down:
            case RightDown:
                temp[1] = -1; temp[3] = 0; temp[5] = 0f; temp[7] = -1f;
                break;
        }

        return temp;
    }

    public TextureRenderer setReference(EDirection reference){
        this.reference = reference;

        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
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
    public TextureRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }


    public TextureRenderer setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }

    public void updateShape(){
        //shape.updateVbo(calculatePosition(), positionIndex);
        shape.updateVbo(texturePos(), textureIndex);
    }
}
