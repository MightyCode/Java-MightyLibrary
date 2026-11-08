package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
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
        if (path.indexOf("ogg", lastIndex) == -1)
            throw new ResourceException("unsupported sound format (only ogg): " + path);

        loadOgg(path, info, sound::checkCancelled);
        sound.setSoundLoadInfo(info);
    }


    // Throws a ResourceException when the file is missing or not a valid ogg vorbis file
    // checkCancelled is called between the steps of the decoding (DataType.checkCancelled)
    public void loadOgg(String path, SoundLoadInfo sInfo, Runnable checkCancelled) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            sInfo.Buffer = SoundDataType.readVorbis(path, 32 * 1024, info, checkCancelled, SoundDataType.DECODE_CHUNK_SAMPLES);
            sInfo.nativeBuffer = true;
            sInfo.Channel = info.channels();
            sInfo.SampleRate = info.sample_rate();
        } catch (ResourceException | java.util.concurrent.CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceException("can't read the sound file " + path, e);
        }
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
