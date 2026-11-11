package io.github.mightycode.mightylib.resources.sound;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.sounds.SoundLoadInfo;
import org.lwjgl.BufferUtils;

import java.nio.ShortBuffer;

/**
 * Fallback of the sounds: a short strident beep generated in memory, it can't fail and can't go unnoticed.
 * Played in place of a sound that failed, is still loading or has an unknown name.
 */
public class FallbackSoundData extends SoundData {
    private static final int SAMPLE_RATE = 44100;
    private static final int FREQUENCY = 2000;
    private static final float DURATION_SECONDS = 0.25f;
    private static final short AMPLITUDE = 8000;

    public FallbackSoundData() {
        super(Resources.FALLBACK_NAME, null);
    }

    @Override
    protected boolean internLoad() {
        int samples = (int) (SAMPLE_RATE * DURATION_SECONDS);
        int halfPeriod = SAMPLE_RATE / FREQUENCY / 2;
        ShortBuffer buffer = BufferUtils.createShortBuffer(samples);

        // Square wave: harsher than a sine
        for (int i = 0; i < samples; ++i)
            buffer.put((i / halfPeriod) % 2 == 0 ? AMPLITUDE : (short) -AMPLITUDE);
        buffer.flip();

        setSoundLoadInfo(new SoundLoadInfo(1, SAMPLE_RATE, buffer, false));

        return super.internLoad();
    }
}
