package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

/** How the values of an attribute of a BatchFormat are stored and read by the batch program. */
public enum EBatchAttributeType {
    // 4 bytes per component, read as float (float to vec4 in GLSL); written by DrawBatch2D.put
    FLOAT,
    // 4 bytes per component, read as int (int to ivec4, or the unsigned ones); written by DrawBatch2D.putInt
    INT,
    /**
     * Up to 4 bytes in 4 (R, G, B, A in memory), each read as a float from 0 to 1 (float to vec4 in GLSL); written at
     * once as an int RGBA by DrawBatch2D.putColor (Color4f.toIntRGBA).
     */
    NORMALIZED_UBYTE;

    // Bytes of an attribute of this type with these components (the bytes are padded to 4)
    int bytes(int components) {
        return this == NORMALIZED_UBYTE ? 4 : 4 * components;
    }

    // Values written for an attribute: one per component, one int for the bytes
    int slots(int components) {
        return this == NORMALIZED_UBYTE ? 1 : components;
    }
}
