package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Icon extends SingleSourceDataType {
    private int width;
    private int height;

    private BufferedImage tempImg;
    private final GLFWImage.Buffer buffer;

    public Icon(String dataName, String path) {
        super(TYPE_SET_UP.MAIN_CONTEXT, dataName, path);

        buffer = GLFWImage.create(1);
        tempImg = null;
    }

    public void setBufferedImage(BufferedImage img){
        tempImg = img;
    }

    @Override
    protected boolean internLoad() {
        int[] pixels = new int[tempImg.getHeight() * tempImg.getWidth()];

        tempImg.getRGB(0, 0, tempImg.getWidth(), tempImg.getHeight(), pixels, 0, tempImg.getWidth());

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(tempImg.getWidth() * tempImg.getHeight() * 4);

        this.width = tempImg.getWidth();
        this.height = tempImg.getHeight();

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
        tempImg = null;
        GLFWImage iconGI = GLFWImage.create().set(width, height, byteBuffer);
        buffer.put(0, iconGI);

        return true;
    }

    public int getWidth(){ return width; }
    public int getHeight(){ return height; }

    public GLFWImage.Buffer getBuffer(){ return buffer; }

    @Override
    public void internUnload() {
        buffer.clear();
    }
}
