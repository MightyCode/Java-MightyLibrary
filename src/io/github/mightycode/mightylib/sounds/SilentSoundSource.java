package io.github.mightycode.mightylib.sounds;

import org.joml.Vector3f;

// A source of the silent service: keeps its settings and its state, plays nothing
public class SilentSoundSource implements ISoundSource {
    private final String soundName;
    private final String gainNodeName;
    private final boolean owned;

    // The info of its creation until its delay ends, then null
    SoundSourceCreationInfo waiting;
    private boolean released;
    private boolean hadPlayed;
    private String state = "initial";

    private boolean loop, relative;
    private final Vector3f position = new Vector3f();
    private final Vector3f speed = new Vector3f();
    private float gain = 1;
    private float nodeGain = 1;

    SilentSoundSource(SoundSourceCreationInfo info) {
        soundName = info.name;
        gainNodeName = info.gainNode;
        owned = info.owned;
        waiting = info;
    }

    // The delay ended: the settings of the info, then played
    void start(float nodeGain) {
        SoundSourceCreationInfo info = waiting;
        waiting = null;
        position.set(info.position);
        speed.set(info.speed);
        loop = info.loop;
        relative = info.relative;
        setGain(info.gain, nodeGain);
        play();
    }

    boolean isReleased() {
        return released;
    }

    boolean hadPlayed() {
        return hadPlayed;
    }

    void setGain(float gain, float nodeGain) {
        this.gain = gain;
        this.nodeGain = nodeGain;
    }

    @Override
    public SilentSoundSource setLoop(boolean value) {
        loop = value;
        return this;
    }

    @Override
    public boolean isLooping() {
        return loop;
    }

    @Override
    public SilentSoundSource setRelative(boolean value) {
        relative = value;
        return this;
    }

    @Override
    public boolean isRelative() {
        return relative;
    }

    @Override
    public SilentSoundSource setPosition(Vector3f position) {
        this.position.set(position);
        return this;
    }

    @Override
    public SilentSoundSource setSpeed(Vector3f speed) {
        this.speed.set(speed);
        return this;
    }

    @Override
    public SilentSoundSource setGain(float gain) {
        this.gain = gain;
        return this;
    }

    @Override
    public float getSelfGain() {
        return gain;
    }

    @Override
    public String getGainNodeName() {
        return gainNodeName;
    }

    @Override
    public SilentSoundSource setProperty(int param, float value) {
        return this;
    }

    @Override
    public void play() {
        hadPlayed = true;
        state = "playing";
    }

    @Override
    public void pause() {
        if (state.equals("playing"))
            state = "paused";
    }

    @Override
    public void stop() {
        if (hadPlayed)
            state = "stopped";
    }

    @Override
    public void reset() {
        stop();
        play();
    }

    @Override
    public boolean isReady() {
        return waiting == null && !released;
    }

    @Override
    public void release() {
        if (isReady())
            stop();
        released = true;
    }

    @Override
    public boolean isOwned() {
        return owned;
    }

    @Override
    public String getSoundName() {
        return soundName;
    }

    @Override
    public String state() {
        if (released)
            return "released";

        return waiting != null ? "waiting" : state;
    }

    @Override
    public boolean playsFallback() {
        return false;
    }

    @Override
    public Vector3f getPosition() {
        return isReady() ? new Vector3f(position) : new Vector3f();
    }

    @Override
    public float getFinalGain() {
        return gain * nodeGain;
    }

    @Override
    public boolean isPlaying() {
        return state().equals("playing");
    }

    @Override
    public boolean inPause() {
        return state().equals("paused");
    }

    @Override
    public boolean isStopped() {
        return state().equals("stopped");
    }
}
