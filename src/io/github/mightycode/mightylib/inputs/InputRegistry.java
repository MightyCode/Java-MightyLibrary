package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.registry.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry of the actions and axes of the engine, the game and its mods, shared by all the windows.
 * An identifier is unique among the actions and the axes : a second registration is refused (collision).
 * Elements are registered when their constant classes are initialised (EngineActions, the ActionId of a project).
 */
public final class InputRegistry {
    private static final Map<Identifier, InputAction> ACTIONS = new HashMap<>();
    private static final Map<Identifier, InputAxis> AXES = new HashMap<>();
    private static int nextIndex = 0;

    private InputRegistry() {}

    public static InputAction action(String identifier) {
        return action(Identifier.parse(identifier));
    }

    public static synchronized InputAction action(Identifier identifier) {
        checkFree(identifier);
        InputAction action = new InputAction(identifier, nextIndex++);
        ACTIONS.put(identifier, action);
        return action;
    }

    public static InputAxis axis(String identifier) {
        return axis(Identifier.parse(identifier));
    }

    public static synchronized InputAxis axis(Identifier identifier) {
        checkFree(identifier);
        InputAxis axis = new InputAxis(identifier, nextIndex++);
        AXES.put(identifier, axis);
        return axis;
    }

    private static void checkFree(Identifier identifier) {
        if (ACTIONS.containsKey(identifier))
            throw new IllegalArgumentException("Identifier " + identifier + " is already an action");
        if (AXES.containsKey(identifier))
            throw new IllegalArgumentException("Identifier " + identifier + " is already an axis");
    }

    /**
     * Actions matching the name : the full identifier, or else the path alone (case insensitive) in any namespace.
     * One element = found, several = ambiguous, none = unknown.
     */
    public static synchronized List<InputAction> matchActions(String name) {
        return match(ACTIONS, name, InputAction::identifier);
    }

    public static synchronized List<InputAxis> matchAxes(String name) {
        return match(AXES, name, InputAxis::identifier);
    }

    // The only action matching the name, null if unknown or ambiguous
    public static InputAction findAction(String name) {
        List<InputAction> actions = matchActions(name);
        return actions.size() == 1 ? actions.get(0) : null;
    }

    public static InputAxis findAxis(String name) {
        List<InputAxis> axes = matchAxes(name);
        return axes.size() == 1 ? axes.get(0) : null;
    }

    // Sorted by identifier
    public static synchronized List<InputAction> getActions() {
        List<InputAction> actions = new ArrayList<>(ACTIONS.values());
        actions.sort(Comparator.comparing(InputAction::identifier));
        return actions;
    }

    public static synchronized List<InputAxis> getAxes() {
        List<InputAxis> axes = new ArrayList<>(AXES.values());
        axes.sort(Comparator.comparing(InputAxis::identifier));
        return axes;
    }

    private static <T> List<T> match(Map<Identifier, T> elements, String name, Function<T, Identifier> identifierOf) {
        List<T> result = new ArrayList<>();
        if (name == null)
            return result;

        if (name.indexOf(':') >= 0) {
            try {
                T element = elements.get(Identifier.parse(name.toLowerCase(Locale.ROOT)));
                if (element != null)
                    result.add(element);
            } catch (IllegalArgumentException e) {
                // Not a valid identifier : nothing matches
            }
            return result;
        }

        for (T element : elements.values()) {
            if (identifierOf.apply(element).path().equalsIgnoreCase(name))
                result.add(element);
        }

        result.sort(Comparator.comparing(identifierOf));
        return result;
    }
}
