package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Map;

import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public class TextureLoader extends ResourceLoader {

    private final static String BASIC_PATH = "resources/textures/";
    private final static String LIST_TEXTURES = BASIC_PATH + "textures.json";

    @Override
    public Class<?> getType() {
        return Texture.class;
    }

    @Override
    public String getResourceNameType() {
        return "Texture";
    }

    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString(LIST_TEXTURES));
        obj = obj.getJSONObject("textures");

        Texture texture = new Texture("error", BASIC_PATH + "error.png");
        texture.setAspectTexture(TextureParameters.PIXEL_ART_PARAMETERS);

        data.put("error", texture);
        create(data, obj, "");
    }


    private void create(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.getJSONObject(currentNode).has("file")){
                JSONObject information = node.getJSONObject(currentNode);
                String temp = information.getString("qualityType");

                Texture currentTexture = new Texture(currentNode, currentPath + information.getString("file"));

                if (temp.equals("pixelart")){
                    currentTexture.setAspectTexture(TextureParameters.PIXEL_ART_PARAMETERS);
                } else if (temp.equals("realistic")){
                    currentTexture.setAspectTexture(TextureParameters.REALISTIC_PARAMETERS);
                }

                if (information.has("textureType")) {
                    temp = information.getString("textureType");
                    if (temp.equals("rectangle"))
                        currentTexture.setTextureType(GL_TEXTURE_RECTANGLE);
                }

                data.put(currentNode, currentTexture);
            } else {
                create(data, node.getJSONObject(currentNode), BASIC_PATH + currentPath + currentNode + "/");
            }
        } while(arrayNodes.hasNext());
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof Texture))
            return;

        Texture texture = (Texture) dataType;

        try {
            BufferedImage image = ImageIO.read(new FileInputStream(texture.getPath()));
            texture.createImage(image);

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(texture.getPath() + "\n");
            e.printStackTrace();
        }
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        if (data.containsKey(resourceName))
            return;

        Texture currentTexture = new Texture(resourceName, resourcePath);
        data.put(resourceName, currentTexture);

        load(currentTexture);
    }
}
