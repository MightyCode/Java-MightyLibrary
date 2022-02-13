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
        SoundSource source = new SoundSource();
        creationInfo.startTimer();

        awaitedNewSounds.add(creationInfo);
        creationInfo.managerId = soundsSource.size();
        soundsSource.add(source);

        return source;
    }

    public void remove(SoundSource source){
        source.stop();
        source.unload();

        soundsSource.remove(source);
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
    }

    private float calculateGain(String name){
        float gain = 1.0f;
        KeyTreeNode<String, Float> node = gainTree.getNode(name);

        while(node != null){
            if (node.value != null)
                gain *= node.value;

            node = node.parent();
        }

        return gain;
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

    public SoundListener getListener(){
        return listener;
    }
}
