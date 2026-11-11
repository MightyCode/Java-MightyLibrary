package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.sound.decoder.*;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import io.github.mightycode.mightylib.utils.math.KeyTree;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SoundLoader extends ResourceLoader {
    // The file formats supported (SoundDecoder), a decoder added later replaces the one of the same extension
    private final List<SoundDecoder> decoders = new ArrayList<>();

    public SoundLoader() {
        addDecoder(new OggDecoder());
        addDecoder(new WavDecoder());
        addDecoder(new Mp3Decoder());
        addDecoder(new FlacDecoder());
    }

    public void addDecoder(SoundDecoder decoder) {
        decoders.add(0, decoder);
    }

    private SoundDecoder findDecoder(String path) {
        String extension = getFileExtension(path);
        if (extension == null)
            return null;

        extension = extension.toLowerCase(Locale.ROOT);
        for (SoundDecoder decoder : decoders) {
            for (String supported : decoder.extensions()) {
                if (supported.equals(extension))
                    return decoder;
            }
        }

        return null;
    }

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
        exploreResourcesFile(data, resources().folder());
    }

    @Override
    public DataType createFallback() {
        return new FallbackSoundData();
    }

    @Override
    public String filterFile(String path) {
        return findDecoder(path) != null ? getFileName(path) : null;
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        if (data.containsKey(name))
            System.err.println("--Sound name " + name + " used by several files, " + currentPath + " replaces the other");

        data.put(name, new SoundData(name, currentPath));
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof SoundData))
            return;

        SoundData sound = (SoundData) dataType;

        String path = sound.path();

        // Failing here makes the preload fail: the fallback replaces the sound
        SoundDecoder decoder = findDecoder(path);
        if (decoder == null)
            throw new ResourceException("unsupported sound format: " + path);

        SoundLoadInfo info = decoder.decode(path, sound::checkCancelled);
        if (info.getChannels() != 1 && info.getChannels() != 2) {
            int channels = info.getChannels();
            info.release();
            throw new ResourceException("sound with " + channels + " channels (mono or stereo only): " + path);
        }

        sound.setSoundLoadInfo(info);
    }


    // The path is complete (folder of the resources included)
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

        do {
            String currentNode = arrayNodes.next();

            if(node.get(currentNode) instanceof JSONObject){
                SoundLoader.loadGainNode(gainTree, node.getJSONObject(currentNode), currentNode, current);
            }
        } while(arrayNodes.hasNext());
    }
}
