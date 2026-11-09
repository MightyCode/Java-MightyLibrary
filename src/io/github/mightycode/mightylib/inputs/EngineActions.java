package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;

// Actions of the engine itself, bound by default in every InputManager
public final class EngineActions {
    public static final InputAction OPEN_COMMAND = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "open_command"));
    public static final InputAction RELOAD_TEXTURE = InputRegistry.action(Identifier.of(Identifier.ENGINE_NAMESPACE, "reload_texture"));

    private EngineActions() {}
}
