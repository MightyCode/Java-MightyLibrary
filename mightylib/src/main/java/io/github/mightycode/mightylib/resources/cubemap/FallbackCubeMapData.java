package io.github.mightycode.mightylib.resources.cubemap;

import io.github.mightycode.mightylib.resources.Resources;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * Fallback of the cubemaps: the magenta and black checkerboard of the textures on each face, created in memory,
 * it can't fail. Its own element (a CubeMapTexture) is sent in the frames following the first loading.
 */
public final class FallbackCubeMapData extends CubeMapData {
    private static final int SIZE = 16;
    private static final int CELL = 8;

    public FallbackCubeMapData() {
        super(Resources.FALLBACK_NAME, Collections.emptyList());
    }

    @Override
    protected synchronized boolean internLoad() {
        ByteBuffer[] faces = new ByteBuffer[FACE_COUNT];
        for (int face = 0; face < FACE_COUNT; ++face) {
            ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
            for (int y = 0; y < SIZE; ++y) {
                for (int x = 0; x < SIZE; ++x) {
                    boolean magenta = (x / CELL + y / CELL) % 2 == 0;
                    pixels.put((byte) (magenta ? 255 : 0)).put((byte) 0).put((byte) (magenta ? 255 : 0)).put((byte) 255);
                }
            }
            pixels.flip();
            faces[face] = pixels;
        }

        setGeneratedFaces(faces, SIZE);
        return true;
    }
}
