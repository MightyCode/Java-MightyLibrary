package io.github.mightycode.mightylib.graphics.renderer._2D.batch;

import java.util.Locale;

/**
 * How the 2D draws are sent (EngineConfig.setBatch2DMode, -Dmightylib.batch2D, command batch mode), changeable while
 * the game runs.
 */
public enum EBatch2DMode {
    // Each renderer draws alone, as without batch: the reference of the picture
    OFF,
    // Batched; the buffers are orphaned then filled at each flush (glBufferData null, glBufferSubData)
    ORPHAN,
    // Batched; a ring buffer written without waiting (glMapBufferRange unsynchronized), a fence per flush
    RING,
    // Batched; quads of sprites drawn by instances, the other batches like ORPHAN
    INSTANCED;

    public boolean isBatched() {
        return this != OFF;
    }

    // Lower case, as written in the option and the command
    public String optionName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The mode of this name (case ignored), null when unknown. */
    public static EBatch2DMode parse(String name) {
        if (name == null)
            return null;

        for (EBatch2DMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name.trim()))
                return mode;
        }

        return null;
    }
}
