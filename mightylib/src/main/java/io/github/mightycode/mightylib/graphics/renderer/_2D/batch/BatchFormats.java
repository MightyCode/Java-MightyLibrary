package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import io.github.mightycode.mightylib.resources.EngineResources;

import java.util.LinkedHashMap;
import java.util.Map;

/** The batch formats of the batch programs of the engine (registered by the engine for its shaders). */
public final class BatchFormats {
    /**
     * Program sprite2D: per vertex, its position (transformed: world units of the camera of the draw), texture
     * coordinates, the region of the texture it is limited to (left, right, top, bottom; no limit when right <= left,
     * utils/textureRegion.glsl) and color (an sRGB RGBA, multiplied with the texture). What DrawBatch2D.vertex writes.
     */
    public static final BatchFormat SPRITE = BatchFormat.builder("sprite")
            .vertex("aPos", 2, EBatchAttributeType.FLOAT)
            .vertex("inTexCoords", 2, EBatchAttributeType.FLOAT)
            .vertex("inTexRegion", 4, EBatchAttributeType.FLOAT)
            .vertex("inColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .build();

    /**
     * Program sprite2DInstanced (mode INSTANCED, quads of sprites as instances): the corner of the unit quad per
     * vertex; per instance the rows of the affine transform of the quad, the texture coordinates at its corner
     * (0, 0) and along its axes, the region of the texture it is limited to (as in SPRITE), its color.
     */
    public static final BatchFormat SPRITE_INSTANCED = BatchFormat.builder("spriteInstanced")
            .vertex("aCorner", 2, EBatchAttributeType.FLOAT)
            .instance("aRow0", 3, EBatchAttributeType.FLOAT)
            .instance("aRow1", 3, EBatchAttributeType.FLOAT)
            .instance("aTexOrigin", 2, EBatchAttributeType.FLOAT)
            .instance("aTexAxes", 4, EBatchAttributeType.FLOAT)
            .instance("aTexRegion", 4, EBatchAttributeType.FLOAT)
            .instance("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            // The corners in the order of the corners of DrawBatch2D.quad, and its two triangles
            .template(new float[]{0, 1, 0, 0, 1, 0, 1, 1}, new int[]{0, 3, 2, 0, 2, 1})
            .build();

    /**
     * Programs of the vector shapes (2D/shape/shape2DInstancedV.glsl with 2D/shape/shape2DBatchF.glsl for the shapes
     * of the engine, the program shape2DBatch, or the fragment shader of a shape of a project), one instance per
     * shape: the corner of the unit square per vertex; per instance the rows of its 2D affine model, its size and
     * reference point, fill and stroke colors (packed sRGB ints, Color4f.toIntRGBA), stroke width, alignment, unit
     * and antialiasing, margin and textured, texture position, the parameters of the shape (the uniforms of
     * shape2DV.glsl, in VectorShape2DRenderer), then its kind in shape2DBatch (utils/shapeTypes2D.glsl; 0 for a shape
     * of a project, ignored by its program).
     */
    public static final BatchFormat SHAPE = shape("shape", "aSizeReference", 4)
            .instance("aShapeType", 1, EBatchAttributeType.INT)
            .template(new float[]{0, 1, 0, 0, 1, 0, 1, 1}, new int[]{0, 3, 2, 0, 2, 1})
            .build();

    /**
     * Program of PointsRenderer (2D/shape/points2DInstancedV.glsl), one instance per point: SHAPE with the center of
     * the point (in the frame of the renderer) instead of the size and reference point; corners -1 to 1.
     */
    public static final BatchFormat POINT = shape("point", "aCenter", 2)
            .template(new float[]{-1, -1, -1, 1, 1, 1, 1, -1}, new int[]{0, 1, 2, 0, 2, 3})
            .build();

    /**
     * Program of the geometric shapes (2D/shape/stroke2DBatchV.glsl), per vertex: the attributes of stroke2DV.glsl
     * (base, extrusion, coverage, paint), then the values of its shape (rows of the 2D affine model, size, fill and
     * stroke colors, stroke width, unit, antialiasing and textured, texture position, bounding box): shapes of other
     * widths and colors share a draw call.
     */
    public static final BatchFormat STROKE = BatchFormat.builder("stroke")
            .vertex("aBase", 2, EBatchAttributeType.FLOAT)
            .vertex("aExtrusion", 4, EBatchAttributeType.FLOAT)
            .vertex("aCoverage", 4, EBatchAttributeType.FLOAT)
            .vertex("aPaint", 2, EBatchAttributeType.FLOAT)
            .vertex("aModel0", 3, EBatchAttributeType.FLOAT)
            .vertex("aModel1", 3, EBatchAttributeType.FLOAT)
            .vertex("aSize", 2, EBatchAttributeType.FLOAT)
            .vertex("aColor", 1, EBatchAttributeType.INT)
            .vertex("aStrokeColor", 1, EBatchAttributeType.INT)
            .vertex("aStroke", 4, EBatchAttributeType.FLOAT)
            .vertex("aTexturePosition", 4, EBatchAttributeType.FLOAT)
            .vertex("aBounds", 4, EBatchAttributeType.FLOAT)
            .build();

    private static BatchFormat.Builder shape(String name, String placeName, int placeComponents) {
        return BatchFormat.builder(name)
                .vertex("aCorner", 2, EBatchAttributeType.FLOAT)
                .instance("aModel0", 3, EBatchAttributeType.FLOAT)
                .instance("aModel1", 3, EBatchAttributeType.FLOAT)
                .instance(placeName, placeComponents, EBatchAttributeType.FLOAT)
                .instance("aColor", 1, EBatchAttributeType.INT)
                .instance("aStrokeColor", 1, EBatchAttributeType.INT)
                .instance("aStroke", 4, EBatchAttributeType.FLOAT)
                .instance("aMarginTextured", 2, EBatchAttributeType.FLOAT)
                .instance("aTexturePosition", 4, EBatchAttributeType.FLOAT)
                .instance("aParams0", 4, EBatchAttributeType.FLOAT)
                .instance("aParams1", 4, EBatchAttributeType.FLOAT);
    }

    private BatchFormats() {}

    /** The batch programs of the engine (shaders.json of the engine) and their formats, registered by the engine. */
    public static Map<String, BatchFormat> engineFormats() {
        Map<String, BatchFormat> formats = new LinkedHashMap<>();
        formats.put(EngineResources.SPRITE_2D, SPRITE);
        formats.put(EngineResources.SPRITE_2D_INSTANCED, SPRITE_INSTANCED);
        formats.put(EngineResources.SHAPE_2D_BATCH, SHAPE);
        formats.put(EngineResources.POINTS_2D_BATCH, POINT);
        formats.put(EngineResources.STROKE_2D_BATCH, STROKE);
        return formats;
    }
}
