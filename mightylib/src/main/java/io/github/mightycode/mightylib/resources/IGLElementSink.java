package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.GLElement;
import org.apiguardian.api.API;

import java.util.Map;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Where the batches hold the GL elements of the data they list (GLResources in the engine, none in the tests
 * without GL): a loaded batch sends them to the graphic card during the loading, a preloaded one after the rest.
 * Main thread only.
 */
@API(status = INTERNAL)
public interface IGLElementSink {
    // Add a reference to the GL element of the data (created the first time); null if the data has none
    GLElement acquire(DataType data, boolean urgent);

    // Give back a reference added by acquire, with the same urgency
    void release(GLElement element, boolean urgent);

    /**
     * The first loading is done, the fallback data exist (by the type of their loader): the sink creates their GL
     * elements, kept for the whole program.
     */
    default void fallbackDataLoaded(Map<Class<? extends DataType>, DataType> fallbacks) {}

    // The data was loaded again (reload, or new load after an unload): the elements that failed with it are tried again
    default void dataReloaded(DataType data) {}
}
