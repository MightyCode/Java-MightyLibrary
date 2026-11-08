package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import io.github.mightycode.mightylib.sounds.SoundManager;

import static org.lwjgl.openal.AL10.*;

public class SoundData extends SingleSourceDataType {
    private int bufferId;
    private SoundLoadInfo tempInfo;

    public SoundData(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        bufferId = -1;
    }

    @Override
    protected DataType createReloadCopy() {
        return getClass() == SoundData.class ? new SoundData(dataName, path) : null;
    }

    // The sources holding the previous buffer let it go when it is deleted (SoundManager.DetachBuffer)
    @Override
    protected void swapContent(DataType copy) {
        SoundData other = (SoundData) copy;
        int otherBuffer = other.bufferId;
        other.bufferId = bufferId;
        bufferId = otherBuffer;
    }

    public void setSoundLoadInfo(SoundLoadInfo info) {
        // Read again before being loaded: the previous samples are not used
        if (tempInfo != null && tempInfo != info)
            tempInfo.release();

        tempInfo = info;
    }

    @Override
    protected boolean internLoad(){
        if (bufferId != -1) {
            return false;
        }

        this.bufferId = alGenBuffers();
        alBufferData(bufferId, tempInfo.Channel == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, tempInfo.Buffer, tempInfo.SampleRate);
        // OpenAL has its own copy
        tempInfo.release();
        tempInfo = null;

        int error = alGetError();

        if (error == AL_NO_ERROR){
            return bufferId != -1;
        }

        SoundManager.DisplayError("al buffer data (\" + path + \")\"", error);
        return false;
    }


    // Preloaded then no longer wanted (or failed to load): the decoded samples are freed
    @Override
    protected void internCancelPreload() {
        if (tempInfo != null) {
            tempInfo.release();
            tempInfo = null;
        }

        internUnload();
    }

    public int getBufferId(){
        return bufferId;
    }

    @Override
    public void internUnload() {
        if (this.bufferId != -1) {
            SoundManager.DetachBuffer(bufferId);
            alDeleteBuffers(this.bufferId);
            int error = alGetError();
            if (error != AL_NO_ERROR)
                SoundManager.DisplayError("al delete buffers (" + path + ")", error);
            // Loaded again by a reload
            this.bufferId = -1;
        }
    }
}
