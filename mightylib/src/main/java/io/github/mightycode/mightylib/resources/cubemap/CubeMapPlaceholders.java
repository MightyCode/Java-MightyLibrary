package io.github.mightycode.mightylib.resources.cubemap;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.IGLSharedState;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.resources.texture.ETextureColorSpace;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;

/**
 * State shared by the cubemaps (GLResources.shared): a 1x1 transparent cubemap bound while neither a cubemap nor
 * the fallback one is loaded, created at the first use, deleted with GLResources. Main thread only.
 */
public final class CubeMapPlaceholders implements IGLSharedState {
    private int placeholderId = -1;

    int placeholderId() {
        if (placeholderId == -1) {
            placeholderId = GenDeleteResources.GenTexture();
            GLContext.currentState().bindTexture(GL_TEXTURE_CUBE_MAP, placeholderId);
            ByteBuffer transparent = BufferUtils.createByteBuffer(4);
            for (int face = 0; face < CubeMapData.FACE_COUNT; ++face)
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, Texture.InternalFormat(ETextureColorSpace.SRGB), 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, transparent);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            Logger.CheckOpenGLError("Create placeholder cubemap");
        }

        return placeholderId;
    }

    @Override
    public void delete() {
        if (placeholderId != -1) {
            GenDeleteResources.DeleteTexture(placeholderId);
            placeholderId = -1;
        }
    }
}
