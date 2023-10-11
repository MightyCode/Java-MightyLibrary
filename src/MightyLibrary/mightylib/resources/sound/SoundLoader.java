package MightyLibrary.mightylib.resources.sound;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.data.JSONFile;
import MightyLibrary.mightylib.sounds.SoundDataType;
import MightyLibrary.mightylib.sounds.SoundLoadInfo;
import MightyLibrary.mightylib.util.math.KeyTree;
import MightyLibrary.mightylib.util.math.KeyTreeNode;
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
        create(data, "resources");
    }

    private void create(Map<String, DataType> data, final String currentPath){
        File file = new File(currentPath);

        if (file.isFile()){
            String name = currentPath.substring(currentPath.lastIndexOf("/") + 1, currentPath.lastIndexOf("."));
            String ending = currentPath.substring(currentPath.lastIndexOf("."));

            if (ending.equals(".ogg")) {
                data.put(name, new SoundData(name, currentPath));
                System.out.println("Create sound res : " + currentPath);
            }
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, currentPath + "/" + childPath);
            }
        }
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
