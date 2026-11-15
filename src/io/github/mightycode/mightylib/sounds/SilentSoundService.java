package io.github.mightycode.mightylib.sounds;

import io.github.mightycode.mightylib.utils.math.KeyTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The sound without device (no OpenAL call): the sources exist and keep their settings and state, nothing is heard; the
 * gain tree is the real one. A sound that is not owned ends at once (deleted the tick after it played). Used when no
 * device can be opened, or with -Dmightylib.sound=silent.
 */
public class SilentSoundService implements ISoundService {
    private final List<SilentSoundSource> sources = new ArrayList<>();
    private final SilentSoundListener listener = new SilentSoundListener();
    private final GainTree gainTree;
    // Buffers are names only: never 0 (no buffer)
    private int lastBuffer;

    // Null file: no gain node
    public SilentSoundService(String gainTreeFile) {
        gainTree = new GainTree(gainTreeFile);
    }

    @Override
    public SilentSoundSource createSoundSource(SoundSourceCreationInfo creationInfo) {
        SoundSourceCreationInfo info = creationInfo.copy();
        info.startTimer();
        SilentSoundSource source = new SilentSoundSource(info);
        sources.add(source);
        return source;
    }

    @Override
    public void lateUpdate() {
        // The ended ones first: a sound that is not owned is seen playing during one tick
        for (int i = sources.size() - 1; i >= 0; --i) {
            SilentSoundSource source = sources.get(i);
            if (source.isReleased() || !source.isOwned() && source.hadPlayed())
                sources.remove(i);
        }

        for (SilentSoundSource source : sources) {
            if (source.waiting == null)
                continue;

            source.waiting.updateTimer();
            if (source.waiting.isTimerFinished())
                source.start(gainTree.gain(source.getGainNodeName()));
        }
    }

    @Override
    public SilentSoundListener getListener() {
        return listener;
    }

    @Override
    public List<SilentSoundSource> getSources() {
        return Collections.unmodifiableList(sources);
    }

    @Override
    public List<KeyTreeNode<String, Float>> getGainNodes() {
        return gainTree.nodes();
    }

    @Override
    public float getNodeGain(String name) {
        return gainTree.gain(name);
    }

    @Override
    public boolean changeGain(String name, float value) {
        if (!gainTree.change(name, value))
            return false;

        for (SilentSoundSource source : sources)
            source.setGain(source.getSelfGain(), gainTree.gain(source.getGainNodeName()));

        return true;
    }

    @Override
    public void printGainTree() {
        gainTree.print();
    }

    // The waiting sources are never created
    @Override
    public void clearAwaitedSong() {
        sources.removeIf(source -> source.waiting != null);
    }

    @Override
    public int createBuffer(SoundLoadInfo info) {
        return ++lastBuffer;
    }

    @Override
    public void deleteBuffer(int bufferId) {
        // Nothing holds it
    }

    @Override
    public void unload() {
        sources.clear();
    }
}
