package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;

/**
 * Decodes the sound files of some formats to 16 bit PCM. Run by a worker: no OpenAL call.
 * Added to a SoundLoader (SoundLoader.addDecoder) to support a new format.
 */
public interface SoundDecoder {
    // File extensions handled, lowercase with the dot (".ogg")
    String[] extensions();

    /**
     * Decode the whole file at this logical path, read in the sources. The result owns native memory (SoundLoadInfo.nativeBuffer, freed by release).
     * Calls checkCancelled between two steps (it throws to stop), frees what it allocated when it stops or fails,
     * and throws a ResourceException when the file is missing or invalid.
     */
    SoundLoadInfo decode(ResourceSources sources, String path, Runnable checkCancelled);
}
