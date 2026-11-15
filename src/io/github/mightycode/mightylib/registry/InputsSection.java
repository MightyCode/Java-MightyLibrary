package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputAxis;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import io.github.mightycode.mightylib.inputs.inputType.IAxis;

import java.util.Objects;

/**
 * The actions and axes of a plugin (EngineRegistry.inputs), with their default bindings: those of the engine are bound
 * in every window, the others in the windows of the game (WindowConfig.hasProjectInputs). An identifier is an action
 * or an axis, not both. The keys are constants of the plugin (InputAction.of("game:jump")).
 */
public final class InputsSection {
    // An action or an axis and its default binding (null: not bound by default)
    static final class Input {
        final InputAction action;
        final InputAxis axis;
        final IAction actionBinding;
        final IAxis axisBinding;

        private Input(InputAction action, IAction actionBinding, InputAxis axis, IAxis axisBinding) {
            this.action = action;
            this.actionBinding = actionBinding;
            this.axis = axis;
            this.axisBinding = axisBinding;
        }
    }

    private final Registrations registrations;
    private final EngineRegistry registry;

    InputsSection(Registrations registrations, EngineRegistry registry) {
        this.registrations = registrations;
        this.registry = registry;
    }

    // Not bound by default: the game binds it later (IInputManager.init)
    public InputsSection action(InputAction action) {
        return action(action, null);
    }

    /**
     * @throws IllegalArgumentException if the identifier is outside the namespace of the plugin
     * @throws IllegalStateException    if the identifier is registered (action or axis)
     */
    public InputsSection action(InputAction action, IAction defaultBinding) {
        registrations.checkOpen();
        registry.checkNamespace(Objects.requireNonNull(action, "action").identifier());
        registrations.inputs.register(registry.namespace(), action.identifier(),
                new Input(action, defaultBinding, null, null));
        return this;
    }

    public InputsSection axis(InputAxis axis) {
        return axis(axis, null);
    }

    public InputsSection axis(InputAxis axis, IAxis defaultBinding) {
        registrations.checkOpen();
        registry.checkNamespace(Objects.requireNonNull(axis, "axis").identifier());
        registrations.inputs.register(registry.namespace(), axis.identifier(), new Input(null, null, axis, defaultBinding));
        return this;
    }

    /**
     * Replaces the default binding of an action of a previous plugin (the engine included).
     * @throws IllegalStateException if no action has this identifier
     */
    public InputsSection replace(InputAction action, IAction defaultBinding) {
        registrations.checkOpen();
        Input existing = registrations.inputs.get(Objects.requireNonNull(action, "action").identifier());
        if (existing != null && existing.action == null)
            throw new IllegalStateException(action + " is an axis, not an action");

        registrations.inputs.replace(registry.namespace(), action.identifier(),
                new Input(action, defaultBinding, null, null));
        return this;
    }

    public InputsSection replace(InputAxis axis, IAxis defaultBinding) {
        registrations.checkOpen();
        Input existing = registrations.inputs.get(Objects.requireNonNull(axis, "axis").identifier());
        if (existing != null && existing.axis == null)
            throw new IllegalStateException(axis + " is an action, not an axis");

        registrations.inputs.replace(registry.namespace(), axis.identifier(), new Input(null, null, axis, defaultBinding));
        return this;
    }
}
