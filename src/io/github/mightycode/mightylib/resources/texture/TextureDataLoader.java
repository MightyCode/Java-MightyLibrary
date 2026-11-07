package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.Resources;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public class TextureDataLoader extends ResourceLoader {
    private final static String LIST_TEXTURES = Resources.FOLDER + "textures/textures.json";

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
        JSONObject obj = new JSONObject(FileMethods.readFileAsString(LIST_TEXTURES));
        create(data, obj, "");
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
                        Resources.FOLDER + currentPath + information.getString("file"));

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
        BufferedImage image;
        try (FileInputStream input = new FileInputStream(textureData.path())) {
            image = ImageIO.read(input);
        } catch (Exception e) {
            throw new IllegalStateException("Can't read the texture file " + textureData.path(), e);
        }

        if (image == null)
            throw new IllegalStateException("Unknown image format: " + textureData.path());

        textureData.setBufferedImage(image);
    }
}
