package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.utils.math.geometry.Direction;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class Shape2DRenderer extends Renderer {
    private final static Vector4f REFERENCE_RANGE_DIRECTION = new Vector4f(0, 0, 1, 1);

    // Reference direction and point are related
    private EDirection referenceDirection;
    private Vector2f referencePoint;

    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    private boolean initialized = false;

    public Shape2DRenderer(String shaderName, boolean useEbo) {
        super(shaderName, useEbo);

        referenceDirection = EDirection.LeftUp;
        referencePoint = new Vector2f();

        texturePosition = new Vector4f();

        positionIndex = shape.addVboFloat(new float[]{}, 2, Shape.STATIC_STORE);
        textureIndex = shape.addVboFloat(new float[]{}, 2, Shape.STATIC_STORE);
    }

    public boolean load(int remainingMilliseconds) {
        int[] indices = getIndices();
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indices);
        shape.updateVbo(calculatePosition(), positionIndex);
        shape.updateVbo(calculateTexturePosition(), textureIndex);

        initialized = true;

        return true;
    }

    @Override
    public void display() {
        if (!initialized) {
            throw new RuntimeException("Shape not initialized : " + this);
        }

        super.display();
    }

    protected abstract int[] getIndices();
    protected abstract float[] calculatePosition();
    protected abstract float[] calculateTexturePosition();

    public Shape2DRenderer setReferenceDirection(EDirection reference){
        if (reference == EDirection.Undef)
            return this;

        this.referenceDirection = reference;
        this.referencePoint = Direction.DirectionToRangedVector(reference, REFERENCE_RANGE_DIRECTION);

        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }

    public EDirection getReferenceDirection(){
        return referenceDirection;
    }

    public Shape2DRenderer setReferencePoint(Vector2f reference){
        referencePoint = reference;
        referenceDirection = Direction.RangedVectorToDirection(reference, REFERENCE_RANGE_DIRECTION);

        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }

    public Vector2f getReferencePoint() {
        return referencePoint;
    }

    public Shape2DRenderer setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;

        return this;
    }

    // Set size with size of pixel
    public Shape2DRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }

    public Shape2DRenderer setSizeToTexture() {
        if (mainTexture == null)
            return this;

        setScale(new Vector3f(mainTexture.getWidth(), mainTexture.getHeight(), 1.0f));

        return this;
    }


    public Shape2DRenderer setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }

    public Vector2f get2DPosition(){
        return new Vector2f(position.x, position.y);
    }

    public Shape2DRenderer updateShapeTexture(){
        shape.updateVbo(calculateTexturePosition(), textureIndex);

        return this;
    }

    public Shape2DRenderer updateShapePosition(){
        shape.updateVbo(calculatePosition(), positionIndex);

        return this;
    }
}
