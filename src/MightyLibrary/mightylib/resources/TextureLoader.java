package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.graphics.texture.TextureParameters;
import MightyLibrary.mightylib.resources.map.TileMap;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Map;

public class TextureLoader extends ResourceLoader {

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
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/textures.json"));
        obj = obj.getJSONObject("textures");

        Texture texture = new Texture("error", "error.png");
        texture.setAspectTexture(TextureParameters.PIXEL_ART_PARAMETERS);

        data.put("error", texture);
        create(data, obj, "");
    }


    private void create(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.getJSONObject(currentNode).has("type")){
                JSONObject information = node.getJSONObject(currentNode);
                String type = information.getString("type");

                Texture currentTexture = new Texture(currentNode, currentPath + information.getString("file"));

                if (type.equals("pixelart")){
                    currentTexture.setAspectTexture(TextureParameters.PIXEL_ART_PARAMETERS);
                } else if (type.equals("realistic")){
                    currentTexture.setAspectTexture(TextureParameters.REALISTIC_PARAMETERS);
                }


                data.put(currentNode, currentTexture);
            } else {
                create(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            }
        } while(arrayNodes.hasNext());
    }


    @Override
    public boolean load(DataType dataType) {
        if (!(dataType instanceof Texture))
            return false;

        Texture texture = (Texture) dataType;

        try {
            BufferedImage image = ImageIO.read(new FileInputStream(texture.path));
            texture.createImage(image);

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(texture.path + "\n");
            e.printStackTrace();

            return false;
        }

        return true;
    }
}
