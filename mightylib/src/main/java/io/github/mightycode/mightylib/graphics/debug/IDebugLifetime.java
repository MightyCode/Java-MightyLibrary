package io.github.mightycode.mightylib.graphics.debug;

/**
 * How long a drawing of DebugDraw2D lives (DebugLifetimes gives the usual ones). A drawing whose handle was removed is
 * dropped without asking it.
 */
public interface IDebugLifetime {
    /**
     * Hook called once per frame, before the drawings of the view are drawn, from the frame the drawing was asked:
     * false drops it (not drawn, never asked again). Main thread.
     */
    boolean alive();

    /** Hook: its name for the command debugdraw list (the drawings are counted by name). */
    default String name() {
        return getClass().getSimpleName();
    }
}
