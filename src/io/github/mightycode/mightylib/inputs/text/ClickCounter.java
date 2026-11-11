package io.github.mightycode.mightylib.inputs.text;

/**
 * Counts the clicks of a double or triple click: a press at the same place (within DISTANCE pixels) less than DELAY
 * after the previous one continues the series; after a triple click a new series starts. Time given, no input read.
 */
public final class ClickCounter {
    static final float DISTANCE = 4;
    static final long DELAY_NANOSECONDS = 500_000_000L;
    private static final int MAX_CLICKS = 3;

    private int count;
    private float lastX;
    private float lastY;
    private long lastTime;

    /**
     * A press of the button.
     * @return 1 for a click, 2 for a double click, 3 for a triple click
     */
    public int press(float x, float y, long nanoTime) {
        boolean continues = count > 0 && count < MAX_CLICKS
                && nanoTime - lastTime < DELAY_NANOSECONDS
                && Math.abs(x - lastX) <= DISTANCE && Math.abs(y - lastY) <= DISTANCE;

        count = continues ? count + 1 : 1;
        lastX = x;
        lastY = y;
        lastTime = nanoTime;
        return count;
    }
}
