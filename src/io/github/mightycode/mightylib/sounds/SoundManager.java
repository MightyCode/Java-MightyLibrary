package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.sound.SoundLoader;
import io.github.mightycode.mightylib.utils.math.KeyTree;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.sql.Types.NULL;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class SoundManager {
    private long device;
    private long context;

    private final SoundListener listener;


    public SoundListener getListener(){
        return listener;
    }

    private final List<SoundSourceCreationInfo> awaitedNewSounds;

    private final List<SoundSource> soundsSource;

    private final KeyTree<String, Float> gainTree;

    // The sounds are data of this Resources
    private final Resources resources;

    public SoundManager(Resources resources, String gainPath){
        this.resources = resources;
        resources.setSoundBufferDetacher(this::detachBuffer);
        listener = new SoundListener();

        awaitedNewSounds = new ArrayList<>();
        soundsSource = new ArrayList<>();

        gainTree = new KeyTree<>();
        // No gain tree: every sound keeps a gain of 1
        if (gainPath != null)
            SoundLoader.loadGainTree(resources.folder() + gainPath, gainTree);
    }


    public boolean init(){
        this.device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            return false;
        }

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        this.context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            return false;
        }

        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        return true;
    }


    public SoundSource createSoundSource(SoundSourceCreationInfo creationInfo){
        SoundSourceCreationInfo safeInfo = creationInfo.copy();
        SoundSource source = new SoundSource(resources, safeInfo.name, safeInfo.gainNode, safeInfo.owned);
        safeInfo.startTimer();

        safeInfo.managerId = soundsSource.size();
        awaitedNewSounds.add(safeInfo);
        soundsSource.add(source);

        return source;
    }

    private void removeAt(int i){
        SoundSource source = soundsSource.get(i);
        source.unload();

        soundsSource.remove(i);
        updateAwaitedList(i);
    }


    public void updateAwaitedList(int id){
        for (SoundSourceCreationInfo info : awaitedNewSounds){
            if (info.managerId > id)
                --info.managerId;
        }
    }


    public void lateUpdate(){
        SoundSourceCreationInfo info;
        for (int i = awaitedNewSounds.size() - 1; i >= 0; --i){
            info = awaitedNewSounds.get(i);

            // Released before its delay ended: never created, deleted from the list below
            if (soundsSource.get(info.managerId).isReleased()) {
                awaitedNewSounds.remove(i);
                continue;
            }

            info.updateTimer();

            if (info.isTimerFinished()) {
                SoundSource sound = soundsSource.get(info.managerId);
                if (!sound.init(info.name)){
                    soundsSource.remove(info.managerId);
                    System.err.println("Fail to initEntity sound name : " + info.name);
                }

                sound.setPosition(info.position).setSpeed(info.speed).setGain(info.gain, calculateGain(info.gainNode))
                                .setLoop(info.loop).setRelative(info.relative);
                sound.play();

                awaitedNewSounds.remove(i);
            }
        }

        SoundSource source;

        for (int i = soundsSource.size() - 1; i >= 0; --i) {
            source = soundsSource.get(i);

            // An owned source lives until released; the others until they stop (hadPlayed first: a source still
            // waiting for its delay has no OpenAL name to query)
            if (source.isReleased() || (!source.isOwned() && source.hadPlayed() && source.isStopped()))
                removeAt(i);
            else
                source.resumeIfLoaded();
        }
    }


    // Read-only view, for inspection (command sounds)
    public List<SoundSource> getSources(){
        return Collections.unmodifiableList(soundsSource);
    }

    public List<KeyTreeNode<String, Float>> getGainNodes(){
        return gainTree.nodes();
    }

    // Product of the values of the node and its ancestors (1 for an unknown node)
    public float getNodeGain(String name){
        return calculateGain(name);
    }

    private float calculateGain(String name){
        float gain = 1.0f;
        KeyTreeNode<String, Float> node = gainTree.getNode(name);

        while(node != null){
            if (node.getValue() != null)
                gain *= node.getValue();

            //System.out.println(node.getKey());
            node = node.parent();
        }

        return gain;
    }


    public boolean changeGain(String name, float newValue){
        KeyTreeNode<String, Float> node = gainTree.getNode(name);

        float castValue = (newValue > 1) ? 1 : (newValue < 0) ? 0 : newValue;
        if (node == null || castValue == node.getValue())
            return false;

        node.setValue(castValue);

        for (SoundSource sound : soundsSource){
            sound.setGain(sound.getSelfGain(), calculateGain(sound.getGainNodeName()));
        }

        return true;
    }

    public void clearAwaitedSong(){
        awaitedNewSounds.clear();
    }

    public void printGainTree(){
        gainTree.printTree("List of gains");
    }

    // Before deleting a buffer: the sources still holding it let it go
    private void detachBuffer(int bufferId) {
        for (SoundSource source : soundsSource)
            source.detachBuffer(bufferId);
    }

    public boolean unloadSoundSource(){
        if (device == NULL) {
            return false;
        }

        if (context == NULL) {
            return false;
        }

        for(SoundSource sounds : soundsSource){
            sounds.unload();
        }
        // Deleted sources: the buffers deleted afterwards must not be detached from them (AL_INVALID_NAME)
        soundsSource.clear();
        awaitedNewSounds.clear();

        return true;
    }

    public boolean unload(){
        if (device == NULL) {
            return false;
        }

        if (context == NULL) {
            return false;
        }

        alcDestroyContext(context);
        alcCloseDevice(device);

        return true;
    }

    public static void DisplayError(String action, int error){
        System.err.println("Error when " + action  + "\n");
        switch (error){
            case AL_INVALID_NAME :
                System.err.println("A bad name (ID) was passed to an OpenAL function");
                break;
            case AL_INVALID_ENUM:
                System.err.println("An invalid enum value was passed to an OpenAL function");
                break;
            case AL_INVALID_VALUE:
                System.err.println("An invalid value was passed to an OpenAL function");
                break;
            case AL_INVALID_OPERATION:
                System.err.println("The requested operation is not valid");
                break;
            case AL_OUT_OF_MEMORY:
                System.err.println("The requested operation resulted in OpenAL running out of memory");
                break;
            default:
                System.out.println("UNKNOWN ERROR");
                break;
        }
    }
}
