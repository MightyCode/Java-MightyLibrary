package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer.utils.EDrawMode;
import io.github.mightycode.mightylib.utils.math.geometry.Direction;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Matrix4fc;
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

    // The same geometry for the batched draws, kept on the CPU
    private boolean batchIndicesDirty = true;
    private boolean batchPositionDirty = true;
    private boolean batchTextureDirty = true;
    private int[] batchIndices;
    private float[] batchPositions;
    private float[] batchTextures;
    private float[] batchTransformed = new float[0];

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

    // Hook (call super): sends the changed geometry to the mesh, then draws it (drawn alone, not batched)
    @Override
    public void draw() {
        uploadGeometry();

        super.draw();
    }

    /**
     * Batched when its shader has a batch equivalent ("batch" in shaders.json), its depth test is off and its mesh
     * draws triangles: the vertices of calculatePosition transformed by the model, the coordinates of
     * calculateTexturePosition, the triangles of getIndices (asked again when they change, like for the mesh).
     */
    @Override
    protected boolean submitToBatch(DrawBatch2D batch) {
        if (!CanBatch(isDepthTest(), getMesh().getDrawMode()))
            return false;

        refreshBatchGeometry();
        int vertexCount = batchPositions.length / 2;
        if (batchTextures.length < vertexCount * 2)
            return false;

        int state = batchState(batch);
        if (state < 0)
            return false;

        int color = batchColor();
        TransformPositions(modelMatrix(), batchPositions, batchTransformed);
        batch.beginDraw(state, getLayer(), getSortDepth());
        for (int i = 0; i < vertexCount; ++i)
            batch.vertex(batchTransformed[2 * i], batchTransformed[2 * i + 1], batchTextures[2 * i],
                    batchTextures[2 * i + 1], color);
        for (int index : batchIndices)
            batch.index(index);
        batch.endDraw();
        return true;
    }

    // The batch keeps no depth, and draws its indices as a list of triangles (not lines, strips or fans)
    static boolean CanBatch(boolean depthTest, EDrawMode drawMode) {
        return !depthTest && drawMode == EDrawMode.Triangles;
    }

    private void refreshBatchGeometry() {
        if (batchIndicesDirty) {
            batchIndices = getIndices();
            batchIndicesDirty = false;
        }

        if (batchPositionDirty) {
            batchPositions = calculatePosition();
            if (batchTransformed.length != batchPositions.length)
                batchTransformed = new float[batchPositions.length];
            batchPositionDirty = false;
        }

        if (batchTextureDirty) {
            batchTextures = calculateTexturePosition();
            batchTextureDirty = false;
        }
    }

    // The 2D positions (x, y pairs) by the model, like its vertex shader: model * (x, y, 0, 1), affine
    static void TransformPositions(Matrix4fc model, float[] positions, float[] transformed) {
        float m00 = model.m00(), m01 = model.m01(), m10 = model.m10(), m11 = model.m11();
        float m30 = model.m30(), m31 = model.m31();
        for (int i = 0; i < positions.length; i += 2) {
            float x = positions[i];
            float y = positions[i + 1];
            transformed[i] = m00 * x + m10 * y + m30;
            transformed[i + 1] = m01 * x + m11 * y + m31;
        }
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
        batchPositionDirty = true;

        return this;
    }

    public final EDirection getReferenceDirection(){
        return referenceDirection;
    }

    public final Shape2DRenderer setReferencePoint(Vector2f reference){
        referencePoint = reference;
        referenceDirection = Direction.RangedVectorToDirection(reference, REFERENCE_RANGE_DIRECTION);

        positionDirty = true;
        batchPositionDirty = true;

        return this;
    }

    public final Vector2f getReferencePoint() {
        return referencePoint;
    }

    // The part of the texture drawn (x, y, x2, y2 in 0..1), sent at the next display
    public final Shape2DRenderer setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;
        textureDirty = true;
        batchTextureDirty = true;

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
        batchTextureDirty = true;

        return this;
    }

    // Sent at the next display
    public final Shape2DRenderer updateShapeIndices(){
        indicesDirty = true;
        batchIndicesDirty = true;

        return this;
    }

    // Sent at the next display
    public final Shape2DRenderer updateShapePosition(){
        positionDirty = true;
        batchPositionDirty = true;

        return this;
    }
}
