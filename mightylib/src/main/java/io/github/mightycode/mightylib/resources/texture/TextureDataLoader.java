package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.source.SourceEntry;
import org.json.JSONObject;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public final class TextureDataLoader extends ResourceLoader {
    private static final String TEXTURES_FOLDER = "textures";
    private static final String LIST_TEXTURES = TEXTURES_FOLDER + "/textures.json";
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
        // Merged by entry: the textures of a higher source are added, or replace those of the same name
        for (JSONObject layer : FileMethods.readJSONObjectLayers(resources().sources(), LIST_TEXTURES))
            create(data, layer, "");
    }

    /**
     * The textures of textures.json, plus the images of the textures folder it does not list: named after their file,
     * with the default parameters of TextureData (realistic, 2D) and a warning, so a file dropped in the folder can be
     * used (and reloaded) without editing the list.
     */
    @Override
    public void discover(Map<String, DataType> data) {
        create(data);

        Set<String> listed = new HashSet<>();
        for (DataType texture : data.values()) {
            String path = ((TextureData) texture).path();
            if (path != null && !path.isEmpty())
                listed.add(ResourceSources.normalize(path));
        }

        discoverImages(data, listed, TEXTURES_FOLDER);
    }

    // The images of the folder and its sub-folders, in every source, that textures.json does not list
    private void discoverImages(Map<String, DataType> data, Set<String> listed, String folder) {
        for (SourceEntry entry : resources().sources().list(folder)) {
            if (entry.isFolder()) {
                discoverImages(data, listed, entry.path());
                continue;
            }

            String fileName = entry.name();
            int dot = fileName.lastIndexOf('.');
            if (dot <= 0 || !IMAGE_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT)))
                continue;
            if (listed.contains(entry.path()))
                continue;

            String name = fileName.substring(0, dot);
            if (data.containsKey(name)) {
                System.err.println("--Texture " + entry.path() + " is not in textures.json and its name '" + name
                        + "' is already used: ignored");
                continue;
            }

            System.err.println("--Texture " + entry.path() + " is not in textures.json: default parameters"
                    + " (realistic, 2D)");
            data.put(name, new TextureData(name, entry.path()));
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

                TextureData currentTexture = new TextureData(currentNode, currentPath + information.getString("file"));

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
        byte[] undecoded;
        try {
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
            undecoded = toArray(file);
        } finally {
            MemoryUtil.memFree(file);
        }

        BufferedImage image;
        try (InputStream input = new ByteArrayInputStream(undecoded)) {
            image = ImageIO.read(input);
        } catch (Exception e) {
            throw new ResourceException("can't read the texture file " + textureData.path(), e);
        }

        if (image == null)
            throw new ResourceException("unknown image format: " + textureData.path());

        textureData.setBufferedImage(image);
    }

    // A native buffer, freed by the caller (MemoryUtil.memFree)
    private ByteBuffer readFile(TextureData textureData) {
        ByteBuffer file;
        try {
            file = resources().sources().readNative(textureData.path());
        } catch (NoSuchFileException e) {
            throw new ResourceException("texture file not found: " + textureData.path());
        } catch (IOException e) {
            throw new ResourceException("can't read the texture file " + textureData.path(), e);
        } catch (IllegalArgumentException e) {
            // Absolute or with "..": written in textures.json
            throw new ResourceException(e.getMessage());
        }

        if (!file.hasRemaining()) {
            MemoryUtil.memFree(file);
            throw new ResourceException("empty texture file " + textureData.path());
        }
        return file;
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
