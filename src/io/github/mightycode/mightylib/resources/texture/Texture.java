package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public class Texture extends GLElement implements IGLBindable, IRenderTextureBindable {
    // 2D textures bigger than this are sent in several steps, by strips of rows, so a frame never waits for a whole texture
    private static final int BIG_TEXTURE_BYTES = 4 * 1024 * 1024;
    // Size of a strip: what the measured speed allows in the time left, within these bounds.
    // The minimum is sent even when no time is left, the maximum keeps a forced step short.
    private static final int MIN_STRIP_BYTES = 256 * 1024;
    private static final int MAX_STRIP_BYTES = 4 * 1024 * 1024;

    // Measured upload speed, shared by all textures, starts with a cautious guess
    private static double bytesPerMillisecond = 1024 * 1024;

    private final TextureData data;
    private int textureId;
    // Rows already sent, for a big texture sent by strips
    private int uploadedRows;
    // Version of the data in textureId
    private int uploadedVersion;
    // Size of the data in textureId: the data may already have the size of its next version
    private int uploadedWidth, uploadedHeight;
    // Hot reload: the new version is sent in this texture, which replaces textureId once complete
    private int pendingId = -1;
    private int pendingRows;
    private int pendingVersion;

    private int qualityType;

    private int textureType;

    // Belongs to the engine of the view that creates it (the explicit Resources constructors choose another)
    private final Resources resources;

    public Texture(String name) {
        this(Views.engine().resources(), name);
    }

    // A missing texture would wait forever to be loaded (and block a loading screen): the fallback replaces it
    public Texture(Resources resources, String name) {
        this(resources, resources.getResourceOrFallback(TextureData.class, name));
    }

    public Texture(TextureData data) {
        this(Views.engine().resources(), data);
    }

    public Texture(Resources resources, TextureData data) {
        this.resources = resources;
        this.data = data;
        if (data != null)
            resourceDependencies.add(data);

        textureId = -1;
    }

    public void setAspectTexture(int aspectTexture){
        this.qualityType = aspectTexture;
    }

    public void bindRenderTexture() {
        bindRenderTexture(0);
    }

    @Override
    public void bindRenderTexture(int texturePos) {
        // Active the texture to right position
        glActiveTexture(GL_TEXTURE0 + texturePos);
        if (isLoaded()){
            checkVersion();
            glBindTexture(textureType, textureId);
            EngineStats.countTextureBind();

            // Called for each draw: avoid building the message when checks are disabled
            if (Logger.DO_CHECK)
                Logger.CheckOpenGLError("Bind texture : " + data.getDataName() + " , texture id : " + textureId + " , texture type : " + textureType);
        // Failed or still loading: the fallback texture instead
        } else {
            // Same target as this texture: a rectangle sampler can't read a 2D texture
            int rectangleId = data != null && data.defaultTextureType == GL_TEXTURE_RECTANGLE ? glResources().placeholders().fallbackRectangleId(resources) : -1;
            if (rectangleId != -1)
                glBindTexture(GL_TEXTURE_RECTANGLE, rectangleId);
            else
                glBindTexture(GL_TEXTURE_2D, getFallbackId());
            EngineStats.countTextureBind();
        }
    }

    // Its data was reloaded: sent again by GLResources, the current version is used meanwhile
    private void checkVersion() {
        if (data.isLoaded() && data.getVersion() != uploadedVersion)
            requestReload();
    }

    @Override
    protected boolean reload(int remainingMilliseconds) {
        // Being read again in place (no reload copy): the current version stays until the data is loaded
        if (!data.isLoaded())
            return true;

        int version = data.getVersion();
        if (pendingId == -1 && version == uploadedVersion)
            return true;

        // Changed again while being sent: start again with the latest version
        if (pendingId != -1 && pendingVersion != version)
            deletePending();

        if (pendingId == -1) {
            pendingId = GenDeleteResources.GenTexture();
            pendingVersion = version;
            glBindTexture(textureType, pendingId);

            if (isSentByStrips()) {
                glTexImage2D(textureType, 0, GL_RGBA8, data.getWidth(), data.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                pendingRows = 0;
            } else {
                glTexImage2D(textureType, 0, GL_RGBA8, data.getWidth(), data.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data.getData());
                pendingRows = data.getHeight();
            }
            Logger.CheckOpenGLError("Send texture again : " + data.getDataName());
        } else {
            glBindTexture(textureType, pendingId);
        }

        if (pendingRows < data.getHeight())
            pendingRows = sendStrips(pendingRows, remainingMilliseconds);

        if (pendingRows < data.getHeight())
            return false;

        GenDeleteResources.DeleteTexture(textureId);
        textureId = pendingId;
        pendingId = -1;
        uploadedVersion = pendingVersion;
        uploadedWidth = data.getWidth();
        uploadedHeight = data.getHeight();
        finishLoad();

        return true;
    }

    private void deletePending() {
        if (pendingId != -1) {
            GenDeleteResources.DeleteTexture(pendingId);
            pendingId = -1;
        }
    }

    // Texture of the fallback data (CreateErrorTexture), transparent until it is sent itself
    private int getFallbackId() {
        Texture fallback = glResources().getResource(Texture.class, Resources.FALLBACK_NAME);
        if (fallback != null && fallback != this && fallback.isLoaded())
            return fallback.textureId;

        return glResources().placeholders().placeholderId();
    }

    // Data giving the size: the fallback one while this one is not loaded
    private TextureData sizeData() {
        return resources.usable(TextureData.class, data);
    }

    private boolean isSentByStrips() {
        return textureType == GL_TEXTURE_2D && (long) data.getWidth() * data.getHeight() * 4 > BIG_TEXTURE_BYTES;
    }

    /**
     * Small textures are sent at once. Big ones are sent by strips of rows while time is left in the frame
     * (at least one strip): false is returned until the last strip, the loading continues at the next frame.
     */
    @Override
    public boolean load(int remainingMilliseconds) {
        if (data == null) {
            return false;
        }

        // Called again on a loaded texture: send it again
        if (isLoaded())
            unload(remainingMilliseconds);

        // Reloaded while being sent by strips: start again with the latest version
        if (textureId != -1 && data.getVersion() != uploadedVersion)
            unload(remainingMilliseconds);

        try {
            if (textureId == -1) {
                qualityType = data.defaultAspectTexture;
                textureType = data.defaultTextureType;

                textureId = GenDeleteResources.GenTexture();
                uploadedRows = 0;
                uploadedVersion = data.getVersion();
                uploadedWidth = data.getWidth();
                uploadedHeight = data.getHeight();

                glBindTexture(this.textureType, textureId);
                Logger.CheckOpenGLError("Bind texture : " + data.getDataName() + " , texture id : " + textureId + " , texture type : " + textureType + " , quality type : " + qualityType + " , path : " + data.path() + " width : " + data.getWidth() + " , height : " + data.getHeight());

                if (!isSentByStrips()) {
                    glTexImage2D(this.textureType, 0, GL_RGBA8, data.getWidth(), data.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data.getData());
                    Logger.CheckOpenGLError("Set texture data : " + data.getDataName());

                    return finishLoad();
                }

                // Memory reserved now, filled by the strips
                glTexImage2D(this.textureType, 0, GL_RGBA8, data.getWidth(), data.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                Logger.CheckOpenGLError("Reserve texture : " + data.getDataName());
            } else {
                glBindTexture(this.textureType, textureId);
            }

            uploadedRows = sendStrips(uploadedRows, remainingMilliseconds);

            if (uploadedRows < data.getHeight())
                return false;

            return finishLoad();
        } catch (Exception e) {
            Logger.LogException("Can't create the texture " + data.path(), e);
            GenDeleteResources.DeleteTexture(textureId);
            textureId = -1;

            return false;
        }
    }

    // Send rows of the data from this one to the bound texture while time is left (at least one strip): the next row
    private int sendStrips(int fromRow, int remainingMilliseconds) {
        int width = data.getWidth();
        int height = data.getHeight();
        int rowBytes = width * 4;
        int sentRows = fromRow;
        long start = System.nanoTime();

        do {
            double leftMilliseconds = remainingMilliseconds - (System.nanoTime() - start) / 1e6;
            long stripBytes = (long) Math.max(MIN_STRIP_BYTES, Math.min(MAX_STRIP_BYTES, bytesPerMillisecond * leftMilliseconds));
            int rows = (int) Math.max(1, Math.min(height - sentRows, stripBytes / rowBytes));

            ByteBuffer strip = data.getData().duplicate();
            strip.position(sentRows * rowBytes);
            strip.limit((sentRows + rows) * rowBytes);

            long stripStart = System.nanoTime();
            TextureUploader.uploadRows(textureType, sentRows, width, rows, strip);
            Logger.CheckOpenGLError("Send texture strip : " + data.getDataName());
            sentRows += rows;

            // Smoothed, a single slow strip must not shrink the next ones too much
            double stripMilliseconds = Math.max(0.01, (System.nanoTime() - stripStart) / 1e6);
            bytesPerMillisecond = bytesPerMillisecond * 0.7 + ((double) rows * rowBytes / stripMilliseconds) * 0.3;
        } while (sentRows < height && (System.nanoTime() - start) / 1e6 < remainingMilliseconds);

        return sentRows;
    }

    private boolean finishLoad() {
        TextureParameters.applyParameters(this);
        Logger.CheckOpenGLError("Set texture parameters : " + data.getDataName());

        return true;
    }

    private void setTextParam(int param, int value) {
        bindRenderTexture();
        if (data.isLoaded())
            glTexParameteri(this.textureType, param, value);
    }

    public int getQualityType(){
        return qualityType;
    }

    public void setTextureType(int textureType){
        this.textureType = textureType;
    }

    public int getTextureType(){
        return this.textureType;
    }

    @Override
    public int getRenderTextureId() {
        if (!isLoaded())
            return getFallbackId();

        checkVersion();
        return textureId;
    }

    @Override
    public int getWidth() {
        return isLoaded() ? uploadedWidth : sizeData().getWidth();
    }

    /**
     * Shown with the latest version of its data (false while the new version is sent, or not loaded yet).
     * What is computed from the texture (texture coordinates, sizes) waits for it, not for the data.
     */
    public boolean isUpToDate() {
        return isLoaded() && data.getVersion() == uploadedVersion;
    }

    @Override
    public int getHeight() {
        return isLoaded() ? uploadedHeight : sizeData().getHeight();
    }

    @Override
    public void unload(int remainingMilliseconds) {
        // The GL texture exists independently of its CPU data, which can already be unloaded
        if (textureId != -1) {
            GenDeleteResources.DeleteTexture(textureId);
            textureId = -1;
        }

        deletePending();
        uploadedRows = 0;
    }
}
