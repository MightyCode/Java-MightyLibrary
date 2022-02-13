package MightyLibrary.mightylib.sounds;

import MightyLibrary.mightylib.resources.Resources;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

import static org.lwjgl.openal.AL10.alGenSources;

public class SoundSource {
    private SoundData soundData;
    private int sourceId;
    private boolean loop, relative;

    SoundSource() {}

    public boolean init(String soundName){
        this.sourceId = alGenSources();

        return setSoundData(soundName);
    }

    private boolean setSoundData(String soundName) {
        stop();

        this.soundData = Resources.getInstance().getResource(SoundData.class, soundName);
        if (soundData == null)
            return false;


        AL10.alSourcei(sourceId, AL10.AL_BUFFER, soundData.getBufferId());

        return true;
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
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
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
        AL10.alSourcef(sourceId, AL10.AL_GAIN, gain);

        return this;
    }

    public SoundSource setProperty(int param, float value) {
        AL10.alSourcef(sourceId, param, value);

        return this;
    }

    public void play() {
        AL10.alSourcePlay(sourceId);
    }

    public boolean isPlaying() {
        return AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    public void pause() {
        AL10.alSourcePause(sourceId);
    }

    public void stop() {
        AL10.alSourceStop(sourceId);
    }

    public void unload() {
        stop();
        AL10.alDeleteSources(sourceId);
    }
}
