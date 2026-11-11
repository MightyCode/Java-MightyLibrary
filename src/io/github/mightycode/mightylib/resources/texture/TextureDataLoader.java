package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public class TextureDataLoader extends ResourceLoader {
    private static final String LIST_TEXTURES = "textures/textures.json";
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "bmp", "tga", "gif"));

    @Override
    public Class<? extends DataType> getType() {
        return TextureData.class;
    }

    @Override
    public String getResourceNameType() {
        return "TextureData";
    }

    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString(resources().folder() + LIST_TEXTURES));
        create(data, obj, "");
    }

    /**
     * The textures of textures.json, plus the images of the textures folder it does not list: named after their file,
     * with the default parameters of TextureData (realistic, 2D) and a warning, so a file dropped in the folder can be
     * used (and reloaded) without editing the list.
     */
    @Override
    public void discover(Map<String, DataType> data) {
        create(data);

        Set<Path> listed = new HashSet<>();
        for (DataType texture : data.values())
            listed.add(Paths.get(((TextureData) texture).path()).toAbsolutePath().normalize());

        Path folder = Paths.get(resources().folder() + "textures");
        if (!Files.isDirectory(folder))
            return;

        try (Stream<Path> files = Files.walk(folder)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                String fileName = file.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                if (dot <= 0 || !IMAGE_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT)))
                    return;
                if (listed.contains(file.toAbsolutePath().normalize()))
                    return;

                String name = fileName.substring(0, dot);
                if (data.containsKey(name)) {
                    System.err.println("--Texture " + file + " is not in textures.json and its name '" + name
                            + "' is already used: ignored");
                    return;
                }

                System.err.println("--Texture " + file + " is not in textures.json: default parameters (realistic, 2D)");
                data.put(name, new TextureData(name, file.toString().replace('\\', '/')));
            });
        } catch (IOException e) {
            throw new ResourceException("can't list " + folder, e);
        }
    }

    @Override
    public DataType createFallback() {
        return new FallbackTextureData();
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {}

    @Override
    public String filterFile(String path) {
        return null;
    }


    private void create(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.getJSONObject(currentNode).has("file")){
                JSONObject information = node.getJSONObject(currentNode);
                String temp = information.getString("qualityType");

                TextureData currentTexture = new TextureData(currentNode,
                        resources().folder() + currentPath + information.getString("file"));

                if (temp.equals("pixelart")){
                    currentTexture.defaultAspectTexture = TextureParameters.PIXEL_ART_PARAMETERS;
                } else if (temp.equals("realistic")){
                    currentTexture.defaultAspectTexture = TextureParameters.REALISTIC_PARAMETERS;
                }

                if (information.has("textureType")) {
                    temp = information.getString("textureType");
                    if (temp.equals("rectangle"))
                        currentTexture.defaultTextureType = GL_TEXTURE_RECTANGLE;
                }

                data.put(currentNode, currentTexture);
            } else {
                create(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            }
        } while(arrayNodes.hasNext());
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof TextureData))
            return;

        TextureData textureData = (TextureData) dataType;

        // Failing here makes the preload fail: the fallback replaces the texture
        ByteBuffer file = readFile(textureData);

        // The decoding is the costly part
        textureData.checkCancelled();

        // stb_image decodes straight to RGBA; ImageIO (slower) reads what it does not know
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(file, width, height, channels, 4);
            if (pixels != null) {
                textureData.setDecodedPixels(pixels, width.get(0), height.get(0));
                return;
            }

            System.err.println("--Texture " + textureData.getDataName() + ": stb can't read it ("
                    + STBImage.stbi_failure_reason() + "), ImageIO is used");
        }

        BufferedImage image;
        try (InputStream input = new ByteArrayInputStream(toArray(file))) {
            image = ImageIO.read(input);
        } catch (Exception e) {
            throw new ResourceException("can't read the texture file " + textureData.path(), e);
        }

        if (image == null)
            throw new ResourceException("unknown image format: " + textureData.path());

        textureData.setBufferedImage(image);
    }

    private static ByteBuffer readFile(TextureData textureData) {
        try (FileChannel channel = FileChannel.open(Paths.get(textureData.path()), StandardOpenOption.READ)) {
            long size = channel.size();
            if (size <= 0 || size > Integer.MAX_VALUE)
                throw new ResourceException("can't read the texture file " + textureData.path() + " (size " + size + ")");

            ByteBuffer file = BufferUtils.createByteBuffer((int) size);
            while (file.hasRemaining()) {
                if (channel.read(file) < 0)
                    break;
            }

            file.flip();
            return file;
        } catch (IOException e) {
            throw new ResourceException("can't read the texture file " + textureData.path(), e);
        }
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
