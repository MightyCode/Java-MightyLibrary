package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.utils.math.geometry.Direction;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class Shape2DRenderer extends MeshRenderer {
    private final static Vector4f REFERENCE_RANGE_DIRECTION = new Vector4f(0, 0, 1, 1);

    // Reference direction and point are related
    private EDirection referenceDirection;
    private Vector2f referencePoint;

    protected final int positionIndex, textureIndex;
    protected Vector4f texturePosition;

    // The geometry is sent at the next display: subclasses' fields are not set yet in this constructor,
    // and several changes in a tick cost a single upload
    private boolean indicesDirty = true;
    private boolean positionDirty = true;
    private boolean textureDirty = true;

    public Shape2DRenderer(String shaderName, boolean useEbo) {
        super(shaderName, useEbo);

        referenceDirection = EDirection.LeftUp;
        referencePoint = new Vector2f();

        texturePosition = new Vector4f();

        positionIndex = mesh.addVboFloat(new float[]{}, 2, Mesh.STATIC_STORE);
        textureIndex = mesh.addVboFloat(new float[]{}, 2, Mesh.STATIC_STORE);
    }

    private void uploadGeometry() {
        if (indicesDirty) {
            mesh.setEboStorage(Mesh.STATIC_STORE);
            mesh.setEbo(getIndices());
            indicesDirty = false;
        }

        if (positionDirty) {
            mesh.updateVbo(calculatePosition(), positionIndex);
            positionDirty = false;
        }

        if (textureDirty) {
            mesh.updateVbo(calculateTexturePosition(), textureIndex);
            textureDirty = false;
        }
    }

    @Override
    public void display() {
        uploadGeometry();

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

        positionDirty = true;

        return this;
    }

    public EDirection getReferenceDirection(){
        return referenceDirection;
    }

    public Shape2DRenderer setReferencePoint(Vector2f reference){
        referencePoint = reference;
        referenceDirection = Direction.RangedVectorToDirection(reference, REFERENCE_RANGE_DIRECTION);

        positionDirty = true;

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
    public Shape2DRenderer setSizePix(Vector2f newSize){
        setScale(new Vector3f(newSize.x, newSize.y, 1.0f));

        return this;
    }

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

    // Sent at the next display
    public Shape2DRenderer updateShapeTexture(){
        textureDirty = true;

        return this;
    }

    // Sent at the next display
    public Shape2DRenderer updateShapePosition(){
        positionDirty = true;

        return this;
    }
}
