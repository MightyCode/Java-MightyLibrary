package MightyLibrary.mightylib.sounds;

import MightyLibrary.mightylib.resources.SoundLoader;
import MightyLibrary.mightylib.util.math.KeyTree;
import MightyLibrary.mightylib.util.math.KeyTreeNode;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.sql.Types.NULL;
import static org.lwjgl.openal.ALC10.*;

public class SoundManager {
    private static SoundManager instance;

    public static SoundManager getInstance(){
        if (instance == null)
            instance = new SoundManager();

        return instance;
    }

    private long device;
    private long context;

    private final SoundListener listener;

    private final List<SoundSourceCreationInfo> awaitedNewSounds;

    private final List<SoundSource> soundsSource;

    private final KeyTree<String, Float> gainTree;

    private SoundManager (){
        listener = new SoundListener();

        awaitedNewSounds = new ArrayList<>();
        soundsSource = new ArrayList<>();

        gainTree = new KeyTree<>();
        SoundLoader.loadGainTree(gainTree);
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
        SoundSource source = new SoundSource(safeInfo.gainNode);
        safeInfo.startTimer();

        safeInfo.managerId = soundsSource.size();
        awaitedNewSounds.add(safeInfo);
        soundsSource.add(source);

        return source;
    }


    private void removeAt(int i){
        SoundSource source = soundsSource.get(i);
        source.stop();
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
            info.updateTimer();

            if (info.isTimerFinished()) {
                SoundSource sound = soundsSource.get(info.managerId);
                if (!sound.init(info.name)){
                    soundsSource.remove(info.managerId);
                    System.err.println("Fail to init sound name : " + info.name);
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
            if (source.isStopped() && source.hadPlayed()) {
                removeAt(i);
            }
        }
    }


    private float calculateGain(String name){
        float gain = 1.0f;
        KeyTreeNode<String, Float> node = gainTree.getNode(name);

        while(node != null){
            if (node.value != null)
                gain *= node.value;

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
            if (sound.getGainNodeName().equals(name)){
                sound.setGain(sound.getSelfGain(), castValue);
            }
        }

        return true;
    }

    public void clearAwaitedSong(){
        awaitedNewSounds.clear();
    }

    public void printGainTree(){
        gainTree.printTree("List of gains");
    }

    public boolean unload(){
        if (device == NULL) {
            return false;
        }

        if (context == NULL) {
            return false;
        }

        for(SoundSource sounds : soundsSource){
            sounds.stop();
            sounds.unload();
        }

        alcDestroyContext(context);
        alcCloseDevice(device);

        return true;
    }

    public SoundListener getListener(){
        return listener;
    }
}
