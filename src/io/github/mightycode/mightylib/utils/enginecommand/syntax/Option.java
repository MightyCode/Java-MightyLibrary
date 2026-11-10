package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Named option of a command: "--name value", "--name=value", or a flag "--name" without value. Optional, valid in the
 * subtree of the node that declares it, anywhere after it on the line. No short alias (collisions between the engine,
 * the game and the mods).
 * Option.of("interval", "Time between two reads").value("ms", Arguments.longValue(1, 60000)).defaultValue(1000L)
 */
public final class Option {
    // Lowercase kebab case: "scale", "max-size"
    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    // Added by the engine to every command
    public static final String HELP = "help";

    private final String name;
    private final String description;
    private final List<String> valueNames;
    private final List<ArgumentType<?>> valueTypes;
    private Object defaultValue;
    private boolean hasDefault;
    private boolean repeatable;
    private final Set<String> excluded;
    private final List<CommandCondition> conditions;
    // Null: the one of its node
    private ECommandLevel level;

    private Option(String name, String description) {
        this.name = name;
        this.description = description;
        this.valueNames = new ArrayList<>();
        this.valueTypes = new ArrayList<>();
        this.excluded = new LinkedHashSet<>();
        this.conditions = new ArrayList<>();
    }

    /**
     * A flag until values are added.
     * @param name lowercase kebab case, typed after "--"; "help" is reserved
     */
    public static Option of(String name, String description) {
        if (name == null || !NAME.matcher(name).matches())
            throw new IllegalArgumentException("Invalid option name ->" + name + "<- (lowercase kebab case : max-size)");
        if (name.equals(HELP))
            throw new IllegalArgumentException("The option --help is added to every command");

        return new Option(name, description);
    }

    // Adds a value, typed after the previous one: --rect x y w h
    public Option value(String valueName, ArgumentType<?> type) {
        if (valueName == null || valueName.isEmpty() || type == null)
            throw new IllegalArgumentException("A value of the option --" + name + " needs a name and a type");
        if (type.isGreedy())
            throw new IllegalArgumentException("The value of the option --" + name + " can't take the rest of the line");

        valueNames.add(valueName);
        valueTypes.add(type);
        return this;
    }

    // Value of get(name) when the option is not given (one value: the value; several: their list)
    public Option defaultValue(Object value) {
        defaultValue = value;
        hasDefault = true;
        return this;
    }

    // Can be given several times: CommandContext.getList(name) gives each use
    public Option repeatable() {
        repeatable = true;
        return this;
    }

    // Reserved to a level and above: for the lower levels it does not exist (no completion, no help, unknown option)
    public Option level(ECommandLevel level) {
        if (level == null)
            throw new IllegalArgumentException("Null level of the option --" + name);

        this.level = level;
        return this;
    }

    // Asked at each execution and completion, see CommandCondition
    public Option requires(CommandCondition condition) {
        if (condition == null)
            throw new IllegalArgumentException("Null condition of the option --" + name);

        conditions.add(condition);
        return this;
    }

    // Options that can't be given with this one (--rect and --corners): an error if both are
    public Option excludes(String... names) {
        for (String other : names)
            excluded.add(other.toLowerCase(Locale.ROOT));
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int valueCount() {
        return valueNames.size();
    }

    public String valueName(int index) {
        return valueNames.get(index);
    }

    public ArgumentType<?> valueType(int index) {
        return valueTypes.get(index);
    }

    public boolean hasDefault() {
        return hasDefault;
    }

    public Object getDefault() {
        return defaultValue;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    // Both ways: one option excluding the other is enough
    boolean conflictsWith(Option other) {
        return excluded.contains(other.name) || other.excluded.contains(name);
    }

    ECommandLevel getLevel() {
        return level;
    }

    boolean isVisible(CommandContext context) {
        return CommandCondition.Visible(level, conditions, context);
    }

    void check(CommandContext context) {
        CommandCondition.Check(conditions, context);
    }

    Set<String> getExcluded() {
        return Collections.unmodifiableSet(excluded);
    }

    void validate() {
        if (hasDefault && valueNames.isEmpty())
            throw new IllegalStateException("The flag --" + name + " can't have a default value");
    }
}
