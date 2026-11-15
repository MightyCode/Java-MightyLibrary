package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;

import java.util.List;

/**
 * The sound of the engine (service mightylib:sound, Engine.sound): sources, listener and gain tree. OpenAL by default
 * (OpenALSoundService), silent when no device can be opened or with -Dmightylib.sound=silent (SilentSoundService).
 * Phases: lateUpdate creates the waiting sources and deletes the ended ones, unload deletes the sources, terminate
 * the device.
 */
public interface ISoundService extends IEngineService, ISoundBuffers {
    /**
     * A source playing the sound of the info, once its delay ended (ISoundSource.isReady). Not owned: deleted when it
     * stops; owned: deleted once released.
     */
    ISoundSource createSoundSource(SoundSourceCreationInfo creationInfo);

    ISoundListener getListener();

    // Read-only, for inspection (command sounds)
    List<? extends ISoundSource> getSources();

    List<KeyTreeNode<String, Float>> getGainNodes();

    // Product of the values of the node and its ancestors (1 for an unknown node)
    float getNodeGain(String name);

    // New value of a node (clamped to [0, 1]), applied to the sources; false if the node is unknown or unchanged
    boolean changeGain(String name, float value);

    // The gain tree in the program output
    void printGainTree();

    // At a scene change: the sounds still waiting for their delay are forgotten
    void clearAwaitedSong();
}
