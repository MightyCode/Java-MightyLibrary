package io.github.mightycode.mightylib.inputs;

import java.util.ArrayList;
import java.util.List;

/**
 * Who reads the inputs of a window: a stack of owners (any object). Empty = the scene, the default holder. The top
 * owner reads the raw values (KeyboardManager.getRawKeyEvents, MouseManager.getRawPos...), the scene sees nothing
 * while the stack is not empty (except through the engine actions, see InputManager).
 *
 * A change of the top owner is told to the InputManager, which hides from the new holder what was pressed during
 * the current tick (an Escape that gives the focus back is not an Escape for the scene).
 *
 * @author MightyCode
 */
public final class InputFocus {
    private final List<Object> owners = new ArrayList<>();
    private final Runnable onChange;

    InputFocus(Runnable onChange) {
        this.onChange = onChange;
    }

    /** The owner takes the focus above the others (it moves up if it already holds a place). */
    public void request(Object owner) {
        if (owner == null)
            throw new IllegalArgumentException("A focus owner is not null");
        if (!owners.isEmpty() && owners.get(owners.size() - 1) == owner)
            return;

        owners.remove(owner);
        owners.add(owner);
        onChange.run();
    }

    /** The owner gives up its place, wherever it is: the owner below gets the focus back if it was the top. */
    public void release(Object owner) {
        int index = owners.indexOf(owner);
        if (index < 0)
            return;

        boolean top = index == owners.size() - 1;
        owners.remove(index);
        if (top)
            onChange.run();
    }

    /** The owner at the top, null when the scene holds the focus. */
    public Object current() {
        return owners.isEmpty() ? null : owners.get(owners.size() - 1);
    }

    /** True when the owner is at the top: the one to read the raw inputs. */
    public boolean isCurrent(Object owner) {
        return owner != null && owners.size() > 0 && owners.get(owners.size() - 1) == owner;
    }

    /** True when the owner has a place in the stack, at the top or not. */
    public boolean isRequested(Object owner) {
        return owners.contains(owner);
    }

    /** True when someone holds the focus: the scene reads nothing. */
    public boolean isHeld() {
        return !owners.isEmpty();
    }
}
