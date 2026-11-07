package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.sound.SoundData;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

import static org.lwjgl.openal.AL10.alGenSources;

public class SoundSource {
    private SoundData soundData;
    private int sourceId;
    // Buffer given to the source: the sound, or the fallback while the sound isn't loaded (0 = none)
    private int boundBufferId;
    private boolean loop, relative;

    private float gain;
    private final String gainNodeName;
    private float gainNode;

    private boolean hadPlayed;

    SoundSource(String gainNode) {
        this.soundData = null;
        this.sourceId  = -1;
        this.gain = 1;
        this.gainNodeName = gainNode;
        this.gainNode = 1;
        this.loop = false;
        this.relative = false;
        this.hadPlayed = false;
    }

    public boolean init(String soundName){
        this.sourceId = alGenSources();

        return setSoundData(soundName);
    }

    private boolean setSoundData(String soundName) {
        stop();

        // Unknown name: the fallback sound
        this.soundData = Resources.getInstance().getResourceOrFallback(SoundData.class, soundName);
        if (soundData == null)
            return false;

        bindUsableBuffer();
        return true;
    }

    // The sound if loaded, otherwise the fallback (failed or still loading): checked again at each play
    private void bindUsableBuffer() {
        SoundData usable = Resources.getInstance().usable(SoundData.class, soundData);
        int bufferId = usable == null || !usable.isLoaded() ? 0 : usable.getBufferId();

        if (bufferId != boundBufferId) {
            AL10.alSourceStop(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
            boundBufferId = bufferId;
        }
    }


    public SoundSource setLoop(boolean value){
        this.loop = value;

        if (loop){
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_TRUE);
        } else {
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
        }

        return this;
    }

    public boolean isLooping(){
        return this.loop;
    }

    public SoundSource setRelative(boolean value){
        this.relative = value;

        if (relative) {
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        } else {
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        }

        return this;
    }

    public boolean isRelative(){
        return this.relative;
    }

    public SoundSource setPosition(Vector3f position) {
        AL10.alSource3f(sourceId, AL10.AL_POSITION, position.x, position.y, position.z);

        return this;
    }

    public SoundSource setSpeed(Vector3f speed) {
        AL10.alSource3f(sourceId, AL10.AL_VELOCITY, speed.x, speed.y, speed.z);

        return this;
    }

    public SoundSource setGain(float gain) {
        return setGain(gain, gainNode);
    }

    SoundSource setGain(float gain, float gainNode) {
        this.gainNode = gainNode;
        this.gain = gain;

        AL10.alSourcef(sourceId, AL10.AL_GAIN, gain * gainNode);

        return this;
    }

    public float getSelfGain(){
        return gain;
    }

    String getGainNodeName(){
        return gainNodeName;
    }

    public SoundSource setProperty(int param, float value) {
        AL10.alSourcef(sourceId, param, value);

        return this;
    }

    public void play() {
        hadPlayed = true;

        if (soundData != null)
            bindUsableBuffer();
        AL10.alSourcePlay(sourceId);
    }

    boolean hadPlayed(){
        return this.hadPlayed;
    }

    public boolean isPlaying() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    public boolean inPause() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED;
    }

    public boolean isStopped() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED;
    }

    public void pause() {
        AL10.alSourcePause(sourceId);
    }

    public void stop() {
        AL10.alSourceStop(sourceId);
    }

    public void reset(){
        stop();
        play();
    }

    void unload() {
        stop();
        AL10.alDeleteSources(sourceId);
    }
}
