package io.github.mightycode.mightylib.sounds;

/**
 * Where the sounds keep their samples once loaded (SoundData): given to the resources by the engine, from its sound
 * service (Resources.setSoundBuffers). Main thread only.
 */
public interface ISoundBuffers {
    // A buffer holding the samples (mono or stereo, 16 bits), -1 if it fails (reported)
    int createBuffer(SoundLoadInfo info);

    // The sources holding the buffer let it go, then it is deleted
    void deleteBuffer(int bufferId);
}
