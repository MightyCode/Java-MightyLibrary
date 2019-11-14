package MightyLibrary.mightylib.util;

public class Timer {
    /**
     * Time of activation.
     */
    private long startTime;

    /**
     * Class constructor.
     */
    public Timer() {
        startTime = System.nanoTime();
    }

    /**
     * Reset Timer.
     */
    public void reset() {
        startTime = System.nanoTime();
    }

    /**
     * Get the time between startTime and now.
     *
     * @return startTime - now
     */
    public long getDuration() {
        return System.nanoTime() - startTime;
    }

    /**
     * Get time of activation.
     *
     * @return startTime
     */
    public long getStartTime() {
        return startTime;
    }
}