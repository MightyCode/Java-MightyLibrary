package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The hooks of the plugins on the life of the engine (EngineRegistry.lifecycle), called by the engine on the main
 * thread, in the order of the plugins. A hook that throws is reported (Logger.LogException) and the next ones are
 * called; in strict mode (EngineConfig.setStrictResources) the exception is thrown again.
 */
public final class LifecycleHooks {
    private static final class Hook<T> {
        private final String plugin;
        private final T callback;

        private Hook(String plugin, T callback) {
            this.plugin = plugin;
            this.callback = callback;
        }
    }

    private final List<Hook<Runnable>> started = new ArrayList<>();
    private final List<Hook<ISceneChanged>> sceneChanged = new ArrayList<>();
    private final List<Hook<Runnable>> shutdown = new ArrayList<>();
    private final List<Hook<Consumer<WindowContext>>> windowOpened = new ArrayList<>();
    private final List<Hook<Consumer<WindowContext>>> windowClosed = new ArrayList<>();
    private boolean strict;

    // ---- Registration (LifecycleSection) ----

    void addStarted(String plugin, Runnable hook) { started.add(new Hook<>(plugin, hook)); }
    void addSceneChanged(String plugin, ISceneChanged hook) { sceneChanged.add(new Hook<>(plugin, hook)); }
    void addShutdown(String plugin, Runnable hook) { shutdown.add(new Hook<>(plugin, hook)); }
    void addWindowOpened(String plugin, Consumer<WindowContext> hook) { windowOpened.add(new Hook<>(plugin, hook)); }
    void addWindowClosed(String plugin, Consumer<WindowContext> hook) { windowClosed.add(new Hook<>(plugin, hook)); }

    // A hook that throws stops the engine (the tests, tools/scenario.py --strict)
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    // ---- Called by the engine ----

    // The resources are loaded, the first scene is about to start
    public void started() {
        call("started", started, Runnable::run);
    }

    // The new scene of the context runs: loaded, its first frame complete
    public void sceneChanged(Context context, Scene previous, Scene next) {
        call("sceneChanged", sceneChanged, hook -> hook.sceneChanged(context, previous, next));
    }

    // The engine stops, nothing is released yet
    public void shutdown() {
        call("shutdown", shutdown, Runnable::run);
    }

    // A window was opened: the main window of a context or a window of a view
    public void windowOpened(WindowContext window) {
        call("windowOpened", windowOpened, hook -> hook.accept(window));
    }

    // A window is about to be destroyed
    public void windowClosed(WindowContext window) {
        call("windowClosed", windowClosed, hook -> hook.accept(window));
    }

    private <T> void call(String event, List<Hook<T>> hooks, Consumer<T> call) {
        for (Hook<T> hook : hooks) {
            try {
                call.accept(hook.callback);
            } catch (RuntimeException e) {
                if (strict)
                    throw e;

                Logger.LogException("--The hook " + event + " of the plugin " + hook.plugin + " failed", e);
            }
        }
    }
}
