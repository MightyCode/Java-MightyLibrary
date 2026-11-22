package io.github.mightycode.mightylib.graphics.debug;

/** Where the coordinates of a drawing of DebugDraw2D are. */
public enum EDebugSpace {
    // World units, seen through the 2D camera of the view (it moves and zooms with it)
    WORLD,
    // Pixels of the virtual size of the view, origin at its top left (it stays on screen)
    SCREEN
}
