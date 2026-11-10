package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;

// Actions of the engine itself, bound by default in every InputManager and seen even while the console captures the inputs
public final class EngineActions {
    // ² (the key left of 1, whatever the layout): open or close the console, at every level
    public static final InputAction TOGGLE_CONSOLE = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "toggle_console"));
    // F2: move the open console to a window of its own, or back to the main window
    public static final InputAction DETACH_CONSOLE = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "detach_console"));
    public static final InputAction RELOAD_TEXTURE = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "reload_texture"));

    private EngineActions() {}
}
