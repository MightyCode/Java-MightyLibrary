package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;

import java.util.Objects;

/**
 * Key of an action (jump, open menu...): its identifier alone, equal to any other key with the same
 * identifier. Declared by a plugin with its default binding (EngineRegistry.inputs), read in each InputManager.
 * Constants of a game: public static final InputAction JUMP = InputAction.of("game:jump");
 */
public final class InputAction {
    private final Identifier identifier;

    private InputAction(Identifier identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    public static InputAction of(Identifier identifier) {
        return new InputAction(identifier);
    }

    // "namespace:path"
    public static InputAction of(String identifier) {
        return new InputAction(Identifier.parse(identifier));
    }

    public Identifier identifier() { return identifier; }

    @Override
    public boolean equals(Object other) {
        return other instanceof InputAction && identifier.equals(((InputAction) other).identifier);
    }

    @Override
    public int hashCode() { return identifier.hashCode(); }

    @Override
    public String toString() { return identifier.toString(); }
}
