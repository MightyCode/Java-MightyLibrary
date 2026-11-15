package io.github.mightycode.mightylib.main;

// Gives tests access to the package-private parts of MainThread
public final class MainThreadTestAccess {
    private MainThreadTestAccess() {}

    // The test thread plays the main thread: without it, every thread would be considered as the main one
    public static void setCurrentAsMain() {
        MainThread.setCurrentAsMain();
    }

    public static void processAll() {
        MainThread.processAll();
    }
}
