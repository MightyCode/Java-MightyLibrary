package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.ViewCullBounds;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import io.github.mightycode.mightylib.resources.EngineResources;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Many points of the same size, round or square, with smooth edges and a stroke, drawn in a single call (one quad
 * per point). The positions are given relative to the position of the renderer (its rotation applies to them); the
 * size of the points is setPointSize (world units), not the size of the shape. A texture covers each point.
 */
public final class PointsRenderer extends VectorShape2DRenderer {
    private static final int CORNER_CHANNEL = 2;
    private static final float[] CORNERS = {-1f, -1f, -1f, 1f, 1f, 1f, 1f, -1f};

    private final List<Vector2f> points = new ArrayList<>();
    private final int cornerIndex;
    private boolean cornersDirty;
    private float pointSize;
    private EPointShape pointShape;
    // Box of the centers (view culling), computed again when they change
    private final float[] centersBox = new float[4];
    private boolean centersBoxDirty = true;
    private boolean hasCentersBox;

    public PointsRenderer() {
        this(EngineResources.POINTS_2D);
    }

    // A shader of the project, with the attributes and parameters of points2DV.glsl and points2DF.glsl
    public PointsRenderer(String shaderName) {
        super(shaderName);

        pointSize = 8f;
        pointShape = EPointShape.ROUND;
        cornerIndex = getMesh().addVboFloat(CORNER_CHANNEL, new float[]{}, 2, Mesh.DYNAMIC_STORE);
    }

    // Replaces the points (copied)
    public PointsRenderer setPoints(Collection<Vector2f> newPoints) {
        points.clear();
        for (Vector2f point : newPoints)
            points.add(new Vector2f(point));

        return pointsChanged();
    }

    public PointsRenderer addPoint(Vector2f point) {
        points.add(new Vector2f(point));
        return pointsChanged();
    }

    public PointsRenderer clearPoints() {
        points.clear();
        return pointsChanged();
    }

    // Moves the point of this index
    public PointsRenderer setPoint(int index, Vector2f point) {
        points.get(index).set(point);
        updateShapePosition();
        centersBoxDirty = true;
        return this;
    }

    public int pointCount() {
        return points.size();
    }

    // A copy of the point of this index
    public Vector2f getPoint(int index) {
        return new Vector2f(points.get(index));
    }

    private PointsRenderer pointsChanged() {
        updateShapeIndices();
        updateShapePosition();
        updateShapeTexture();
        cornersDirty = true;
        centersBoxDirty = true;
        return this;
    }

    // Diameter (or side) of the points, world units
    public PointsRenderer setPointSize(float pointSize) {
        this.pointSize = Math.max(pointSize, 0f);
        return this;
    }

    public float getPointSize() {
        return pointSize;
    }

    public PointsRenderer setPointShape(EPointShape pointShape) {
        if (pointShape == null)
            throw new IllegalArgumentException("The shape of the points can't be null");
        this.pointShape = pointShape;
        return this;
    }

    public EPointShape getPointShape() {
        return pointShape;
    }

    // Draws nothing without points; sends the corners of the quads when their number changed
    @Override
    public void display() {
        if (points.isEmpty())
            return;

        if (cornersDirty) {
            float[] corners = new float[points.size() * CORNERS.length];
            for (int i = 0; i < points.size(); ++i)
                System.arraycopy(CORNERS, 0, corners, i * CORNERS.length, CORNERS.length);
            getMesh().updateVbo(corners, cornerIndex);
            cornersDirty = false;
        }

        super.display();
    }

    @Override
    protected int[] getIndices() {
        int[] indices = new int[points.size() * 6];
        for (int i = 0; i < points.size(); ++i) {
            int first = i * 4;
            int index = i * 6;
            indices[index] = first;
            indices[index + 1] = first + 1;
            indices[index + 2] = first + 2;
            indices[index + 3] = first;
            indices[index + 4] = first + 2;
            indices[index + 5] = first + 3;
        }

        return indices;
    }

    // The center of its point for each vertex
    @Override
    protected float[] calculatePosition() {
        float[] positions = new float[points.size() * 8];
        for (int i = 0; i < points.size(); ++i) {
            Vector2f point = points.get(i);
            for (int corner = 0; corner < 4; ++corner) {
                positions[i * 8 + corner * 2] = point.x;
                positions[i * 8 + corner * 2 + 1] = point.y;
            }
        }

        return positions;
    }

    // Not read by points2DV.glsl, as long as the positions for a shader of a project
    @Override
    protected float[] calculateTexturePosition() {
        return new float[points.size() * 8];
    }

    // The quads of its points (shapePointVertex): the box of the centers, widened by a point and its margin
    @Override
    protected boolean viewCullBounds(ViewCullBounds out) {
        if (centersBoxDirty) {
            hasCentersBox = !points.isEmpty();
            if (hasCentersBox) {
                centersBox[0] = centersBox[1] = Float.POSITIVE_INFINITY;
                centersBox[2] = centersBox[3] = Float.NEGATIVE_INFINITY;
                for (Vector2f point : points) {
                    centersBox[0] = Math.min(centersBox[0], point.x);
                    centersBox[1] = Math.min(centersBox[1], point.y);
                    centersBox[2] = Math.max(centersBox[2], point.x);
                    centersBox[3] = Math.max(centersBox[3], point.y);
                }
                hasCentersBox = Float.isFinite(centersBox[0]) && Float.isFinite(centersBox[1])
                        && Float.isFinite(centersBox[2]) && Float.isFinite(centersBox[3]);
            }
            centersBoxDirty = false;
        }

        return hasCentersBox && ShapeCullBounds.Points(out, centersBox, pointSize, batchStroke(), isAntialiasing(),
                Math.max(extraMargin(), 0f));
    }

    @Override
    BatchFormat batchFormat() {
        return BatchFormats.POINT;
    }

    // One instance per point: its center, the values of the renderer
    @Override
    void writeBatchDraw(DrawBatch2D batch, int fill) {
        for (Vector2f point : points) {
            BatchGeometry2D.WriteModelRows(batch, modelMatrix());
            batch.put(point.x, point.y);
            writeInstanceValues(batch, fill);
        }
    }

    @Override
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        first.set(pointSize, pointShape == EPointShape.SQUARE ? 1f : 0f, 0f, 0f);
    }
}
