package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.GLElement;

/**
 * Where the batches hold the GL elements of the data they list (GLResources in the engine, none in the tests
 * without GL): a loaded batch sends them to the graphic card during the loading, a preloaded one after the rest.
 * Main thread only.
 */
public interface IGLElementSink {
    // Add a reference to the GL element of the data (created the first time); null if the data has none
    GLElement acquire(DataType data, boolean urgent);

    // Give back a reference added by acquire, with the same urgency
    void release(GLElement element, boolean urgent);
}
