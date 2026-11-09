package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;

/**
 * Handle of a registered action (jump, open menu...), obtained from InputRegistry.action.
 * Bound to keys/buttons in each InputManager by an ActionInput. One handle per identifier.
 */
public final class InputAction {
    private final Identifier identifier;
    private final int index;

    InputAction(Identifier identifier, int index) {
        this.identifier = identifier;
        this.index = index;
    }

    public Identifier identifier() { return identifier; }

    // Dense number given by the registry, valid only during this launch (never saved)
    int index() { return index; }

    @Override
    public String toString() { return identifier.toString(); }
}
