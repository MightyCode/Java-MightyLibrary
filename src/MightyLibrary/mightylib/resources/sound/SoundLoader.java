package MightyLibrary.mightylib.resources.sound;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.sounds.SoundDataType;
import MightyLibrary.mightylib.sounds.SoundLoadInfo;
import MightyLibrary.mightylib.util.math.KeyTree;
import MightyLibrary.mightylib.util.math.KeyTreeNode;
import org.json.JSONObject;
import org.lwjgl.stb.STBVorbisInfo;

import java.util.Iterator;
import java.util.Map;

public class SoundLoader extends ResourceLoader {

    @Override
    public Class<?> getType() {
        return SoundData.class;
    }

    @Override
    public String getResourceNameType() {
        return "SoundData";
    }


    @Override
    public void create(Map<String, DataType> data){
        JSONObject obj = new JSONObject(FileMethods.readFileAsString("resources/sounds/sounds.json"));
        obj = obj.getJSONObject("sounds");

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


    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof SoundData))
            return;

        SoundData sound = (SoundData) dataType;

        String path = sound.getPath();
        int lastIndex = path.lastIndexOf('.');

        SoundLoadInfo info = new SoundLoadInfo();

        if (path.indexOf("ogg", lastIndex) != -1)
            if(!loadOgg(path, info))
                return;


        sound.createSound(info);
    }


    public boolean loadOgg(String path, SoundLoadInfo sInfo){
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            try {
                sInfo.Buffer = SoundDataType.readVorbis(path, 32 * 1024, info);
            } catch (Exception e) {
                e.printStackTrace();

                return false;
            }

            // Copy to buffer
            sInfo.Channel = info.channels();
            sInfo.SampleRate = info.sample_rate();
        } catch (Exception e){
            e.printStackTrace();

            return false;
        }

        return true;
    }
}
