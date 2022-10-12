package MightyLibrary.mightylib.graphics.texture;

import MightyLibrary.mightylib.resources.DataType;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Icon extends DataType {
    public int width;

    public int height;

    public ByteBuffer Buffer;

    public Icon(String dataName, String path) {
        super(dataName, path);

        BufferedImage img = null;
        try {
            img = ImageIO.read(new FileInputStream("resources/icon.png"));

            int[] pixels = new int[img.getHeight() * img.getWidth()];

            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

            Buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

            this.width = img.getWidth();
            this.height = img.getHeight();

            for (int a = 0; a < height; ++a) {
                for (int b = 0; b < width; ++b) {
                    int pixel = pixels[a * width + b];
                    Buffer.put((byte) ((pixel >> 16) & 0xFF));
                    Buffer.put((byte) ((pixel >> 8)  & 0xFF));
                    Buffer.put((byte) ((pixel        & 0xFF)));
                    Buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }

            Buffer.flip();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean unload() {
        return false;
    }
}
