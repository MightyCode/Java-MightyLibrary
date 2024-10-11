package MightyLibrary.mightylib.resources.sound;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import MightyLibrary.mightylib.sounds.SoundLoadInfo;
import MightyLibrary.mightylib.sounds.SoundManager;

import static org.lwjgl.openal.AL10.*;

public class SoundData extends SingleSourceDataType {
    private int bufferId;
    private SoundLoadInfo tempInfo;

    public SoundData(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        bufferId = -1;
    }

    public void setSoundLoadInfo(SoundLoadInfo info) {
        tempInfo = info;
    }

    @Override
    protected boolean internLoad(){
        if (bufferId != -1) {
            return false;
        }

        this.bufferId = alGenBuffers();
        alBufferData(bufferId, tempInfo.Channel == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, tempInfo.Buffer, tempInfo.SampleRate);
        tempInfo = null;

        int error = alGetError();

        if (error == AL_NO_ERROR){
            return bufferId != -1;
        }

        SoundManager.DisplayError("al buffer data (\" + path + \")\"", error);
        return false;
    }


    public int getBufferId(){
        return bufferId;
    }

    @Override
    public void internUnload() {
        if (this.bufferId != -1) {
            alDeleteBuffers(this.bufferId);
            int error = alGetError();
            if (error != AL_NO_ERROR)
                SoundManager.DisplayError("al delete buffers (" + path + ")", error);
        }
    }
}
