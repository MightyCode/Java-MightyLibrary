package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
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

    // Bound while a texture is not loaded yet: draws nothing instead of failing
    private static int placeholderId = -1;
    private static int fallbackRectangleId = -1;

    private final TextureData data;
    private int textureId;
    // Rows already sent, for a big texture sent by strips
    private int uploadedRows;

    private int qualityType;

    private int textureType;

    public static void CreateErrorTexture() {
        GLResources.getInstance().addElementOrReturnIfPresent(Texture.class, new Texture(Resources.FALLBACK_NAME), Resources.FALLBACK_NAME);
    }

    public static void DeletePlaceholder() {
        if (placeholderId != -1) {
            GenDeleteResources.DeleteTexture(placeholderId);
            placeholderId = -1;
        }

        if (fallbackRectangleId != -1) {
            GenDeleteResources.DeleteTexture(fallbackRectangleId);
            fallbackRectangleId = -1;
        }
    }

    // The fallback checkerboard as a rectangle texture, for the samplers of rectangle textures (fonts); -1 until ready
    private static int getFallbackRectangleId() {
        if (fallbackRectangleId == -1) {
            TextureData fallback = Resources.getInstance().getFallback(TextureData.class);
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

    private static int getPlaceholderId() {
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

    public Texture(String name) {
        this(findData(name));
    }

    // A missing texture would wait forever to be loaded (and block a loading screen): the fallback replaces it
    private static TextureData findData(String name) {
        return Resources.getInstance().getResourceOrFallback(TextureData.class, name);
    }

    public Texture(TextureData data) {
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
            glBindTexture(textureType, textureId);
            EngineStats.countTextureBind();

            // Called for each draw: avoid building the message when checks are disabled
            if (Logger.DO_CHECK)
                Logger.CheckOpenGLError("Bind texture : " + data.getDataName() + " , texture id : " + textureId + " , texture type : " + textureType);
        // Failed or still loading: the fallback texture instead
        } else {
            // Same target as this texture: a rectangle sampler can't read a 2D texture
            int rectangleId = data != null && data.defaultTextureType == GL_TEXTURE_RECTANGLE ? getFallbackRectangleId() : -1;
            if (rectangleId != -1)
                glBindTexture(GL_TEXTURE_RECTANGLE, rectangleId);
            else
                glBindTexture(GL_TEXTURE_2D, getFallbackId());
            EngineStats.countTextureBind();
        }
    }

    // Texture of the fallback data (CreateErrorTexture), transparent until it is sent itself
    private int getFallbackId() {
        Texture fallback = GLResources.getInstance().getResource(Texture.class, Resources.FALLBACK_NAME);
        if (fallback != null && fallback != this && fallback.isLoaded())
            return fallback.textureId;

        return getPlaceholderId();
    }

    // Data giving the size: the fallback one while this one is not loaded
    private TextureData sizeData() {
        return Resources.getInstance().usable(TextureData.class, data);
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

        try {
            if (textureId == -1) {
                qualityType = data.defaultAspectTexture;
                textureType = data.defaultTextureType;

                textureId = GenDeleteResources.GenTexture();
                uploadedRows = 0;

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

            sendStrips(remainingMilliseconds);

            if (uploadedRows < data.getHeight())
                return false;

            return finishLoad();
        } catch (Exception e) {
            System.err.println("Fail to create texture " + data.path() + " :");
            e.printStackTrace();
            GenDeleteResources.DeleteTexture(textureId);
            textureId = -1;

            return false;
        }
    }

    private void sendStrips(int remainingMilliseconds) {
        int width = data.getWidth();
        int height = data.getHeight();
        int rowBytes = width * 4;
        long start = System.nanoTime();

        do {
            double leftMilliseconds = remainingMilliseconds - (System.nanoTime() - start) / 1e6;
            long stripBytes = (long) Math.max(MIN_STRIP_BYTES, Math.min(MAX_STRIP_BYTES, bytesPerMillisecond * leftMilliseconds));
            int rows = (int) Math.max(1, Math.min(height - uploadedRows, stripBytes / rowBytes));

            ByteBuffer strip = data.getData().duplicate();
            strip.position(uploadedRows * rowBytes);
            strip.limit((uploadedRows + rows) * rowBytes);

            long stripStart = System.nanoTime();
            glTexSubImage2D(textureType, 0, 0, uploadedRows, width, rows, GL_RGBA, GL_UNSIGNED_BYTE, strip);
            Logger.CheckOpenGLError("Send texture strip : " + data.getDataName());
            uploadedRows += rows;

            // Smoothed, a single slow strip must not shrink the next ones too much
            double stripMilliseconds = Math.max(0.01, (System.nanoTime() - stripStart) / 1e6);
            bytesPerMillisecond = bytesPerMillisecond * 0.7 + ((double) rows * rowBytes / stripMilliseconds) * 0.3;
        } while (uploadedRows < height && (System.nanoTime() - start) / 1e6 < remainingMilliseconds);
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
        return isLoaded() ? textureId : getFallbackId();
    }

    @Override
    public int getWidth() {
        return isLoaded() ? data.getWidth() : sizeData().getWidth();
    }

    @Override
    public int getHeight() {
        return isLoaded() ? data.getHeight() : sizeData().getHeight();
    }

    @Override
    public void unload(int remainingMilliseconds) {
        // The GL texture exists independently of its CPU data, which can already be unloaded
        if (textureId != -1) {
            GenDeleteResources.DeleteTexture(textureId);
            textureId = -1;
        }

        uploadedRows = 0;
    }
}
