package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.utils.math.geometry.Direction;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * A 2D shape drawn by a MeshRenderer: positions and texture coordinates computed by the subclass (hooks
 * getIndices, calculatePosition, calculateTexturePosition), sent at the next display when they changed.
 * A subclass calls updateShapePosition or updateShapeTexture when what they compute changes.
 */
public abstract class Shape2DRenderer extends MeshRenderer {
    private final static Vector4f REFERENCE_RANGE_DIRECTION = new Vector4f(0, 0, 1, 1);

    // Reference direction and point are related
    private EDirection referenceDirection;
    private Vector2f referencePoint;

    private final int positionIndex, textureIndex;
    private Vector4f texturePosition;

    // The geometry is sent at the next display: subclasses' fields are not set yet in this constructor,
    // and several changes in a tick cost a single upload
    private boolean indicesDirty = true;
    private boolean positionDirty = true;
    private boolean textureDirty = true;

    public Shape2DRenderer(String shaderName, boolean useEbo) {
        this(new Material(shaderName), useEbo);
    }

    // Drawn with the material (shared with its other renderers)
    public Shape2DRenderer(Material material, boolean useEbo) {
        super(material, useEbo);

        referenceDirection = EDirection.LeftUp;
        referencePoint = new Vector2f();

        texturePosition = new Vector4f();

        Mesh mesh = getMesh();
        positionIndex = mesh.addVboFloat(new float[]{}, 2, Mesh.STATIC_STORE);
        textureIndex = mesh.addVboFloat(new float[]{}, 2, Mesh.STATIC_STORE);
    }

    private void uploadGeometry() {
        Mesh mesh = getMesh();
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

    // Hook (call super): sends the changed geometry, then draws
    @Override
    public void display() {
        uploadGeometry();

        super.display();
    }

    // Hook: the indices of the triangles, asked at the first display and again after updateShapeIndices
    protected abstract int[] getIndices();
    // Hook: the positions of the vertices (2 floats each), asked again after updateShapePosition
    protected abstract float[] calculatePosition();
    // Hook: the texture coordinates of the vertices (2 floats each), asked again after updateShapeTexture
    protected abstract float[] calculateTexturePosition();

    // Hook (call super): the point of the shape placed at its position; a subclass that follows it redefines it
    public Shape2DRenderer setReferenceDirection(EDirection reference){
        if (reference == EDirection.Undef)
            return this;

        this.referenceDirection = reference;
        this.referencePoint = Direction.DirectionToRangedVector(reference, REFERENCE_RANGE_DIRECTION);

        positionDirty = true;

        return this;
    }

    public final EDirection getReferenceDirection(){
        return referenceDirection;
    }

    public final Shape2DRenderer setReferencePoint(Vector2f reference){
        referencePoint = reference;
        referenceDirection = Direction.RangedVectorToDirection(reference, REFERENCE_RANGE_DIRECTION);

        positionDirty = true;

        return this;
    }

    public final Vector2f getReferencePoint() {
        return referencePoint;
    }

    // The part of the texture drawn (x, y, x2, y2 in 0..1), sent at the next display
    public final Shape2DRenderer setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;
        textureDirty = true;

        return this;
    }

    public final Vector4f getTexturePosition() {
        return texturePosition;
    }

    // Set size with size of pixel
    public final Shape2DRenderer setSizePix(Vector2f newSize){
        return setSizePix(newSize.x, newSize.y);
    }

    // Hook (call super): a subclass that follows the size redefines it
    public Shape2DRenderer setSizePix(float width, float height){
        setScale(new Vector3f(width, height, 1.0f));

        return this;
    }

    public final Shape2DRenderer setSizeToTexture() {
        IRenderTextureBindable mainTexture = getMainTexture();
        if (mainTexture == null)
            return this;

        setScale(new Vector3f(mainTexture.getWidth(), mainTexture.getHeight(), 1.0f));

        return this;
    }


    // Hook (call super): a subclass that follows the position redefines it
    public Shape2DRenderer setPosition(Vector2f position){
        super.setPosition(new Vector3f(position.x, position.y, 0.0f));

        return this;
    }

    public final Vector2f get2DPosition(){
        return new Vector2f(position().x, position().y);
    }

    // Sent at the next display
    public final Shape2DRenderer updateShapeTexture(){
        textureDirty = true;

        return this;
    }

    // Sent at the next display
    public final Shape2DRenderer updateShapeIndices(){
        indicesDirty = true;

        return this;
    }

    // Sent at the next display
    public final Shape2DRenderer updateShapePosition(){
        positionDirty = true;

        return this;
    }
}
