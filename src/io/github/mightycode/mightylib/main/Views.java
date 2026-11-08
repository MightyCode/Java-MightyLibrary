package io.github.mightycode.mightylib.main;

/**
 * The view being run by the current thread. The engine sets it around all the code of a view (init, launch, update,
 * display, unload, commands) and MainThread gives it back to the tasks it runs. There is no default: asking for
 * the current view where there is none (a worker, before the engine starts) throws, instead of silently using
 * another window.
 */
public final class Views {
    private static final ThreadLocal<IView> CURRENT = new ThreadLocal<>();

    private Views() {}

    /**
     * Restores the previous view when closed. Use with try-with-resources:
     * {@code try (Views.Scope ignored = Views.enter(view)) { ... }}
     */
    public static final class Scope implements AutoCloseable {
        private final IView previous;

        private Scope(IView previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            set(previous);
        }
    }

    public static Scope enter(IView view) {
        IView previous = CURRENT.get();
        set(view);
        return new Scope(previous);
    }

    private static void set(IView view) {
        if (view == null)
            CURRENT.remove();
        else
            CURRENT.set(view);
    }

    public static IView current() {
        IView view = CURRENT.get();
        if (view == null)
            throw new IllegalStateException("No current view: this code must run in the engine (main thread, "
                    + "inside a scene or a MainThread task), or be given the view explicitly");

        return view;
    }

    // Null when there is none: for the code that only passes the view along (MainThread)
    static IView currentOrNull() {
        return CURRENT.get();
    }

    public static Engine engine() {
        return current().engine();
    }
}
