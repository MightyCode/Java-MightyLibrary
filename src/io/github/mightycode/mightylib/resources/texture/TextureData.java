package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.utils.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class TextureData extends SingleSourceDataType {
    protected int width;
    protected int height;

    private int[] pixelCache = null;

    private BufferedImage tempBufferedImage;
    // Decoded by stb_image (native memory, freed by freeNativePixels), waiting for internLoad
    private ByteBuffer decodedPixels;
    private int decodedWidth, decodedHeight;
    ByteBuffer byteBuffer;
    // byteBuffer is owned by stb_image: it must be freed, not left to the garbage collector
    private boolean nativePixels;

    int defaultAspectTexture;
    int defaultTextureType;

    public TextureData(String name, String path) {
        this(TYPE_SET_UP.THREAD_CONTEXT, name, path);
    }

    public TextureData(TYPE_SET_UP typeSetUp, String name, String path) {
        super(typeSetUp, name, path);

        defaultTextureType = GL_TEXTURE_2D;
        defaultAspectTexture = TextureParameters.REALISTIC_PARAMETERS;
        tempBufferedImage = null;

        internUnload();
    }

    // Subtypes give their own copy: a TextureData copy would lose what they add
    @Override
    protected DataType createReloadCopy() {
        if (getClass() != TextureData.class)
            return null;

        TextureData copy = new TextureData(getTypeSetUp(), dataName, path);
        copy.copySettingsFrom(this);
        return copy;
    }

    // Set by the code, not read from the file: kept by the reload copy
    protected final void copySettingsFrom(TextureData original) {
        defaultAspectTexture = original.defaultAspectTexture;
        defaultTextureType = original.defaultTextureType;
    }

    @Override
    protected void swapContent(DataType copy) {
        TextureData other = (TextureData) copy;

        int otherWidth = other.width, otherHeight = other.height;
        other.width = width;
        other.height = height;
        width = otherWidth;
        height = otherHeight;

        ByteBuffer otherBuffer = other.byteBuffer;
        other.byteBuffer = byteBuffer;
        byteBuffer = otherBuffer;

        boolean otherNative = other.nativePixels;
        other.nativePixels = nativePixels;
        nativePixels = otherNative;

        int[] otherCache = other.pixelCache;
        other.pixelCache = pixelCache;
        pixelCache = otherCache;
    }

    public void setBufferedImage(BufferedImage img) {
        tempBufferedImage = img;
    }

    // RGBA pixels decoded by stb_image (native memory), taken over by this data: freed when it is unloaded
    void setDecodedPixels(ByteBuffer pixels, int width, int height) {
        freeDecodedPixels();
        decodedPixels = pixels;
        decodedWidth = width;
        decodedHeight = height;
    }

    @Override
    protected boolean internLoad() {
        if (decodedPixels != null) {
            freeNativePixels();
            byteBuffer = decodedPixels;
            nativePixels = true;
            width = decodedWidth;
            height = decodedHeight;
            decodedPixels = null;
            return true;
        }

        int[] pixels = new int[tempBufferedImage.getHeight() * tempBufferedImage.getWidth()];

        tempBufferedImage.getRGB(0, 0, tempBufferedImage.getWidth(), tempBufferedImage.getHeight(), pixels, 0, tempBufferedImage.getWidth());

        byteBuffer = BufferUtils.createByteBuffer(tempBufferedImage.getWidth() * tempBufferedImage.getHeight() * 4);

        this.width = tempBufferedImage.getWidth();
        this.height = tempBufferedImage.getHeight();

        for (int a = 0; a < height; ++a) {
            for (int b = 0; b < width; ++b) {
                int pixel = pixels[a * width + b];
                byteBuffer.put((byte) ((pixel >> 16) & 0xFF));
                byteBuffer.put((byte) ((pixel >> 8)  & 0xFF));
                byteBuffer.put((byte) ((pixel        & 0xFF)));
                byteBuffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        byteBuffer.flip();
        tempBufferedImage = null;

        return true;
    }

    public BufferedImage loadBufferedImage(){
        try (FileInputStream input = new FileInputStream(path())) {
            return ImageIO.read(input);
        } catch (IOException e) {
            Logger.LogException("Can't read the image " + path(), e);
        }

        return null;
    }

    // Decoded pixels (RGBA) once the file is read, 0 before
    private long decodedPixelBytes() {
        if (decodedPixels != null)
            return (long) decodedWidth * decodedHeight * 4;
        if (byteBuffer != null)
            return byteBuffer.capacity();
        if (tempBufferedImage != null)
            return (long) tempBufferedImage.getWidth() * tempBufferedImage.getHeight() * 4;

        return 0;
    }

    // The pixels once read; before, a guess: a compressed file is about four times smaller than its pixels
    @Override
    public long estimatedCpuBytes() {
        long decoded = decodedPixelBytes();
        return decoded > 0 ? decoded : super.estimatedCpuBytes() * 4;
    }

    // Without mipmaps
    @Override
    public long estimatedGpuBytes() {
        return estimatedCpuBytes();
    }

    // The texture the renderers share under the name of the data
    @Override
    public Class<? extends GLElement> glElementType() {
        return Texture.class;
    }

    @Override
    public GLElement createGLElement() {
        return new Texture(this);
    }

    public ByteBuffer getData() {
        return byteBuffer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setDefaultAspectTexture(int defaultAspectTexture) {
        this.defaultAspectTexture = defaultAspectTexture;
    }

    public void setDefaultTextureType(int defaultTextureType) {
        this.defaultTextureType = defaultTextureType;
    }

    @Override
    protected void internCancelPreload() {
        tempBufferedImage = null;
        freeDecodedPixels();
    }

    private void freeDecodedPixels() {
        if (decodedPixels != null) {
            STBImage.stbi_image_free(decodedPixels);
            decodedPixels = null;
        }
    }

    private void freeNativePixels() {
        if (nativePixels && byteBuffer != null)
            STBImage.stbi_image_free(byteBuffer);

        nativePixels = false;
        byteBuffer = null;
    }

    @Override
    public void internUnload() {
        freeNativePixels();
        pixelCache = null;
        width = -1;
        height = -1;
    }


    public void computeCache() {
        if (pixelCache != null)
            return;

        pixelCache = new int[width * height];

        for (int a = 0; a < height; ++a) {
            for (int b = 0; b < width; ++b) {
                int index = a * width + b;
                pixelCache[index] = getPixel(index);
            }
        }
    }

    public int getPixelCached(int x, int y) {
        return getPixelCached(y * width + x);
    }

    public int getPixelCached(int index) {
        return pixelCache[index];
    }

    public int getPixel(int x, int y) {
        if (byteBuffer == null || x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException("Invalid coordinates or texture not loaded.");
        }

        return getPixel(y * width + x);
    }

    public int getPixel(int index) {
        byte r = byteBuffer.get(index * 4);
        byte g = byteBuffer.get(index * 4 + 1);
        byte b = byteBuffer.get(index * 4 + 2);
        byte a = byteBuffer.get(index * 4 + 3);

        // Combine RGBA bytes into a single int
        int red   = r & 0xFF;
        int green = g & 0xFF;
        int blue  = b & 0xFF;
        int alpha = a & 0xFF;

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
