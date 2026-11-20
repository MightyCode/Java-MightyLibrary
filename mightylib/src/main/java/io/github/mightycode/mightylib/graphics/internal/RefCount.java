package io.github.mightycode.mightylib.graphics.internal;

/**
 * References to an object shared by its users (a Mesh, a Material): each user retains it once and releases it when
 * done; the last release frees it. An object that its creator keeps beyond its users is retained by the creator too.
 * Freed once: retaining it again is an error. Without OpenGL; main thread only.
 */
public final class RefCount {
    private final String name;
    private final Runnable free;
    private int references;
    private boolean freed;

    /**
     * @param name what is counted, for the messages
     * @param free called once, by the release that brings the references to zero
     */
    public RefCount(String name, Runnable free) {
        this.name = name;
        this.free = free;
    }

    public void retain() {
        if (freed)
            throw new IllegalStateException(name + " already freed (its last reference was released)");

        ++references;
    }

    /**
     * @return true when this release freed the object
     * @throws IllegalStateException when not retained
     */
    public boolean release() {
        if (references <= 0)
            throw new IllegalStateException(name + " released more times than retained");

        --references;
        if (references > 0)
            return false;

        freed = true;
        free.run();
        return true;
    }

    public int references() {
        return references;
    }

    public boolean isFreed() {
        return freed;
    }
}
