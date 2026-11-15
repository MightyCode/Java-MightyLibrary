package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;

import java.util.Objects;

/**
 * Key of an axis (move, look...): its identifier alone, equal to any other key with the same identifier.
 * Declared by a plugin with its default binding (EngineRegistry.inputs), read with IInputManager.getAxis.
 */
public final class InputAxis {
    private final Identifier identifier;

    private InputAxis(Identifier identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    public static InputAxis of(Identifier identifier) {
        return new InputAxis(identifier);
    }

    // "namespace:path"
    public static InputAxis of(String identifier) {
        return new InputAxis(Identifier.parse(identifier));
    }

    public Identifier identifier() { return identifier; }

    @Override
    public boolean equals(Object other) {
        return other instanceof InputAxis && identifier.equals(((InputAxis) other).identifier);
    }

    @Override
    public int hashCode() { return identifier.hashCode(); }

    @Override
    public String toString() { return identifier.toString(); }
}
