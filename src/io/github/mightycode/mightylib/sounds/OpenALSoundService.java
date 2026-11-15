package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * The sound with OpenAL: the default device, one context for the process.
 */
public class OpenALSoundService implements ISoundService {
    private static final long NULL = 0L;

    private final long device;
    private final long context;

    private final OpenALSoundListener listener;

    private final List<SoundSourceCreationInfo> awaitedNewSounds;

    private final List<OpenALSoundSource> soundsSource;

    private final GainTree gainTree;

    // The sounds are data of this Resources
    private final Resources resources;

    private OpenALSoundService(Resources resources, String gainTreeFile, long device, long context) {
        this.resources = resources;
        this.device = device;
        this.context = context;
        listener = new OpenALSoundListener();

        awaitedNewSounds = new ArrayList<>();
        soundsSource = new ArrayList<>();

        gainTree = new GainTree(gainTreeFile);
    }

    /**
     * Opens the default device.
     * @param gainTreeFile path of the gain tree, null for none
     * @throws IllegalStateException if no device or context can be created
     */
    public static OpenALSoundService Open(Resources resources, String gainTreeFile) {
        long device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL)
            throw new IllegalStateException("No OpenAL device can be opened");

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        long context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            alcCloseDevice(device);
            throw new IllegalStateException("The OpenAL context can't be created");
        }

        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        try {
            return new OpenALSoundService(resources, gainTreeFile, device, context);
        } catch (RuntimeException e) {
            alcDestroyContext(context);
            alcCloseDevice(device);
            throw e;
        }
    }

    @Override
    public OpenALSoundListener getListener() {
        return listener;
    }

    @Override
    public OpenALSoundSource createSoundSource(SoundSourceCreationInfo creationInfo) {
        SoundSourceCreationInfo safeInfo = creationInfo.copy();
        OpenALSoundSource source = new OpenALSoundSource(resources, safeInfo.name, safeInfo.gainNode, safeInfo.owned);
        safeInfo.startTimer();

        safeInfo.managerId = soundsSource.size();
        awaitedNewSounds.add(safeInfo);
        soundsSource.add(source);

        return source;
    }

    private void removeAt(int i) {
        OpenALSoundSource source = soundsSource.get(i);
        source.unload();

        soundsSource.remove(i);
        updateAwaitedList(i);
    }

    private void updateAwaitedList(int id) {
        for (SoundSourceCreationInfo info : awaitedNewSounds) {
            if (info.managerId > id)
                --info.managerId;
        }
    }

    @Override
    public void lateUpdate() {
        SoundSourceCreationInfo info;
        for (int i = awaitedNewSounds.size() - 1; i >= 0; --i) {
            info = awaitedNewSounds.get(i);

            // Released before its delay ended: never created, deleted from the list below
            if (soundsSource.get(info.managerId).isReleased()) {
                awaitedNewSounds.remove(i);
                continue;
            }

            info.updateTimer();

            if (info.isTimerFinished()) {
                OpenALSoundSource sound = soundsSource.get(info.managerId);
                if (!sound.init(info.name)) {
                    soundsSource.remove(info.managerId);
                    System.err.println("Fail to initEntity sound name : " + info.name);
                }

                sound.setPosition(info.position).setSpeed(info.speed);
                sound.setGain(info.gain, gainTree.gain(info.gainNode)).setLoop(info.loop).setRelative(info.relative);
                sound.play();

                awaitedNewSounds.remove(i);
            }
        }

        OpenALSoundSource source;

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

    @Override
    public List<OpenALSoundSource> getSources() {
        return Collections.unmodifiableList(soundsSource);
    }

    @Override
    public List<KeyTreeNode<String, Float>> getGainNodes() {
        return gainTree.nodes();
    }

    @Override
    public float getNodeGain(String name) {
        return gainTree.gain(name);
    }

    @Override
    public boolean changeGain(String name, float value) {
        if (!gainTree.change(name, value))
            return false;

        for (OpenALSoundSource sound : soundsSource)
            sound.setGain(sound.getSelfGain(), gainTree.gain(sound.getGainNodeName()));

        return true;
    }

    @Override
    public void clearAwaitedSong() {
        awaitedNewSounds.clear();
    }

    @Override
    public void printGainTree() {
        gainTree.print();
    }

    @Override
    public int createBuffer(SoundLoadInfo info) {
        int bufferId = alGenBuffers();
        alBufferData(bufferId, info.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, info.getBuffer(),
                info.getSampleRate());

        int error = alGetError();
        if (error == AL_NO_ERROR)
            return bufferId;

        DisplayError("al buffer data", error);
        alDeleteBuffers(bufferId);
        return -1;
    }

    // OpenAL refuses to delete a buffer a source holds
    @Override
    public void deleteBuffer(int bufferId) {
        for (OpenALSoundSource source : soundsSource)
            source.detachBuffer(bufferId);

        alDeleteBuffers(bufferId);
        int error = alGetError();
        if (error != AL_NO_ERROR)
            DisplayError("al delete buffers", error);
    }

    // After the scenes (their unload can still stop their sources), before the sound buffers
    @Override
    public void unload() {
        for (OpenALSoundSource sound : soundsSource)
            sound.unload();

        // Deleted sources: the buffers deleted afterwards must not be detached from them (AL_INVALID_NAME)
        soundsSource.clear();
        awaitedNewSounds.clear();
    }

    @Override
    public void close() {
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    public static void DisplayError(String action, int error) {
        System.err.println("Error when " + action + "\n");
        switch (error) {
            case AL_INVALID_NAME:
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
