/*
 * Copyright (c) 2018 Amaury Rehel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package MightyLibrary.render.texture;

import MightyLibrary.util.ObjectId;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;


public class Texture extends ObjectId {
    private final static String PATH = "resources/textures/";

    private int textureId;

    private int width;

    private int height;

    private boolean correctLoaded;

    private String path;

    private ByteBuffer buffer;

    public Texture(String name, String path) {
        super(name);
        load(path);
    }

    public Texture(String name, BufferedImage image) {
        super(name);
        createImage(image);
    }

    public void bind() {
        bind(0);
    }

    public void bind(int texturePos) {
        glActiveTexture(GL_TEXTURE0 + texturePos);
        if (correctLoaded){
            glBindTexture(GL_TEXTURE_2D, textureId);
        } else glBindTexture(GL_TEXTURE_2D, 1);
    }

    public void load(String path) {
        try {
            this.path = path;
            BufferedImage image = ImageIO.read(new FileInputStream(PATH +  this.path));
            createImage(image);
        } catch (Exception e) {
            //e.printStackTrace();
            System.err.println("Can't find the path for :");
            System.out.println(PATH +  this.path + "\n");
            correctLoaded = false;
        }
    }

    private void createImage(BufferedImage image) {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        try {
            int[] pixels = new int[image.getHeight() * image.getWidth()];

            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

            buffer = BufferUtils.createByteBuffer(image.getHeight() * image.getWidth() * 4);

            this.width = image.getWidth();
            this.height = image.getHeight();

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int pixel = pixels[i * width + j];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // RED
                    buffer.put((byte) ((pixel >> 8) & 0xFF)); // GREEN
                    buffer.put((byte) ((pixel & 0xFF))); // BLUE
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // ALPHA
                }
            }
            buffer.flip();

            //Pixel art
            /*glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            */

            //Realistic
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);


            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            // Temp texture with lowest quality (far texture)
            glGenerateMipmap(GL_TEXTURE_2D);

            System.out.println("Texture num : " + textureId + " , loaded with path : " + path);
            correctLoaded = true;
        } catch (Exception e) {
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

    private void setParam(int param, int value) {
        bind();
        if (correctLoaded)  glTexParameteri(GL_TEXTURE_2D, param, value);
    }

    public void unload() {
        if (correctLoaded) glDeleteTextures(textureId);
    }
}
