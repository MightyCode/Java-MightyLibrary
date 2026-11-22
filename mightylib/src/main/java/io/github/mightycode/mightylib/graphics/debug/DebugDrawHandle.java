package io.github.mightycode.mightylib.graphics.debug;

/**
 * A drawing of DebugDraw2D, given back by each of its methods: remove() drops it before the end of its lifetime (the
 * end of DebugLifetimes.untilRemoved). The drawings of one frame (DebugLifetimes.oneFrame, the default) share NONE.
 */
public final class DebugDrawHandle {
    /** The handle of the drawings that live one frame, and of those not recorded (DebugDraw2D off): remove does nothing. */
    public static final DebugDrawHandle NONE = new DebugDrawHandle(false);

    private final boolean removable;
    private boolean removed;

    private DebugDrawHandle(boolean removable) {
        this.removable = removable;
    }

    // A handle of its own, for a drawing that lives more than a frame
    static DebugDrawHandle create() {
        return new DebugDrawHandle(true);
    }

    /** The drawing is not drawn anymore (from the next frame). */
    public void remove() {
        if (removable)
            removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }
}
