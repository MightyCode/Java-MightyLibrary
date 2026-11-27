package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.sounds.ISoundBuffers;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;

import io.github.mightycode.mightylib.utils.Logger;

import java.util.Locale;

public class SoundData extends SingleSourceDataType {
    private int bufferId;
    private SoundLoadInfo tempInfo;
    // Size of the samples once decoded (kept by OpenAL after the loading), 0 until the file is read
    private long decodedBytes;

    public SoundData(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        bufferId = -1;
    }

    @Override
    protected DataType createReloadCopy() {
        return getClass() == SoundData.class ? new SoundData(getDataName(), path()) : null;
    }

    // The sources holding the previous buffer let it go when it is deleted (ISoundBuffers.deleteBuffer)
    @Override
    protected void swapContent(DataType copy) {
        SoundData other = (SoundData) copy;
        int otherBuffer = other.bufferId;
        other.bufferId = bufferId;
        bufferId = otherBuffer;

        long otherDecoded = other.decodedBytes;
        other.decodedBytes = decodedBytes;
        decodedBytes = otherDecoded;
    }

    /**
     * The decoded samples (16 bits) once read; before, a guess from the file: a WAV is already raw,
     * a FLAC (lossless) about twice smaller, an OGG or MP3 about ten times smaller.
     */
    @Override
    public long estimatedCpuBytes() {
        if (decodedBytes > 0)
            return decodedBytes;

        return super.estimatedCpuBytes() * CompressionRatio(path());
    }

    static long CompressionRatio(String path) {
        if (path == null)
            return 1;

        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ogg") || lower.endsWith(".mp3"))
            return 10;
        if (lower.endsWith(".flac"))
            return 2;

        return 1;
    }

    public void setSoundLoadInfo(SoundLoadInfo info) {
        // Read again before being loaded: the previous samples are not used
        if (tempInfo != null && tempInfo != info)
            tempInfo.release();

        tempInfo = info;
        if (info != null && info.getBuffer() != null)
            decodedBytes = (long) info.getBuffer().capacity() * Short.BYTES;
    }

    @Override
    protected boolean internLoad(){
        if (bufferId != -1) {
            return false;
        }

        ISoundBuffers buffers = resources().soundBuffers();
        if (buffers == null) {
            Logger.Warn(Logger.CATEGORY_SOUND, "No sound service to load the sound " + path());
            return false;
        }

        this.bufferId = buffers.createBuffer(tempInfo);
        // The sound service has its own copy
        tempInfo.release();
        tempInfo = null;

        if (bufferId == -1)
            Logger.Warn(Logger.CATEGORY_SOUND, "The buffer of the sound " + path() + " can't be created");
        return bufferId != -1;
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
            ISoundBuffers buffers = resources().soundBuffers();
            if (buffers != null)
                buffers.deleteBuffer(bufferId);
            // Loaded again by a reload
            this.bufferId = -1;
        }
    }
}
