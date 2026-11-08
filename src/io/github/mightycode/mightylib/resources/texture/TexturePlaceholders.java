package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

/**
 * GL textures bound in place of a texture that is not loaded yet (draws nothing instead of failing) or that failed.
 * Owned by GLResources, created at the first use, deleted with it. Main thread only.
 */
public final class TexturePlaceholders {
    private int placeholderId = -1;
    private int fallbackRectangleId = -1;

    // 1x1 transparent 2D texture
    int placeholderId() {
        if (placeholderId == -1) {
            placeholderId = GenDeleteResources.GenTexture();
            glBindTexture(GL_TEXTURE_2D, placeholderId);
            ByteBuffer transparent = BufferUtils.createByteBuffer(4);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, transparent);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            Logger.CheckOpenGLError("Create placeholder texture");
        }

        return placeholderId;
    }

    // The fallback checkerboard as a rectangle texture, for the samplers of rectangle textures (fonts); -1 until ready
    int fallbackRectangleId(Resources resources) {
        if (fallbackRectangleId == -1) {
            TextureData fallback = resources.getFallback(TextureData.class);
            if (fallback == null || !fallback.isLoaded())
                return -1;

            fallbackRectangleId = GenDeleteResources.GenTexture();
            glBindTexture(GL_TEXTURE_RECTANGLE, fallbackRectangleId);
            glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA8, fallback.getWidth(), fallback.getHeight(), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, fallback.getData());
            glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            Logger.CheckOpenGLError("Create rectangle fallback texture");
        }

        return fallbackRectangleId;
    }

    public void delete() {
        if (placeholderId != -1) {
            GenDeleteResources.DeleteTexture(placeholderId);
            placeholderId = -1;
        }

        if (fallbackRectangleId != -1) {
            GenDeleteResources.DeleteTexture(fallbackRectangleId);
            fallbackRectangleId = -1;
        }
    }
}
