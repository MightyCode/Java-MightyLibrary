package MightyLibrary.mightylib.graphics.texture;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Texture extends DataType {
    private final static String PATH = "resources/textures/";

    private int width;
    private int height;

    private int textureId;

    private boolean correctLoaded;

    public Texture(String name, String path) {
        super(EDataType.Texture, name, path);
    }

    public void bind() {
        bind(0);
    }

    public void bind(int texturePos) {
        // Active the texture to right position
        glActiveTexture(GL_TEXTURE0 + texturePos);
        if (correctLoaded){
            glBindTexture(GL_TEXTURE_2D, textureId);
        // If isn't correct loaded, bind error texture
        } else glBindTexture(GL_TEXTURE_2D, 1);
    }


    @Override
    public boolean load() {
        try {
            BufferedImage image = ImageIO.read(new FileInputStream(PATH +  this.path));
            createImage(image);
            return true;
        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.out.println(PATH +  this.path + "\n");
            e.printStackTrace();
            correctLoaded = false;

            return false;
        }
    }

    private void createImage(BufferedImage img) {
        textureId = glGenTextures();

        try {
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

            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
            TextureParameters.pixelArtParameters();

            System.out.println("Texture : " + textureId + " , loaded with path : " + path);
            correctLoaded = true;
        } catch (Exception e) {
            System.err.println("Fail to load texture " + path + " :");
            e.printStackTrace();
            glDeleteTextures(textureId);
            correctLoaded = false;
        }
    }

    public int getTexId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void setTextParam(int param, int value) {
        bind();
        if (correctLoaded)  glTexParameteri(GL_TEXTURE_2D, param, value);
    }

    @Override
    public boolean unload() {
        if (correctLoaded) glDeleteTextures(textureId);
        return true;
    }
}
