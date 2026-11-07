package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.utils.ThreadOrCurrent;

public class ThreadManager {
    static int NumberMaxThreadToUse = 1;

    private static int NumberThreadUsed = 1;

    public static synchronized boolean ThreadAvailable() {
        return NumberThreadUsed < NumberMaxThreadToUse;
    }

    // Check and reserve atomically, so two callers can't take the last available thread
    private static synchronized boolean TryReserveThread() {
        if (!ThreadAvailable())
            return false;

        ++NumberThreadUsed;
        return true;
    }

    private static synchronized void FreeThread() {
        --NumberThreadUsed;
    }

    public static boolean RunThread(ThreadOrCurrent thread){
        if (!TryReserveThread()) {
            // Run in the current thread, outside the lock
            thread.content();
            return false;
        }

        thread.start();

        return true;
    }

    public static boolean ReleaseThread(ThreadOrCurrent thread){
        if (thread.IsSeparateThread()) {
            FreeThread();
            thread.interrupt();

            return true;
        }

        return false;
    }
}
