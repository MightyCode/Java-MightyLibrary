package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.utils.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class ShapeRenderer extends Renderer {
    protected EDirection reference;
    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    public ShapeRenderer(String shaderName, boolean useEbo) {
        super(shaderName, useEbo);

        reference = EDirection.LeftUp;
        texturePosition = new Vector4f();

        prepare();

        int[] indices = getIndices();
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        positionIndex = shape.addVboFloat(calculatePosition(), 2, Shape.STATIC_STORE);
        textureIndex = shape.addVboFloat(calculateTexturePosition(), 2, Shape.STATIC_STORE);
    }

    protected abstract void prepare();

    protected abstract int[] getIndices();
    protected abstract float[] calculatePosition();
    protected abstract float[] calculateTexturePosition();

    public ShapeRenderer setReference(EDirection reference){
        this.reference = reference;

        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }


    public ShapeRenderer setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;

        return this;
    }

    // Set size with size of pixel
    public ShapeRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }

    public ShapeRenderer setSizeToTexture() {
        if (textures[0] == null)
            return this;

        setScale(new Vector3f(textures[0].getWidth(), textures[0].getHeight(), 1.0f));

        return this;
    }


    public ShapeRenderer setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }

    public Vector2f get2DPosition(){
        return new Vector2f(position.x, position.y);
    }

    public ShapeRenderer updateShapeTexture(){
        shape.updateVbo(calculateTexturePosition(), textureIndex);

        return this;
    }

    public ShapeRenderer updateShapePosition(){
        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }
}
