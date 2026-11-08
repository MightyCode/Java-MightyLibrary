package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.scenes.Scene;

/**
 * Loads the same sound in each supported format (ogg, wav, mp3, flac) through the batch "soundformats".
 * Check with: python tools/scenario.py "scene TestSoundFormatsScene" wait-loaded "?resources SoundData"
 * (the four sounds must be loaded, none failed).
 */
public class TestSoundFormatsScene extends Scene {
    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "soundformats" };
    }

    @Override
    public void init(String[] args) {}

    @Override
    public void launch(String[] args) {}

    @Override
    public void update() {}

    @Override
    public void display() {}

    @Override
    public void unload() {}
}
