package io.github.mightycode.mightylib.main;

/**
 * How a window is shown on its screen (DisplaySettings).
 */
public enum EWindowMode {
    // A window of the desktop, of the windowed size
    WINDOWED,
    // Exclusive fullscreen: the screen switches to the resolution of the window
    FULLSCREEN,
    // A window without border covering its whole screen, at the resolution of the screen
    BORDERLESS;

    // Case insensitive, null if unknown
    public static EWindowMode FromText(String text) {
        for (EWindowMode mode : values()) {
            if (mode.name().equalsIgnoreCase(text))
                return mode;
        }

        return null;
    }
}
