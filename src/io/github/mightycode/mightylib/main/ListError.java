package io.github.mightycode.mightylib.main;

public abstract class ListError {
    public static final int NO_ERROR = 0;
    public static final int LIBRARIES_LOAD_FAIL = 2;
    public static final int WINDOW_CREATION_FAIL = 3;
    public static final int PLUGIN_REGISTRATION_FAIL = 4;
    // Strict mode: a hook of a plugin failed while the engine stopped (LifecycleHooks.hasTeardownFailed)
    public static final int LIFECYCLE_HOOK_FAIL = 5;
}
