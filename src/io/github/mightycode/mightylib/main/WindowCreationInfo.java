package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import org.joml.Vector2i;

final class WindowCreationInfo {
    public Vector2i Size;
    public Vector2i VirtualSize;
    public String WindowName;
    public boolean Fullscreen;
    // Screen of the window, null for the primary one
    public MonitorInfo Monitor;
    public KeyboardLanguage KeyboardLanguage;
}
