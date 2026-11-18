package io.github.mightycode.mightylib.graphics.internal;


/**
 * GL state shared by every element of a type (placeholders of the textures, upload buffers), named by its class:
 * created at the first GLResources.shared call, deleted with GLResources. Main thread only.
 * GLResources knows no element type: what an element type needs beside its elements lives here.
 */
public interface IGLSharedState {
    // Nothing left to send (the loading queues are empty): what is only needed while sending can be freed
    default void loadingIdle() {}

    // GLResources is released: every GL object of the state is deleted
    void delete();
}
