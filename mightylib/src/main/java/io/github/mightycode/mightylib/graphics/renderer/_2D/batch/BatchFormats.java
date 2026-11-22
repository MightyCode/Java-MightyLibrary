package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import java.util.LinkedHashMap;
import java.util.Map;

/** The batch formats of the batch programs of the engine (registered by the engine for its shaders). */
public final class BatchFormats {
    /**
     * Program sprite2D: per vertex, its position (transformed: world units of the camera of the draw), texture
     * coordinates and color (an sRGB RGBA, multiplied with the texture). What DrawBatch2D.vertex writes.
     */
    public static final BatchFormat SPRITE = BatchFormat.builder("sprite")
            .vertex("aPos", 2, EBatchAttributeType.FLOAT)
            .vertex("inTexCoords", 2, EBatchAttributeType.FLOAT)
            .vertex("inColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            .build();

    /**
     * Program sprite2DInstanced (mode INSTANCED, quads of sprites as instances): the corner of the unit quad per
     * vertex; per instance the rows of the affine transform of the quad, the texture coordinates at its corner
     * (0, 0) and along its axes, its color.
     */
    public static final BatchFormat SPRITE_INSTANCED = BatchFormat.builder("spriteInstanced")
            .vertex("aCorner", 2, EBatchAttributeType.FLOAT)
            .instance("aRow0", 3, EBatchAttributeType.FLOAT)
            .instance("aRow1", 3, EBatchAttributeType.FLOAT)
            .instance("aTexOrigin", 2, EBatchAttributeType.FLOAT)
            .instance("aTexAxes", 4, EBatchAttributeType.FLOAT)
            .instance("aColor", 4, EBatchAttributeType.NORMALIZED_UBYTE)
            // The corners in the order of the corners of DrawBatch2D.quad, and its two triangles
            .template(new float[]{0, 1, 0, 0, 1, 0, 1, 1}, new int[]{0, 3, 2, 0, 2, 1})
            .build();

    private BatchFormats() {}

    /** The batch programs of the engine (shaders.json of the engine) and their formats, registered by the engine. */
    public static Map<String, BatchFormat> engineFormats() {
        Map<String, BatchFormat> formats = new LinkedHashMap<>();
        formats.put("sprite2D", SPRITE);
        formats.put("sprite2DInstanced", SPRITE_INSTANCED);
        return formats;
    }
}
