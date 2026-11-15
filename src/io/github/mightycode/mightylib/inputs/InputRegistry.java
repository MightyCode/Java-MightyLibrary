package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import io.github.mightycode.mightylib.inputs.inputType.IAxis;
import io.github.mightycode.mightylib.registry.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The actions and axes of an engine (Engine.inputs), declared by the plugins (EngineRegistry.inputs) with their default
 * bindings, read by every IInputManager. An identifier is an action or an axis, not both. Each element gets a dense
 * index at its declaration, valid during this launch only (simulation of the inputs, never saved).
 * Frozen once built: a key that is not declared is an error wherever it is used.
 */
public final class InputRegistry {
    private static final class Element<K, B> {
        private final K key;
        private final int index;
        // Null: not bound by default
        private final B defaultBinding;

        private Element(K key, int index, B defaultBinding) {
            this.key = key;
            this.index = index;
            this.defaultBinding = defaultBinding;
        }
    }

    private final Map<InputAction, Element<InputAction, IAction>> actions = new LinkedHashMap<>();
    private final Map<InputAxis, Element<InputAxis, IAxis>> axes = new LinkedHashMap<>();
    private int nextIndex;
    private boolean frozen;

    /**
     * @param defaultBinding null: not bound by default (the game binds it later)
     * @throws IllegalArgumentException if the identifier is already an action or an axis
     */
    public InputRegistry declareAction(InputAction action, IAction defaultBinding) {
        checkFree(Objects.requireNonNull(action, "action").identifier());
        actions.put(action, new Element<>(action, nextIndex++, defaultBinding));
        return this;
    }

    public InputRegistry declareAxis(InputAxis axis, IAxis defaultBinding) {
        checkFree(Objects.requireNonNull(axis, "axis").identifier());
        axes.put(axis, new Element<>(axis, nextIndex++, defaultBinding));
        return this;
    }

    // Nothing can be declared afterwards
    public InputRegistry freeze() {
        frozen = true;
        return this;
    }

    private void checkFree(Identifier identifier) {
        if (frozen)
            throw new IllegalStateException("The inputs are frozen: declare " + identifier
                    + " in IPlugin.register (registry.inputs())");
        if (actions.containsKey(InputAction.of(identifier)))
            throw new IllegalArgumentException("Identifier " + identifier + " is already an action");
        if (axes.containsKey(InputAxis.of(identifier)))
            throw new IllegalArgumentException("Identifier " + identifier + " is already an axis");
    }

    public boolean isDeclared(InputAction action) {
        return actions.containsKey(action);
    }

    public boolean isDeclared(InputAxis axis) {
        return axes.containsKey(axis);
    }

    /**
     * @throws IllegalStateException if the action is not declared
     */
    int index(InputAction action) {
        Element<InputAction, IAction> element = actions.get(action);
        if (element == null)
            throw new IllegalStateException("The action " + action + " is not declared: declare it in IPlugin.register"
                    + " (registry.inputs().action)");

        return element.index;
    }

    int index(InputAxis axis) {
        Element<InputAxis, IAxis> element = axes.get(axis);
        if (element == null)
            throw new IllegalStateException("The axis " + axis + " is not declared: declare it in IPlugin.register"
                    + " (registry.inputs().axis)");

        return element.index;
    }

    // The engine's (namespace mightylib) are bound in every window, the others in the windows of the game
    private static boolean IsBoundIn(Identifier identifier, boolean pluginInputs) {
        return pluginInputs || Identifier.ENGINE_NAMESPACE.equals(identifier.namespace());
    }

    // The default bindings of a window, in declaration order
    List<ActionInput> defaultActions(boolean pluginInputs) {
        List<ActionInput> bindings = new ArrayList<>();
        for (Element<InputAction, IAction> element : actions.values()) {
            if (element.defaultBinding != null && IsBoundIn(element.key.identifier(), pluginInputs))
                bindings.add(new ActionInput(element.key, element.defaultBinding));
        }

        return bindings;
    }

    List<AxisInput> defaultAxes(boolean pluginInputs) {
        List<AxisInput> bindings = new ArrayList<>();
        for (Element<InputAxis, IAxis> element : axes.values()) {
            if (element.defaultBinding != null && IsBoundIn(element.key.identifier(), pluginInputs))
                bindings.add(new AxisInput(element.key, element.defaultBinding));
        }

        return bindings;
    }

    /**
     * Actions matching the name : the full identifier, or else the path alone (case insensitive) in any namespace.
     * One element = found, several = ambiguous, none = unknown.
     */
    public List<InputAction> matchActions(String name) {
        return match(actions.keySet(), name, InputAction::identifier);
    }

    public List<InputAxis> matchAxes(String name) {
        return match(axes.keySet(), name, InputAxis::identifier);
    }

    // The only action matching the name, null if unknown or ambiguous
    public InputAction findAction(String name) {
        List<InputAction> found = matchActions(name);
        return found.size() == 1 ? found.get(0) : null;
    }

    public InputAxis findAxis(String name) {
        List<InputAxis> found = matchAxes(name);
        return found.size() == 1 ? found.get(0) : null;
    }

    // Sorted by identifier
    public List<InputAction> getActions() {
        List<InputAction> sorted = new ArrayList<>(actions.keySet());
        sorted.sort(Comparator.comparing(InputAction::identifier));
        return sorted;
    }

    public List<InputAxis> getAxes() {
        List<InputAxis> sorted = new ArrayList<>(axes.keySet());
        sorted.sort(Comparator.comparing(InputAxis::identifier));
        return sorted;
    }

    private static <T> List<T> match(Iterable<T> elements, String name, Function<T, Identifier> identifierOf) {
        List<T> result = new ArrayList<>();
        if (name == null)
            return result;

        if (name.indexOf(':') >= 0) {
            Identifier identifier;
            try {
                identifier = Identifier.parse(name.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // Not a valid identifier : nothing matches
                return result;
            }

            for (T element : elements) {
                if (identifierOf.apply(element).equals(identifier))
                    result.add(element);
            }
            return result;
        }

        for (T element : elements) {
            if (identifierOf.apply(element).path().equalsIgnoreCase(name))
                result.add(element);
        }

        result.sort(Comparator.comparing(identifierOf));
        return result;
    }
}
