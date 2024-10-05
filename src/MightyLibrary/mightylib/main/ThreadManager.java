package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.utils.ThreadOrCurrent;

public class ThreadManager {
    static int NumberMaxThreadToUse = 1;

    private static int NumberThreadUsed = 1;

    public static boolean ThreadAvailable() {
        return NumberThreadUsed < NumberMaxThreadToUse;
    }

    public static boolean RunThread(ThreadOrCurrent thread){
        if (! ThreadAvailable()) {
            thread.content();
            return false;
        }

        ++NumberThreadUsed;
        thread.start();

        return true;
    }

    public static boolean ReleaseThread(ThreadOrCurrent thread){
        if (thread.IsSeparateThread()) {
            --NumberThreadUsed;
            thread.interrupt();

            return true;
        }

        return false;
    }
}
