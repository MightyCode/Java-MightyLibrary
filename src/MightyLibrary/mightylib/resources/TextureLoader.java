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

    public TextureLoader(){
        super(Texture.class);
    }

    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/textures/textures.json"));
        obj = obj.getJSONObject("textures");

        data.put("error", new Texture("error", "error.png"));
        create(data, obj, "");
    }


    private void create(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                create(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            } else {
                data.put(currentNode, new Texture(currentNode, currentPath + node.getString(currentNode)));
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
            texture.createImage(image, TextureParameters.PIXEL_ART_PARAMETERS);

        } catch (Exception e) {
            System.err.println("Can't find the path for :");
            System.err.println(texture.path + "\n");
            e.printStackTrace();

            return false;
        }

        return true;
    }
}
