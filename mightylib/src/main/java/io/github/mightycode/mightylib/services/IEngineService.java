package io.github.mightycode.mightylib.services;

/**
 * A unique system of the engine (sound, console...), created once at the start by its factory (IServiceFactory) and
 * reached by its key (ServiceKey, Engine.service). Each phase is called by the engine on the main thread; all of them do
 * nothing by default.
 * update and lateUpdate run in the order of creation; unload and terminate in the reverse order.
 */
public interface IEngineService {
    // Each tick, before the update of the contexts (the console captures the inputs and runs the typed commands)
    default void update() {}

    // Each tick, after the update of the contexts, before their postUpdate (the sound)
    default void lateUpdate() {}

    // Each frame, after the views, over the main window (bound); by layer (overlayLayer), then order of creation
    default void displayOverlay() {}

    // Higher layers are drawn over the lower ones (the console: 1000, always on top)
    default int overlayLayer() {
        return 0;
    }

    // At the stop, after the views, the main window bound, before the resources are released (GL objects, sources)
    default void unload() {}

    // At the very end, after the windows and GLFW are gone (sound device). Not "close": a service keeps that name for
    // itself (the console closes its overlay)
    default void terminate() {}
}
