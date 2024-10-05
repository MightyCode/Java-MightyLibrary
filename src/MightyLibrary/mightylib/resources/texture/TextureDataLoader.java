package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public class TextureDataLoader extends ResourceLoader {
    private final static String LIST_TEXTURES = Resources.FOLDER + "textures/textures.json";

    @Override
    public Class<? extends DataType> getType() {
        return TextureData.class;
    }

    @Override
    public String getResourceNameType() {
        return "Texture";
    }

    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString(LIST_TEXTURES));

        TextureData texture = new TextureData("error", Resources.FOLDER + "textures/error.png");

        data.put("error", texture);
        create(data, obj, "");
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
    public void load(DataType dataType) {
        if (!(dataType instanceof TextureData))
            return;

        TextureData texture = (TextureData) dataType;

        try {
            BufferedImage image = ImageIO.read(new FileInputStream(texture.path()));
            texture.load(image);

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(texture.path() + "\n");
            e.printStackTrace();
        }
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        if (data.containsKey(resourceName))
            return;

        TextureData currentTexture = new TextureData(resourceName, resourcePath);
        data.put(resourceName, currentTexture);

        load(currentTexture);
    }
}
