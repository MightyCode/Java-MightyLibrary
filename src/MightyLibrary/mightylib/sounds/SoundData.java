package MightyLibrary.mightylib.sounds;

import MightyLibrary.mightylib.resources.DataType;

import static org.lwjgl.openal.AL10.*;

public class SoundData extends DataType {

    private final static String PATH = "resources/sounds/";

    private int bufferId;

    public SoundData(String dataName, String path) {
        super(dataName, PATH + path);

        bufferId = -1;
    }


    public boolean createSound(SoundLoadInfo info){
        if (bufferId != -1)
            return false;

        this.bufferId = alGenBuffers();
        alBufferData(bufferId, info.Channel == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, info.Buffer, info.SampleRate);

        return true;
    }


    public int getBufferId(){
        return bufferId;
    }

    @Override
    public boolean unload() {
        if (this.bufferId != -1)
            alDeleteBuffers(this.bufferId);

        this.bufferId = -1;

        return false;
    }
}
