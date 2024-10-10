package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.graphics.surface.TextureParameters;
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

    private BufferedImage tempBufferedImage;
    ByteBuffer byteBuffer;

    int defaultAspectTexture;
    int defaultTextureType;

    public TextureData(String name, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, name, path);

        defaultTextureType = GL_TEXTURE_2D;
        defaultAspectTexture = TextureParameters.REALISTIC_PARAMETERS;
        tempBufferedImage = null;

        internUnload();
    }

    public void setBufferedImage(BufferedImage img) {
        tempBufferedImage = img;
    }

    @Override
    protected boolean internLoad() {
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
        try {
            return ImageIO.read(new FileInputStream(path()));

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(path() + "\n");
            e.printStackTrace();
        }

        return null;
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
    public void internUnload() {
        byteBuffer = null;
        width = -1;
        height = -1;
    }
}
