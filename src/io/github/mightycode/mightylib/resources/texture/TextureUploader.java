package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.Logger;
import org.json.JSONObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.GL_MAP_INVALIDATE_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30.glMapBufferRange;

/**
 * Sends rows of pixels to the bound texture, directly or through pixel buffer objects (PBO).
 * Main thread only. The PBOs are created when needed and freed by releaseBuffers once nothing is left to send.
 */
public final class TextureUploader {
    enum Variant {
        // glTexSubImage2D reads the memory of the program
        DIRECT,
        // glBufferData copies the pixels in the PBO, glTexSubImage2D reads the PBO
        PBO_DATA,
        // The PBO is mapped and filled by a copy, then glTexSubImage2D reads it
        PBO_MAPPED
    }

    // Variant used by ETextureUpload.PBO
    private static final Variant PBO_VARIANT = Variant.PBO_DATA;

    // Two buffers in turn: the driver may still read one while the next strip is written in the other
    private static final int PBO_COUNT = 2;

    private static ETextureUpload mode = ETextureUpload.DIRECT;

    private static final int[] pbos = new int[PBO_COUNT];
    private static int pboCreated = 0;
    private static int nextPbo = 0;

    // Strips sent since the mode was set, bytes, and time spent in the calls (not including the transfer still running)
    private static long strips, bytes, nanos;

    private TextureUploader() {}

    public static void setMode(ETextureUpload newMode) {
        mode = newMode == null ? ETextureUpload.DIRECT : newMode;
        strips = bytes = nanos = 0;
    }

    public static ETextureUpload getMode() {
        return mode;
    }

    public static JSONObject describe() {
        JSONObject json = new JSONObject();
        json.put("mode", mode.name());
        json.put("strips", strips);
        json.put("megabytes", Math.round(bytes / 1048576.0 * 10) / 10.0);
        json.put("millisecondsInCalls", Math.round(nanos / 1e6 * 10) / 10.0);
        json.put("pixelBuffers", pboCreated);
        return json;
    }

    // Rows of the bound texture, from row y, width pixels wide, the pixels are the remaining bytes of the buffer
    static void uploadRows(int target, int y, int width, int rows, ByteBuffer pixels) {
        long start = System.nanoTime();
        int size = pixels.remaining();

        upload(mode == ETextureUpload.PBO ? PBO_VARIANT : Variant.DIRECT, target, y, width, rows, pixels);

        nanos += System.nanoTime() - start;
        bytes += size;
        ++strips;
    }

    static void upload(Variant variant, int target, int y, int width, int rows, ByteBuffer pixels) {
        if (variant == Variant.DIRECT) {
            glTexSubImage2D(target, 0, 0, y, width, rows, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            Logger.CheckOpenGLError("Send texture rows");
            return;
        }

        createBuffers();
        int pbo = pbos[nextPbo];
        nextPbo = (nextPbo + 1) % PBO_COUNT;
        int size = pixels.remaining();

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
        try {
            // Replaces the storage: the strip before is not waited for if the driver still reads it
            if (variant == Variant.PBO_MAPPED) {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, size, GL_STREAM_DRAW);
                ByteBuffer mapped = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, size,
                        GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
                if (mapped == null) {
                    glBufferData(GL_PIXEL_UNPACK_BUFFER, pixels, GL_STREAM_DRAW);
                } else {
                    MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), MemoryUtil.memAddress(mapped), size);
                    glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
                }
            } else {
                glBufferData(GL_PIXEL_UNPACK_BUFFER, pixels, GL_STREAM_DRAW);
            }

            // With a buffer bound, the last argument is an offset in it
            glTexSubImage2D(target, 0, 0, y, width, rows, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
            Logger.CheckOpenGLError("Send texture rows through a PBO");
        } finally {
            // A bound buffer would change the meaning of every later texture call
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        }
    }

    private static void createBuffers() {
        while (pboCreated < PBO_COUNT)
            pbos[pboCreated++] = GenDeleteResources.GenPixelBuffer();
    }

    // Nothing left to send: the buffers are not kept
    public static void releaseBuffers() {
        while (pboCreated > 0)
            GenDeleteResources.DeletePixelBuffer(pbos[--pboCreated]);

        nextPbo = 0;
    }
}
