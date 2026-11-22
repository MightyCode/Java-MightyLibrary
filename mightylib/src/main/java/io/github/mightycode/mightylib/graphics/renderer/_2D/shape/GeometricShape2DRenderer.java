package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector4f;

/*
 * Base of the vector shapes made of triangles (stroke2DV.glsl): polylines, arrows, polygons. The subclass builds its
 * fills and strokes in a StrokeMesh when they change; the vertex shader extrudes the strokes by their width, so a
 * change of width (or of zoom for screen pixels) costs no new geometry. The stroke is always centered on the line.
 */
abstract class GeometricShape2DRenderer extends VectorShape2DRenderer {
    private final StrokeMesh strokeMesh;
    private final Vector4f bounds = new Vector4f();
    private EStrokeJoin join;
    private float miterLimit;

    private boolean geometryDirty;
    private boolean builtWithStroke;
    // The bases and indices of the last geometry built, for the batch
    private float[] batchBases = new float[0];
    private int[] batchIndices = new int[0];

    GeometricShape2DRenderer(String shaderName) {
        super(shaderName);

        strokeMesh = new StrokeMesh(getMesh());
        join = EStrokeJoin.MITER;
        miterLimit = Stroker.DEFAULT_MITER_LIMIT;
        geometryDirty = true;
    }

    // Builds again the geometry at the next display
    final void geometryChanged() {
        geometryDirty = true;
    }

    // Adds the fills and the strokes of the shape (withStroke: the stroke has a width)
    abstract void buildGeometry(StrokeMesh mesh, boolean withStroke);

    // True when what the geometry depends on changed since it was built (size, reference point)
    boolean geometryOutdated() {
        return false;
    }

    // Builds the geometry when it changed; draws nothing without triangles
    @Override
    public void display() {
        boolean withStroke = getStrokeWidth() > 0f;
        if (geometryDirty || withStroke != builtWithStroke || geometryOutdated()) {
            strokeMesh.clear();
            buildGeometry(strokeMesh, withStroke);
            strokeMesh.writeBounds(bounds);
            strokeMesh.upload(getMesh());
            batchBases = strokeMesh.bases();
            batchIndices = strokeMesh.indices();
            updateShapeIndices();
            updateShapePosition();
            updateShapeTexture();
            geometryDirty = false;
            builtWithStroke = withStroke;
        }

        if (strokeMesh.vertexCount() == 0)
            return;

        super.display();
    }

    @Override
    BatchFormat batchFormat() {
        return BatchFormats.STROKE;
    }

    // Its vertices with the values of the shape on each, then its triangles
    @Override
    void writeBatchDraw(DrawBatch2D batch, int fill) {
        float[] extrusions = strokeMesh.uploadedExtrusions();
        float[] coverages = strokeMesh.uploadedCoverages();
        float[] paints = strokeMesh.uploadedPaints();
        Matrix4fc model = modelMatrix();
        Vector2f size = batchSize();
        Vector4f texture = batchTexturePosition();
        ShapeStroke stroke = batchStroke();
        int strokeColor = stroke.color().toIntRGBA();
        float width = stroke.width(), unit = stroke.unit().shaderValue(), antialiased = isAntialiasing() ? 1f : 0f;
        float textured = batchTextured() ? 1f : 0f;

        int count = batchBases.length / 2;
        for (int i = 0; i < count; ++i) {
            batch.put(batchBases[2 * i], batchBases[2 * i + 1]);
            batch.put(extrusions[4 * i], extrusions[4 * i + 1], extrusions[4 * i + 2], extrusions[4 * i + 3]);
            batch.put(coverages[4 * i], coverages[4 * i + 1], coverages[4 * i + 2], coverages[4 * i + 3]);
            batch.put(paints[2 * i], paints[2 * i + 1]);
            BatchGeometry2D.WriteModelRows(batch, model);
            batch.put(size.x, size.y);
            batch.putInt(fill);
            batch.putInt(strokeColor);
            batch.put(width, unit, antialiased, textured);
            batch.put(texture.x, texture.y, texture.z, texture.w);
            batch.put(bounds.x, bounds.y, bounds.z, bounds.w);
        }
        for (int index : batchIndices)
            batch.index(index);
    }

    @Override
    protected final int[] getIndices() {
        return strokeMesh.indices();
    }

    @Override
    protected final float[] calculatePosition() {
        return strokeMesh.bases();
    }

    // Not read by stroke2DV.glsl (the texture follows the bounding box), as long as the positions
    @Override
    protected final float[] calculateTexturePosition() {
        return new float[strokeMesh.vertexCount() * 2];
    }

    @Override
    protected final void writeShapeParameters(Vector4f first, Vector4f second) {
        first.set(bounds);
    }

    // MITER by default
    public final VectorShape2DRenderer setJoin(EStrokeJoin join) {
        if (join == null)
            throw new IllegalArgumentException("The join can't be null");
        this.join = join;
        geometryChanged();
        return this;
    }

    public final EStrokeJoin getJoin() {
        return join;
    }

    // Longest miter, in widths of the line, beyond which it becomes a bevel (4 by default, like SVG)
    public final VectorShape2DRenderer setMiterLimit(float miterLimit) {
        this.miterLimit = Math.max(miterLimit, 1f);
        geometryChanged();
        return this;
    }

    public final float getMiterLimit() {
        return miterLimit;
    }
}
