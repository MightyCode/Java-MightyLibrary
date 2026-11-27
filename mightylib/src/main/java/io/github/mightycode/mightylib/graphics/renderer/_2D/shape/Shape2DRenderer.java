package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.ViewCullBounds;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import io.github.mightycode.mightylib.utils.math.geometry.Direction;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

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
    // The region of the texture read (setTextureRegion), null without limit; its vbo added at the first region (-1
    // before, and for a subclass whose mesh has another attribute on its channel: its program reads no region)
    private Vector4f textureRegion;
    private int regionIndex = -1;
    private int meshVertexCount;

    // The geometry is sent at the next display: subclasses' fields are not set yet in this constructor,
    // and several changes in a tick cost a single upload
    private boolean indicesDirty = true;
    private boolean positionDirty = true;
    private boolean textureDirty = true;
    private boolean regionDirty;

    // The same geometry for the batched draws, kept on the CPU
    private boolean batchIndicesDirty = true;
    private boolean batchPositionDirty = true;
    private boolean batchTextureDirty = true;
    private int[] batchIndices;
    private float[] batchPositions;
    private float[] batchTextures;
    // Box of batchPositions (view culling), computed with them
    private final float[] batchBox = new float[4];
    private boolean hasBatchBox;

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
            float[] positions = calculatePosition();
            mesh.updateVbo(positions, positionIndex);
            // The regions follow the count of vertices
            regionDirty |= meshVertexCount != positions.length / 2;
            meshVertexCount = positions.length / 2;
            positionDirty = false;
        }

        if (regionDirty) {
            uploadRegions(mesh);
            regionDirty = false;
        }

        if (textureDirty) {
            mesh.updateVbo(calculateTexturePosition(), textureIndex);
            textureDirty = false;
        }
    }

    // The region of each vertex (none: zeros, no limit), its vbo added at the first region
    private void uploadRegions(Mesh mesh) {
        if (regionIndex < 0 && (textureRegion == null || mesh.hasChannel(Mesh.COMMON_TEXTURE_REGION_CHANNEL)))
            return;

        float[] regions = new float[meshVertexCount * 4];
        if (textureRegion != null) {
            for (int vertex = 0; vertex < meshVertexCount; ++vertex) {
                regions[4 * vertex] = textureRegion.x;
                regions[4 * vertex + 1] = textureRegion.y;
                regions[4 * vertex + 2] = textureRegion.z;
                regions[4 * vertex + 3] = textureRegion.w;
            }
        }
        if (regionIndex < 0)
            regionIndex = mesh.addVboFloat(Mesh.COMMON_TEXTURE_REGION_CHANNEL, regions, 4, Mesh.STATIC_STORE);
        else
            mesh.updateVbo(regions, regionIndex);
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
     * calculateTexturePosition, the triangles of getIndices (asked again when they change, like for the mesh), read
     * only inside the region of setTextureRegion.
     */
    @Override
    protected boolean submitToBatch(DrawBatch2D batch) {
        if (!BatchGeometry2D.CanBatch(isDepthTest(), getMesh().getDrawMode()))
            return false;

        refreshBatchGeometry();
        int vertexCount = batchPositions.length / 2;
        if (batchTextures.length < vertexCount * 2)
            return false;

        int place = batchPlace(batch);
        if (place < 0)
            return false;
        if (viewCulled(batch, place))
            return true;

        int state = batchState(place);

        BatchGeometry2D.WriteTriangles(batch, state, getLayer(), getSortDepth(), modelMatrix(), batchPositions,
                batchTextures, null, vertexCount, batchColor(), batchIndices, batchIndices.length, textureRegion);
        return true;
    }

    private void refreshBatchGeometry() {
        if (batchIndicesDirty) {
            batchIndices = getIndices();
            batchIndicesDirty = false;
        }

        if (batchPositionDirty) {
            batchPositions = calculatePosition();
            hasBatchBox = BatchGeometry2D.Box(batchPositions, batchPositions.length / 2, batchBox);
            batchPositionDirty = false;
        }

        if (batchTextureDirty) {
            batchTextures = calculateTexturePosition();
            batchTextureDirty = false;
        }
    }

    /**
     * Hook (see Renderer): the box of the positions of calculatePosition, computed with them; a subclass whose
     * shader moves the vertices widens it (call super, then out.margin).
     */
    @Override
    protected boolean viewCullBounds(ViewCullBounds out) {
        if (!hasBatchBox)
            return false;

        out.local(batchBox[0], batchBox[1], batchBox[2], batchBox[3]);
        return true;
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

    /**
     * The part of the texture drawn (left, right, top, bottom in texture coordinates), sent at the next display; read
     * without limit (beyond 0..1, a repeated texture), the region of setTextureRegion removed.
     */
    public final Shape2DRenderer setTexturePosition(Vector4f newTexturePosition){
        texturePosition = newTexturePosition;
        textureDirty = true;
        batchTextureDirty = true;
        if (textureRegion != null) {
            textureRegion = null;
            regionDirty = true;
        }

        return this;
    }

    /**
     * A region of the texture drawn (left, right, top, bottom in texture coordinates, like setTexturePosition), read
     * only inside it: its outer rows and columns whole, never a texel of a neighbor region (the frames of a sheet,
     * the tiles of an atlas that touch each other). The vector shapes read no region (their programs: no limit).
     */
    public final Shape2DRenderer setTextureRegion(Vector4fc region){
        texturePosition = new Vector4f(region);
        textureRegion = new Vector4f(Math.min(region.x(), region.y()), Math.max(region.x(), region.y()),
                Math.min(region.z(), region.w()), Math.max(region.z(), region.w()));
        textureDirty = true;
        batchTextureDirty = true;
        regionDirty = true;

        return this;
    }

    // The region of setTextureRegion, null without limit
    public final Vector4fc getTextureRegion() {
        return textureRegion;
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
