package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowContext;

import java.util.function.Supplier;

/**
 * Who a command belongs to: the engine (general commands), the game, or a view (a scene, a window view).
 * The commands of an owner are removed together (Commands.removeOwnedBy), those of a view when it is left.
 * A command of a view runs under that view, with the OpenGL context of its window, like the code of the view.
 */
public final class CommandOwner {
    public static final CommandOwner ENGINE = new CommandOwner("engine", null);
    public static final CommandOwner GAME = new CommandOwner("game", null);

    private final String kind;
    private final IView view;

    private CommandOwner(String kind, IView view) {
        this.kind = kind;
        this.view = view;
    }

    /**
     * @param kind shown by help: "scene", "window"...
     */
    public static CommandOwner Of(IView view, String kind) {
        if (view == null)
            throw new IllegalArgumentException("A view owner needs a view");

        return new CommandOwner(kind, view);
    }

    public String kind() {
        return kind;
    }

    // Null for the engine and the game
    public IView view() {
        return view;
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
        return view != null && view == owner.view && kind.equals(owner.kind);
    }

    @Override
    public int hashCode() {
        return view == null ? kind.hashCode() : System.identityHashCode(view) * 31 + kind.hashCode();
    }

    @Override
    public String toString() {
        return kind;
    }
}
