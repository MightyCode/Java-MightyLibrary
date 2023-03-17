package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.DataType;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Icon extends DataType {
    private int width;
    private int height;

    private final GLFWImage.Buffer buffer;

    public Icon(String dataName, String path) {
        super(dataName, path);

        buffer = GLFWImage.create(1);
    }

    public void createIcon(BufferedImage img){
        int[] pixels = new int[img.getHeight() * img.getWidth()];

        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

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
        GLFWImage iconGI = GLFWImage.create().set(width, height, byteBuffer);
        buffer.put(0, iconGI);

        correctlyLoaded = true;
    }

    public int getWidth(){ return width; }
    public int getHeight(){ return height; }

    public GLFWImage.Buffer getBuffer(){ return buffer; }

    @Override
    public void unload() {
        buffer.clear();

        correctlyLoaded = false;
    }
}
