package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.json.JSONObject;
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
public final class OpenALSoundService implements ISoundService {
    private static final long NULL = 0L;

    private final long device;
    private final long context;

    private final OpenALSoundListener listener;

    private final List<OpenALSoundSource> soundsSource;

    private final GainTree gainTree;

    // The sounds are data of this Resources
    private final Resources resources;

    private OpenALSoundService(Resources resources, JSONObject gainTree, long device, long context) {
        this.resources = resources;
        this.device = device;
        this.context = context;
        listener = new OpenALSoundListener();

        soundsSource = new ArrayList<>();

        this.gainTree = new GainTree(gainTree);
    }

    /**
     * Opens the default device.
     * @param gainTree content of the gain tree file, null for none
     * @throws IllegalStateException if no device or context can be created
     */
    public static OpenALSoundService Open(Resources resources, JSONObject gainTree) {
        long device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL)
            throw new IllegalStateException("No OpenAL device can be opened");

        long context = NULL;
        try {
            ALCCapabilities deviceCaps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            if (context == NULL)
                throw new IllegalStateException("The OpenAL context can't be created");

            alcMakeContextCurrent(context);
            AL.createCapabilities(deviceCaps);

            return new OpenALSoundService(resources, gainTree, device, context);
        } catch (RuntimeException | LinkageError e) {
            // The fallback of the key (silent) takes over: nothing stays open
            if (context != NULL) {
                alcMakeContextCurrent(NULL);
                alcDestroyContext(context);
            }
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
        safeInfo.startTimer();

        OpenALSoundSource source = new OpenALSoundSource(resources, safeInfo);
        soundsSource.add(source);

        return source;
    }

    private void removeAt(int i) {
        soundsSource.remove(i).unload();
    }

    @Override
    public void lateUpdate() {
        for (int i = soundsSource.size() - 1; i >= 0; --i) {
            OpenALSoundSource sound = soundsSource.get(i);
            SoundSourceCreationInfo info = sound.waiting;

            // Already started, or released before its delay ended (never created, deleted by the loop below)
            if (info == null || sound.isReleased())
                continue;

            info.updateTimer();
            if (!info.isTimerFinished())
                continue;

            if (!sound.init()) {
                System.err.println("--Sound source not created, no data for the sound: " + info.name);
                removeAt(i);
                continue;
            }

            sound.setPosition(info.position).setSpeed(info.speed);
            sound.setGain(info.gain, gainTree.gain(info.gainNode)).setLoop(info.loop).setRelative(info.relative);
            sound.play();
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

    // The waiting sources are never created: no OpenAL source to delete
    @Override
    public void clearAwaitedSong() {
        soundsSource.removeIf(source -> source.waiting != null);
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

        // Deleted sources: the buffers deleted afterward must not be detached from them (AL_INVALID_NAME)
        soundsSource.clear();
    }

    @Override
    public void terminate() {
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    /** An OpenAL error: an {@link io.github.mightycode.mightylib.utils.ELogLevel#ERROR} of the category sound. */
    public static void DisplayError(String action, int error) {
        Logger.Error(Logger.CATEGORY_SOUND, "Error when " + action + ": " + ErrorDescription(error));
    }

    static String ErrorDescription(int error) {
        switch (error) {
            case AL_INVALID_NAME:
                return "A bad name (ID) was passed to an OpenAL function";
            case AL_INVALID_ENUM:
                return "An invalid enum value was passed to an OpenAL function";
            case AL_INVALID_VALUE:
                return "An invalid value was passed to an OpenAL function";
            case AL_INVALID_OPERATION:
                return "The requested operation is not valid";
            case AL_OUT_OF_MEMORY:
                return "The requested operation resulted in OpenAL running out of memory";
            default:
                return "Unknown error " + error;
        }
    }
}
