package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.RectangularFace;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormats;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.ViewCullBounds;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.BatchGeometry2D;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import io.github.mightycode.mightylib.graphics.shader.ShaderUniforms;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.Set;

/**
 * A 2D shape drawn by its signed distance on a quad (the rectangle of the shape, widened for the stroke and the
 * anti-aliasing): smooth edges at every zoom and rotation, a stroke inside, centered on or outside the edge, a fill
 * of a color or of a texture tinted by the color.
 * <p>
 * Fill: the color of setColorMode (white by default; alpha 0 for the stroke alone), or the main texture
 * (setMainTextureChannel) multiplied by the color; setColorMode(color) stops the texture, setColorMode(color, false)
 * keeps it. The size, the reference point and the rotation are those of Shape2DRenderer.
 * <p>
 * A project adds its own shape: a fragment shader that includes utils/shape2D.glsl and gives its distance to
 * shapeColor (vertex shader 2D/shape/shape2DV.glsl), declared in shaders.json, and a subclass that sends its
 * parameters (writeShapeParameters) and, when it goes beyond its rectangle, its margin (extraMargin). To be batched,
 * its entry links ("batch") to its own batch program: the same fragment shader with 2D/shape/shape2DInstancedV.glsl
 * (tags 2D and batch2D), registered with the format BatchFormats.SHAPE (registry.shaders().batchFormat); the kind
 * aShapeType of the format is ignored by it. The shapes of the engine share the batch program shape2DBatch, which
 * chooses each shape by its kind: a shape of a project linked to it is drawn in magenta. Each change of batch
 * program cuts the series of draws: a shape of a project between two shapes of the engine costs two more draw calls.
 */
public abstract class VectorShape2DRenderer extends Shape2DRenderer {
    private static final String SIZE = "shapeSize";
    private static final String REFERENCE = "shapeReference";
    private static final String STROKE_COLOR = "strokeColor";
    private static final String STROKE_WIDTH = "strokeWidth";
    private static final String STROKE_ALIGNMENT = "strokeAlignment";
    private static final String STROKE_UNIT = "strokeUnit";
    private static final String ANTIALIASING = "antialiasing";
    private static final String MARGIN = "shapeMargin";
    private static final String TEXTURE_POSITION = "shapeTexturePosition";
    private static final String TEXTURED = "shapeTextured";
    private static final String PARAMETERS_0 = "shapeParams0";
    private static final String PARAMETERS_1 = "shapeParams1";

    // The values written in the batch by submitToBatch: the renderer is batched with them
    private static final Set<String> BATCH_VALUES = Set.of(SIZE, REFERENCE, STROKE_COLOR, STROKE_WIDTH,
            STROKE_ALIGNMENT, STROKE_UNIT, ANTIALIASING, MARGIN, TEXTURE_POSITION, TEXTURED, PARAMETERS_0,
            PARAMETERS_1);

    private final ShapeStroke stroke;
    private boolean antialiasing;

    // Sent at each draw (the program skips the unchanged ones), filled in place
    private final Vector2f size = new Vector2f();
    private final Vector2f reference = new Vector2f();
    private final Vector4f texturePosition = new Vector4f();
    private final Vector4f parameters0 = new Vector4f();
    private final Vector4f parameters1 = new Vector4f();
    private final ShaderValue strokeWidthValue;
    private final ShaderValue strokeColorValue;
    private final ShaderValue strokeAlignmentValue;
    private final ShaderValue strokeUnitValue;
    private final ShaderValue antialiasingValue;
    private final ShaderValue marginValue;
    private final ShaderValue texturedValue;

    protected VectorShape2DRenderer(String shaderName) {
        super(shaderName, true);

        stroke = new ShapeStroke();
        antialiasing = true;

        addValue(SIZE, Vector2f.class, size);
        addValue(REFERENCE, Vector2f.class, reference);
        addValue(TEXTURE_POSITION, Vector4f.class, texturePosition);
        addValue(PARAMETERS_0, Vector4f.class, parameters0);
        addValue(PARAMETERS_1, Vector4f.class, parameters1);
        strokeWidthValue = addValue(STROKE_WIDTH, Float.class, 0f);
        strokeColorValue = addValue(STROKE_COLOR, Color4f.class, stroke.color());
        strokeAlignmentValue = addValue(STROKE_ALIGNMENT, Float.class, 0f);
        strokeUnitValue = addValue(STROKE_UNIT, Float.class, 0f);
        antialiasingValue = addValue(ANTIALIASING, Float.class, 1f);
        marginValue = addValue(MARGIN, Float.class, 0f);
        texturedValue = addValue(TEXTURED, Integer.class, 0);

        super.setColorMode(new Color4f(1f, 1f, 1f, 1f));
        setTexturePosition(RectangularFace.BasicTexturePosition());
    }

    // Optional: a shape uses only some of them (a circle has no parameter), the others are not reported missing
    private ShaderValue addValue(String name, Class<?> type, Object object) {
        ShaderValue value = new ShaderValue(name, type, object);
        putOptionalValue(name, value);
        return value;
    }

    @Override
    protected int[] getIndices() {
        return RectangularFace.IndicesForSquare();
    }

    @Override
    protected float[] calculatePosition() {
        return RendererUtils.calculatePositionForSquare(new Vector2f(1, 1), getReferencePoint());
    }

    // Not read by the vector shaders (they place the texture themselves), kept for a shader of a project
    @Override
    protected float[] calculateTexturePosition() {
        return new RectangularFace().textureCornerPosition(getTexturePosition());
    }

    /**
     * Hook: the values of the shape are set before each draw, then sent (sendShaderValues); a subclass that sends
     * more redefines it and calls super.
     */
    @Override
    public void updateShader() {
        computeValues();
        texturedValue.setObject(isTextured() ? 1 : 0);

        strokeWidthValue.setObject(stroke.width());
        strokeColorValue.setObject(stroke.color());
        strokeAlignmentValue.setObject(stroke.alignment().outsideFraction());
        strokeUnitValue.setObject(stroke.unit().shaderValue());
        antialiasingValue.setObject(antialiasing ? 1f : 0f);
        marginValue.setObject(Math.max(extraMargin(), 0f));

        super.updateShader();
    }

    // The values of the shape for its draw, alone or batched
    private void computeValues() {
        size.set(scale().x, scale().y);
        reference.set(getReferencePoint());
        texturePosition.set(getTexturePosition());
        writeShapeParameters(parameters0, parameters1);
    }

    private boolean isTextured() {
        return getMainTexture() != null && isTextureEnabled();
    }

    // ---- 2D batch ----

    @Override
    protected final Set<String> batchWrittenValues() {
        return BATCH_VALUES;
    }

    /**
     * Batched when its shader links to a batch program of the format of the shape (BatchFormats.SHAPE, POINT for
     * PointsRenderer, STROKE for the geometric shapes), its depth test is off and its model stays in the plane (BatchGeometry2D.IsAffine2D): one
     * instance with the values updateShader sends (a value a subclass sends itself in updateShader is not in it).
     */
    @Override
    protected final boolean submitToBatch(DrawBatch2D batch) {
        if (isDepthTest() || !BatchGeometry2D.IsAffine2D(modelMatrix()))
            return false;

        int state = batchState(batch);
        if (state < 0 || batch.formatOf(state) != batchFormat())
            return false;
        if (viewCulled(batch, state))
            return true;

        computeValues();
        int fill = batchColor();
        batch.beginDraw(state, getLayer(), getSortDepth());
        writeBatchDraw(batch, fill);
        batch.endDraw();
        return true;
    }

    /**
     * Hook (see Renderer): the quad its vertex shader places, the rectangle of its size around its reference point
     * widened by the outer part of the stroke, a pixel of anti-aliasing and extraMargin. A subclass whose shader
     * goes further than extraMargin widens it (call super, then out.margin).
     */
    @Override
    protected boolean viewCullBounds(ViewCullBounds out) {
        return ShapeCullBounds.VectorShape(out, scale().x, scale().y, getReferencePoint().x, getReferencePoint().y,
                stroke, antialiasing, extraMargin());
    }

    // The format of the batch program of the shape
    BatchFormat batchFormat() {
        return BatchFormats.SHAPE;
    }

    // The values of the draw (between beginDraw and endDraw): one instance for the shape
    void writeBatchDraw(DrawBatch2D batch, int fill) {
        BatchGeometry2D.WriteModelRows(batch, modelMatrix());
        batch.put(size.x, size.y, reference.x, reference.y);
        writeInstanceValues(batch, fill);
        batch.putInt(shapeType());
    }

    // The kind of the shape in the common batch program shape2DBatch: NONE for a shape of a project (its own program)
    int shapeType() {
        return ShapeTypes.NONE;
    }

    // The size of the shape, the values of the stroke: read by the subclasses for their draw
    final Vector2f batchSize() {
        return size;
    }

    final Vector4f batchTexturePosition() {
        return texturePosition;
    }

    final boolean batchTextured() {
        return isTextured();
    }

    final ShapeStroke batchStroke() {
        return stroke;
    }

    // What follows the place of the instance in BatchFormats.SHAPE and POINT
    final void writeInstanceValues(DrawBatch2D batch, int fill) {
        batch.putInt(fill);
        batch.putInt(stroke.color().toIntRGBA());
        batch.put(stroke.width(), stroke.alignment().outsideFraction(), stroke.unit().shaderValue(),
                antialiasing ? 1f : 0f);
        batch.put(Math.max(extraMargin(), 0f), isTextured() ? 1f : 0f);
        batch.put(texturePosition.x, texturePosition.y, texturePosition.z, texturePosition.w);
        batch.put(parameters0.x, parameters0.y, parameters0.z, parameters0.w);
        batch.put(parameters1.x, parameters1.y, parameters1.z, parameters1.w);
    }

    /**
     * Hook called before each draw: writes the parameters of the shape (read by its fragment shader as shapeData0
     * and shapeData1) into the two vectors, in place. Their previous values are kept when not written.
     */
    protected void writeShapeParameters(Vector4f first, Vector4f second) {
        // No parameter by default
    }

    /**
     * Hook called before each draw: how far (world units) the shape goes beyond the rectangle of its size, so that
     * its quad covers it. 0 by default.
     */
    protected float extraMargin() {
        return 0f;
    }

    // Hook (call super): no fill (transparent color), the stroke is kept
    @Override
    public void setNoColorNoTextureMode() {
        super.setNoColorNoTextureMode();
        super.setColorMode(new Color4f(0f, 0f, 0f, 0f));
    }

    // ---- Stroke ----

    // Width of the stroke, 0 (the default) for none; in the unit of setStrokeUnit
    public final VectorShape2DRenderer setStrokeWidth(float width) {
        stroke.setWidth(width);
        return this;
    }

    public final float getStrokeWidth() {
        return stroke.width();
    }

    // Black by default; copied: changing the color given afterwards changes nothing
    public final VectorShape2DRenderer setStrokeColor(Color4f color) {
        stroke.setColor(color);
        return this;
    }

    // A copy
    public final Color4f getStrokeColor() {
        return stroke.color().copy();
    }

    // CENTERED by default
    public final VectorShape2DRenderer setStrokeAlignment(EStrokeAlignment alignment) {
        stroke.setAlignment(alignment);
        return this;
    }

    public final EStrokeAlignment getStrokeAlignment() {
        return stroke.alignment();
    }

    // WORLD by default (the width follows the zoom), SCREEN_PIXELS for the same width at every zoom
    public final VectorShape2DRenderer setStrokeUnit(EStrokeUnit unit) {
        stroke.setUnit(unit);
        return this;
    }

    public final EStrokeUnit getStrokeUnit() {
        return stroke.unit();
    }

    // Smooth edges over one pixel (the default), or sharp ones (pixel art)
    public final VectorShape2DRenderer setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
        return this;
    }

    public final boolean isAntialiasing() {
        return antialiasing;
    }
}
