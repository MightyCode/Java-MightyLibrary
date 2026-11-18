package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.registry.Identifier;

import java.util.function.Supplier;

/**
 * Who a command belongs to: the engine (general commands), a plugin (IPlugin: the game, a mod), or a view (a scene, a
 * window view). The commands of an owner are removed together (Commands.removeOwnedBy), those of a view when it is
 * left. A command of a view runs under that view, with the OpenGL context of its window, like the code of the view.
 */
public final class CommandOwner {
    public static final CommandOwner ENGINE = new CommandOwner("engine", null, Identifier.ENGINE_NAMESPACE);

    private final String kind;
    private final IView view;
    // The namespace of the plugin (the engine's included), null for a view
    private final String plugin;

    private CommandOwner(String kind, IView view, String plugin) {
        this.kind = kind;
        this.view = view;
        this.plugin = plugin;
    }

    // The plugin of this namespace: the engine for "mightylib"
    public static CommandOwner Plugin(String namespace) {
        if (Identifier.ENGINE_NAMESPACE.equals(namespace))
            return ENGINE;
        if (!Identifier.IsNamespace(namespace))
            throw new IllegalArgumentException("Invalid namespace ->" + namespace + "<- of a plugin");

        return new CommandOwner("plugin", null, namespace);
    }

    /**
     * @param kind shown by help: "scene", "window"...
     */
    public static CommandOwner Of(IView view, String kind) {
        if (view == null)
            throw new IllegalArgumentException("A view owner needs a view");

        return new CommandOwner(kind, view, null);
    }

    public String kind() {
        return kind;
    }

    // Null for the engine and the plugins
    public IView view() {
        return view;
    }

    // The namespace of the plugin ("mightylib" for the engine), null for a view
    public String plugin() {
        return plugin;
    }

    // Main thread. The view without window (tests) only becomes the current one.
    <T> T run(Supplier<T> code) {
        if (view == null)
            return code.get();

        try (Views.Scope ignored = Views.enter(view)) {
            WindowContext window = view.context();
            if (window == null)
                return code.get();

            GLContext previous = GLContext.currentOrNull();
            window.getWindow().bindWindow();
            try {
                return code.get();
            } finally {
                if (previous != null && !previous.isDestroyed())
                    previous.makeCurrent();
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof CommandOwner))
            return false;

        CommandOwner owner = (CommandOwner) other;
        if (plugin != null)
            return plugin.equals(owner.plugin);

        return view != null && view == owner.view && kind.equals(owner.kind);
    }

    @Override
    public int hashCode() {
        if (plugin != null)
            return plugin.hashCode();

        return System.identityHashCode(view) * 31 + kind.hashCode();
    }

    // Shown by help: "engine", "plugin demo", "scene"...
    @Override
    public String toString() {
        return plugin != null && this != ENGINE ? kind + " " + plugin : kind;
    }
}
