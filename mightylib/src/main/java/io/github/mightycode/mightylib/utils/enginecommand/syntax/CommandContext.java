package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.ICommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The values read from a command line, by the name of their argument or option, already typed.
 * - argument or option with one value: get(name) gives the value;
 * - option with several values (--rect x y w h): get(name) gives the list of its values;
 * - flag (option without value): has(name), get(name) gives true;
 * - variadic argument, repeatable option: getList(name) gives every value (one per word or per use).
 */
public final class CommandContext {
    private final String commandName;
    private final ECommandLevel level;
    private final ICommandSource source;

    // Every value given, in order
    private final Map<String, List<Object>> values;
    private final Map<String, Object> defaults;

    /**
     * @param commandName the name of the command, for the messages
     * @param level       level of the source of the command
     * @param source      who sends the command
     */
    public CommandContext(String commandName, ECommandLevel level, ICommandSource source) {
        this.commandName = commandName;
        this.level = level == null ? ECommandLevel.DEV : level;
        this.source = source;
        this.values = new HashMap<>();
        this.defaults = new HashMap<>();
    }

    public String commandName() {
        return commandName;
    }

    // Level of the source: the answer can be adapted to it (a readable message for the player, details for the dev)
    public ECommandLevel currentLevel() {
        return level;
    }

    public ICommandSource source() {
        return source;
    }

    // Given on the line (an option with a default value that is not typed is not given)
    public boolean has(String name) {
        return values.containsKey(name);
    }

    /**
     * The value given (the first one of a variadic argument or a repeatable option), else the default value of the
     * option.
     * @throws IllegalArgumentException if nothing is given and there is no default value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        List<Object> given = values.get(name);
        if (given != null)
            return (T) given.get(0);
        if (defaults.containsKey(name))
            return (T) defaults.get(name);

        throw new IllegalArgumentException("No value ->" + name + "<- in the command " + commandName);
    }

    // The value given, else the default value of the option, else fallback
    @SuppressWarnings("unchecked")
    public <T> T getOr(String name, T fallback) {
        List<Object> given = values.get(name);
        if (given != null)
            return (T) given.get(0);
        if (defaults.containsKey(name))
            return (T) defaults.get(name);

        return fallback;
    }

    // Every value given, in order, empty if none
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String name) {
        List<Object> given = values.get(name);
        return given == null ? Collections.emptyList() : (List<T>) Collections.unmodifiableList(given);
    }

    void add(String name, Object value) {
        values.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
    }

    void setDefault(String name, Object value) {
        defaults.put(name, value);
    }
}
