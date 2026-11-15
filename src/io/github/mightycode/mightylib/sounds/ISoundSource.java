package io.github.mightycode.mightylib.sounds;

import org.joml.Vector3f;

/**
 * A sound played by the sound service (ISoundService.createSoundSource). The setters and play/stop are valid once the
 * source is ready (after the delay of its creation info), until it is released.
 */
public interface ISoundSource {
    ISoundSource setLoop(boolean value);

    boolean isLooping();

    // Positioned relative to the listener
    ISoundSource setRelative(boolean value);

    boolean isRelative();

    ISoundSource setPosition(Vector3f position);

    ISoundSource setSpeed(Vector3f speed);

    // Own gain, multiplied by the gain of its node in the gain tree
    ISoundSource setGain(float gain);

    float getSelfGain();

    String getGainNodeName();

    // An OpenAL parameter of the source (AL_REFERENCE_DISTANCE...); ignored by a service without OpenAL
    ISoundSource setProperty(int param, float value);

    void play();

    void pause();

    void stop();

    // Stop then play again
    void reset();

    // Created by the service, not released yet
    boolean isReady();

    /**
     * Gives an owned source back to the service, which deletes it at its next update (also valid before the source is
     * ready: it is then never created). The source must not be used afterwards.
     */
    void release();

    boolean isOwned();

    String getSoundName();

    // waiting (delay not ended), released, initial, playing, paused or stopped
    String state();

    // Plays the fallback (failed or loading sound) instead of its sound; false while waiting
    boolean playsFallback();

    // Zero while the source is not ready
    Vector3f getPosition();

    // Own gain times the gain of its node (without the distance attenuation)
    float getFinalGain();

    boolean isPlaying();

    boolean inPause();

    boolean isStopped();
}
