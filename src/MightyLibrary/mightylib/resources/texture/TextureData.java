package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class TextureData extends SingleSourceDataType {
    protected int width;
    protected int height;

    ByteBuffer byteBuffer;

    int defaultAspectTexture;
    int defaultTextureType;

    public TextureData(String name, String path) {
        super(name, path);

        defaultTextureType = GL_TEXTURE_2D;
        defaultAspectTexture = TextureParameters.REALISTIC_PARAMETERS;

        unload();
    }

    public void load(BufferedImage img) {
        int[] pixels = new int[img.getHeight() * img.getWidth()];

        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        byteBuffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

        this.width = img.getWidth();
        this.height = img.getHeight();

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

        correctlyLoaded = true;
    }

    public BufferedImage loadBufferedImage(){
        try {
            return ImageIO.read(new FileInputStream(path()));

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(path() + "\n");
            e.printStackTrace();
        }

        return null;
    }

    public ByteBuffer getData(){
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
    public void unload() {
        byteBuffer = null;
        width = -1;
        height = -1;
        correctlyLoaded = false;
    }
}
