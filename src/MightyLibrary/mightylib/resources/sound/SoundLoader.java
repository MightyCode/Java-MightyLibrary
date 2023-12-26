package MightyLibrary.mightylib.resources.sound;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.sounds.SoundDataType;
import MightyLibrary.mightylib.sounds.SoundLoadInfo;
import MightyLibrary.mightylib.utils.math.KeyTree;
import MightyLibrary.mightylib.utils.math.KeyTreeNode;
import org.json.JSONObject;
import org.lwjgl.stb.STBVorbisInfo;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

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
        exploreResourcesFile(data, Resources.FOLDER);
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending.equals(".ogg"))
            return getFileName(path);

        return null;
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new SoundData(name, currentPath));
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

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        // Todo
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


    public static void loadGainTree(String gainTreePath, KeyTree<String, Float> gainTree){
        gainTreePath = Resources.FOLDER + gainTreePath;

        JSONObject obj = new JSONObject(FileMethods.readFileAsString(gainTreePath));

        Iterator<String> arrayNodes = obj.keys();

        do{
            String currentNode = arrayNodes.next();

            SoundLoader.loadGainNode(gainTree, obj.getJSONObject(currentNode), currentNode, null);
        } while(arrayNodes.hasNext());

    }

    private static void loadGainNode(KeyTree<String, Float> gainTree, JSONObject node,
                                     String name, KeyTreeNode<String, Float> predecessor){
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
