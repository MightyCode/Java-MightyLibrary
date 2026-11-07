package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.sounds.SoundDataType;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import io.github.mightycode.mightylib.utils.math.KeyTree;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.json.JSONObject;
import org.lwjgl.stb.STBVorbisInfo;

import java.util.Iterator;
import java.util.Map;

public class SoundLoader extends ResourceLoader {

    @Override
    public Class<? extends DataType> getType() {
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
    public DataType createFallback() {
        return new FallbackSoundData();
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".ogg")) return getFileName(path);

        return null;
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new SoundData(name, currentPath));
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof SoundData))
            return;

        SoundData sound = (SoundData) dataType;

        String path = sound.path();
        int lastIndex = path.lastIndexOf('.');

        SoundLoadInfo info = new SoundLoadInfo();

        // Failing here makes the preload fail: the fallback replaces the sound
        if (path.indexOf("ogg", lastIndex) == -1 || !loadOgg(path, info))
            throw new IllegalStateException("Can't read the sound file " + path);

        sound.setSoundLoadInfo(info);
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

        do {
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                SoundLoader.loadGainNode(gainTree, node.getJSONObject(currentNode), currentNode, current);
            }
        } while(arrayNodes.hasNext());
    }
}
