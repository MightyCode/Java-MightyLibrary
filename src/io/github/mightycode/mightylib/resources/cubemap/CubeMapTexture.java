package io.github.mightycode.mightylib.resources.cubemap;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * GL_TEXTURE_CUBE_MAP of a CubeMapData, one face sent per loading step. While it is not loaded, the element of the
 * fallback of its data type is bound (checkerboard), or a transparent cubemap until that one is sent.
 * When its data is reloaded, the new faces are sent in another texture which replaces it once complete.
 */
public final class CubeMapTexture extends GLElement {
    private final CubeMapData data;
    private int textureId = -1;
    private int sentFaces;
    private int uploadedVersion;
    // Hot reload: the new version, sent face by face while textureId stays in use
    private int pendingId = -1;
    private int pendingFaces;
    private int pendingVersion;

    public CubeMapTexture(CubeMapData data) {
        this.data = data;
        addResourceDependency(data);
    }

    // Bound to the texture unit, for a samplerCube
    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_CUBE_MAP, currentId());
        EngineStats.countTextureBind();
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Bind cubemap " + data.getDataName());
    }

    private int currentId() {
        if (isLoaded()) {
            if (data.isLoaded() && data.getVersion() != uploadedVersion)
                requestReload();
            return textureId;
        }

        GLElement fallback = glResources().fallback(data.getClass());
        if (fallback instanceof CubeMapTexture && fallback != this && fallback.isLoaded())
            return ((CubeMapTexture) fallback).textureId;

        return glResources().shared(CubeMapPlaceholders.class, CubeMapPlaceholders::new).placeholderId();
    }

    @Override
    public boolean load(int remainingMilliseconds) {
        if (textureId == -1 || data.getVersion() != uploadedVersion) {
            deleteTexture();
            textureId = create();
            sentFaces = 0;
            uploadedVersion = data.getVersion();
        }

        glBindTexture(GL_TEXTURE_CUBE_MAP, textureId);
        sendFace(sentFaces++);

        return sentFaces == CubeMapData.FACE_COUNT;
    }

    @Override
    protected boolean reload(int remainingMilliseconds) {
        // Being read again in place: the current version stays until the data is loaded
        if (!data.isLoaded())
            return true;

        int version = data.getVersion();
        if (pendingId == -1 && version == uploadedVersion)
            return true;

        if (pendingId != -1 && pendingVersion != version)
            deletePending();

        if (pendingId == -1) {
            pendingId = create();
            pendingFaces = 0;
            pendingVersion = version;
        }

        glBindTexture(GL_TEXTURE_CUBE_MAP, pendingId);
        sendFace(pendingFaces++);
        if (pendingFaces < CubeMapData.FACE_COUNT)
            return false;

        GenDeleteResources.DeleteTexture(textureId);
        textureId = pendingId;
        pendingId = -1;
        uploadedVersion = pendingVersion;
        return true;
    }

    private static int create() {
        int id = GenDeleteResources.GenTexture();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        Logger.CheckOpenGLError("Create cubemap");
        return id;
    }

    // In the bound cubemap
    private void sendFace(int face) {
        int size = data.getFaceSize();
        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, GL_RGBA8, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                data.getFace(face));
        Logger.CheckOpenGLError("Send face " + CubeMapData.faceNames().get(face) + " of cubemap " + data.getDataName());
    }

    private void deleteTexture() {
        if (textureId != -1) {
            GenDeleteResources.DeleteTexture(textureId);
            textureId = -1;
        }
    }

    private void deletePending() {
        if (pendingId != -1) {
            GenDeleteResources.DeleteTexture(pendingId);
            pendingId = -1;
        }
    }

    @Override
    public void unload(int remainingMilliseconds) {
        deleteTexture();
        deletePending();
        sentFaces = 0;
    }
}
