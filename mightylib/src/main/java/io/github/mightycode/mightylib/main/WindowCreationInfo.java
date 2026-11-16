package io.github.mightycode.mightylib.main;

import org.joml.Vector2i;

final class WindowCreationInfo {
    public DisplaySettings Display;
    // Null: the size of the window when it is created
    public Vector2i VirtualSize;
    public String WindowName;
    // Window this one belongs to (owner, centered on it), 0 for none
    public long ParentWindowId;
}
