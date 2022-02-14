package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.sounds.SoundData;
import MightyLibrary.mightylib.util.math.KeyTree;
import MightyLibrary.mightylib.util.math.KeyTreeNode;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public abstract class SoundLoader {
    public static void load(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/sounds/sounds.json"));
        obj = obj.getJSONObject("sounds");

        SoundLoader.load(data, obj, "");
    }

    private static void load(Map<String, DataType> data, JSONObject node, String currentPath){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                SoundLoader.load(data, node.getJSONObject(currentNode), currentPath + currentNode + "/");
            } else {
                data.put(currentNode, new SoundData(currentNode, currentPath + node.getString(currentNode)));
            }
        } while(arrayNodes.hasNext());
    }


    public static void loadGainTree(KeyTree<String, Float> gainTree){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/sounds/sounds.json"));
        obj = obj.getJSONObject("gainTree");

        Iterator<String> arrayNodes = obj.keys();

        do{
            String currentNode = arrayNodes.next();

            SoundLoader.loadGainNode(gainTree, obj.getJSONObject(currentNode), currentNode, null);
        } while(arrayNodes.hasNext());

    }

    private static void loadGainNode(KeyTree<String, Float> gainTree, JSONObject node, String name, KeyTreeNode<String, Float> predecessor){
        Iterator<String> arrayNodes = node.keys();

        if(!arrayNodes.hasNext()) return;

        KeyTreeNode<String, Float> current;
        if (predecessor == null)
            current = gainTree.addNewNode(gainTree.NoRoot, name, node.getFloat("value"));
        else {
            current = gainTree.addNewNode(predecessor.getKey(), name, node.getFloat("value"));
        }

        do{
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                SoundLoader.loadGainNode(gainTree, node.getJSONObject(currentNode), currentNode, current);
            }
        } while(arrayNodes.hasNext());
    }
}
