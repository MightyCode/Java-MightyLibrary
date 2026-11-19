package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

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
