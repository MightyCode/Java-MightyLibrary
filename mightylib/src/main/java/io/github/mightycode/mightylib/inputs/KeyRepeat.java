package io.github.mightycode.mightylib.inputs;

/**
 * Repeats an action while its key is held, like the keyboard of the system does for typing: a step at the press,
 * then after a delay one step at each interval. No GL, no clock: the caller gives the time of the tick.
 * The defaults are the ones of the keyboard settings of Windows (about 0.5 s, then about 30 steps a second).
 *
 * A key already held when the repeat could begin (the focus came back with the key down) does not repeat: only a
 * press starts it.
 */
public final class KeyRepeat {
    public static final float DEFAULT_DELAY = 0.5f;
    public static final float DEFAULT_INTERVAL = 1f / 30f;

    private float delay;
    private float interval;

    private boolean active;
    // Time before the next step
    private float timer;

    public KeyRepeat() {
        this(DEFAULT_DELAY, DEFAULT_INTERVAL);
    }

    public KeyRepeat(float delay, float interval) {
        setRepeat(delay, interval);
    }

    /**
     * @param delay    seconds between the press and the first repeated step
     * @param interval seconds between two repeated steps, more than 0
     */
    public void setRepeat(float delay, float interval) {
        if (delay < 0 || interval <= 0)
            throw new IllegalArgumentException("A repeat needs a delay of 0 or more and an interval above 0");

        this.delay = delay;
        this.interval = interval;
    }

    public float getDelay() { return delay; }

    public float getInterval() { return interval; }

    /**
     * To call at each tick.
     * @param pressed the key went down at this tick
     * @param held    the key is down
     * @param seconds duration of the tick
     * @return the steps to do at this tick: 1 at the press, then 0 or more (a slow tick can owe several)
     */
    public int update(boolean pressed, boolean held, float seconds) {
        if (!held) {
            active = false;
            return pressed ? 1 : 0;
        }

        if (pressed) {
            active = true;
            timer = delay;
            return 1;
        }

        if (!active)
            return 0;

        int steps = 0;
        timer -= seconds;
        while (timer <= 0) {
            ++steps;
            timer += interval;
        }

        return steps;
    }

    /** Forgets the key being held (the scene changed, the focus left). */
    public void reset() {
        active = false;
    }
}
