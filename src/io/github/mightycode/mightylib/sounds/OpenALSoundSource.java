package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.sound.SoundData;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

import static org.lwjgl.openal.AL10.alGenSources;

// A source of the OpenAL service: created by OpenALSoundService.lateUpdate once its delay ended
public class OpenALSoundSource implements ISoundSource {
    private SoundData soundData;
    private int sourceId;
    // Buffer given to the source: the sound, or the fallback while the sound isn't loaded (0 = none)
    private final Resources resources;
    private int boundBufferId;
    private boolean loop, relative;

    private float gain;
    private final String gainNodeName;
    private float gainNode;

    private boolean hadPlayed;

    // Owned: deleted only once released (SoundSourceCreationInfo.owned)
    private final boolean owned;
    private boolean released;
    // Owned source stopped while playing because its buffer was deleted (reload): played again once loaded
    private boolean resumeWhenLoaded;
    // Settings applied when the delay ends (the OpenAL source is created then); null once started
    SoundSourceCreationInfo waiting;

    // Name asked at creation, known before the source is created
    private String soundName;

    OpenALSoundSource(Resources resources, SoundSourceCreationInfo info) {
        this.waiting = info;
        this.soundName = info.name;
        this.owned = info.owned;
        this.resources = resources;
        this.soundData = null;
        this.sourceId  = -1;
        this.gain = 1;
        this.gainNodeName = info.gainNode;
        this.gainNode = 1;
        this.loop = false;
        this.relative = false;
        this.hadPlayed = false;
    }

    // The delay ended: the OpenAL source is created; false if the sound has no data (the caller deletes the source)
    boolean init(){
        waiting = null;
        this.sourceId = alGenSources();

        return setSoundData(soundName);
    }

    private boolean setSoundData(String soundName) {
        stop();

        // Unknown name: the fallback sound
        this.soundData = resources.getResourceOrFallback(SoundData.class, soundName);
        if (soundData == null)
            return false;

        bindUsableBuffer();
        return true;
    }

    // The sound if loaded, otherwise the fallback (failed or still loading): checked again at each play
    private void bindUsableBuffer() {
        SoundData usable = resources.usable(SoundData.class, soundData);
        int bufferId = usable == null || !usable.isLoaded() ? 0 : usable.getBufferId();

        if (bufferId != boundBufferId) {
            AL10.alSourceStop(sourceId);
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
            boundBufferId = bufferId;
        }
    }


    // The buffer will be deleted (reloaded or unloaded sound): OpenAL refuses while a source holds it
    void detachBuffer(int bufferId) {
        if (bufferId != boundBufferId || bufferId == 0)
            return;

        boolean wasPlaying = isPlaying();
        AL10.alSourceStop(sourceId);
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, 0);
        boundBufferId = 0;

        resumeWhenLoaded = owned && !released && wasPlaying;
    }

    // Called by the manager at each tick: a reloaded sound plays again in its owned source
    void resumeIfLoaded() {
        if (!resumeWhenLoaded || soundData == null || !soundData.isLoaded())
            return;

        resumeWhenLoaded = false;
        play();
    }

    @Override
    public OpenALSoundSource setLoop(boolean value){
        this.loop = value;

        if (loop){
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_TRUE);
        } else {
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
        }

        return this;
    }

    @Override
    public boolean isLooping(){
        return this.loop;
    }

    @Override
    public OpenALSoundSource setRelative(boolean value){
        this.relative = value;

        if (relative) {
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        } else {
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        }

        return this;
    }

    @Override
    public boolean isRelative(){
        return this.relative;
    }

    @Override
    public OpenALSoundSource setPosition(Vector3f position) {
        AL10.alSource3f(sourceId, AL10.AL_POSITION, position.x, position.y, position.z);

        return this;
    }

    @Override
    public OpenALSoundSource setSpeed(Vector3f speed) {
        AL10.alSource3f(sourceId, AL10.AL_VELOCITY, speed.x, speed.y, speed.z);

        return this;
    }

    @Override
    public OpenALSoundSource setGain(float gain) {
        return setGain(gain, gainNode);
    }

    OpenALSoundSource setGain(float gain, float gainNode) {
        this.gainNode = gainNode;
        this.gain = gain;

        AL10.alSourcef(sourceId, AL10.AL_GAIN, gain * gainNode);

        return this;
    }

    @Override
    public float getSelfGain(){
        return gain;
    }

    @Override
    public String getGainNodeName(){
        return gainNodeName;
    }

    @Override
    public OpenALSoundSource setProperty(int param, float value) {
        AL10.alSourcef(sourceId, param, value);

        return this;
    }

    @Override
    public void play() {
        hadPlayed = true;

        if (soundData != null)
            bindUsableBuffer();
        AL10.alSourcePlay(sourceId);
    }

    /*
     * True once the manager has created the OpenAL source (after the delay of its creation info), until the source
     * is released or deleted: the setters and play/stop are only valid meanwhile.
     */
    @Override
    public boolean isReady() {
        return sourceId != -1 && !released;
    }

    /**
     * Gives an owned source back to the manager, which deletes it at its next update (also valid before the source
     * is ready: it is then never created). The source must not be used afterwards.
     */
    @Override
    public void release() {
        if (isReady())
            stop();
        released = true;
        resumeWhenLoaded = false;
    }

    boolean isReleased() {
        return released;
    }

    @Override
    public boolean isOwned() {
        return owned;
    }

    @Override
    public String getSoundName() {
        return soundName;
    }

    // waiting (delay not ended), released, or the OpenAL state: initial, playing, paused, stopped
    @Override
    public String state() {
        if (released)
            return "released";
        if (sourceId == -1)
            return "waiting";

        switch (AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE)) {
            case AL10.AL_PLAYING:
                return "playing";
            case AL10.AL_PAUSED:
                return "paused";
            case AL10.AL_STOPPED:
                return "stopped";
            default:
                return "initial";
        }
    }

    // The buffer given to OpenAL is not the one of the sound: fallback (failed, loading) or none; false while waiting
    @Override
    public boolean playsFallback() {
        if (sourceId == -1)
            return false;

        return soundData == null || boundBufferId == 0 || boundBufferId != soundData.getBufferId();
    }

    // Read from OpenAL; zero while the source is not ready
    @Override
    public Vector3f getPosition() {
        if (!isReady())
            return new Vector3f();

        float[] x = new float[1];
        float[] y = new float[1];
        float[] z = new float[1];
        AL10.alGetSource3f(sourceId, AL10.AL_POSITION, x, y, z);
        return new Vector3f(x[0], y[0], z[0]);
    }

    // Own gain times the gain of its node in the gain tree (without the distance attenuation)
    @Override
    public float getFinalGain() {
        return gain * gainNode;
    }

    boolean hadPlayed(){
        return this.hadPlayed;
    }

    @Override
    public boolean isPlaying() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    @Override
    public boolean inPause() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PAUSED;
    }

    @Override
    public boolean isStopped() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED;
    }

    @Override
    public void pause() {
        AL10.alSourcePause(sourceId);
    }

    @Override
    public void stop() {
        AL10.alSourceStop(sourceId);
    }

    @Override
    public void reset(){
        stop();
        play();
    }

    void unload() {
        // Never created (released or cleared before its delay ended)
        if (sourceId == -1)
            return;

        stop();
        AL10.alDeleteSources(sourceId);
        sourceId = -1;
    }
}
