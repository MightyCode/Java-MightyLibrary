package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.main.WindowContext;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The hooks of a plugin on the life of the engine (EngineRegistry.lifecycle): one lambda per event, called on the main
 * thread in the order of the plugins (see LifecycleHooks).
 */
public final class LifecycleSection {
    private final Registrations registrations;
    private final String plugin;

    LifecycleSection(Registrations registrations, String plugin) {
        this.registrations = registrations;
        this.plugin = plugin;
    }

    private LifecycleHooks hooks(Object hook) {
        registrations.checkOpen();
        Objects.requireNonNull(hook, "hook");
        return registrations.lifecycle;
    }

    // The resources are loaded, the first scene is about to start
    public LifecycleSection onStarted(Runnable hook) {
        hooks(hook).addStarted(plugin, hook);
        return this;
    }

    // The new scene of a context runs: loaded, its first frame complete (each context, the main one included)
    public LifecycleSection onSceneChanged(ISceneChanged hook) {
        hooks(hook).addSceneChanged(plugin, hook);
        return this;
    }

    // The engine stops: called first, before anything is released
    public LifecycleSection onShutdown(Runnable hook) {
        hooks(hook).addShutdown(plugin, hook);
        return this;
    }

    // A window was opened: the main window of each context, and the windows of the views
    public LifecycleSection onWindowOpened(Consumer<WindowContext> hook) {
        hooks(hook).addWindowOpened(plugin, hook);
        return this;
    }

    // A window is about to be destroyed (still usable)
    public LifecycleSection onWindowClosed(Consumer<WindowContext> hook) {
        hooks(hook).addWindowClosed(plugin, hook);
        return this;
    }
}
