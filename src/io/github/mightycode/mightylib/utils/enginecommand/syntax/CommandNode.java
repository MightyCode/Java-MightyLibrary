package io.github.mightycode.mightylib.utils.enginecommand.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Node of the declared syntax of a command: the root (the command name), a literal (a fixed word, case ignored) or an
 * argument (a typed value). A path from the root to a node that executes is a branch:
 * root.then(literal("mode", "...").then(argument("mode", choice("poll", "service"), "...")
 *         .option(Option.of("interval", "...").value("ms", longValue(1, 60000)))
 *         .executes(ctx -> ...)))
 * At the same place, the literals are tried before the arguments, then the arguments in their order.
 */
public final class CommandNode {
    enum Kind { ROOT, LITERAL, ARGUMENT }

    private final Kind kind;
    private final String name;
    private final String description;
    private final ArgumentType<?> type;
    private final List<CommandNode> children;
    private final List<Option> options;

    private boolean optional;
    private boolean variadic;
    private ICommandHandler handler;
    private String branchDescription;

    private CommandNode(Kind kind, String name, String description, ArgumentType<?> type) {
        this.kind = kind;
        this.name = name;
        this.description = description;
        this.type = type;
        this.children = new ArrayList<>();
        this.options = new ArrayList<>();
    }

    // The root of a command, given to BaseCommand.syntax
    public static CommandNode root(String commandName, String description) {
        return new CommandNode(Kind.ROOT, commandName, description, null);
    }

    public static CommandNode literal(String word) {
        return literal(word, null);
    }

    // A fixed word (a sub-command), case ignored
    public static CommandNode literal(String word, String description) {
        if (word == null || word.isEmpty() || word.startsWith("-") || HasWhitespace(word))
            throw new IllegalArgumentException("Invalid literal ->" + word + "<-");

        return new CommandNode(Kind.LITERAL, word, description, null);
    }

    public static CommandNode argument(String name, ArgumentType<?> type) {
        return argument(name, type, null);
    }

    /**
     * A typed value, read by CommandContext.get(name).
     * @param name unique among the arguments and options of its branches
     */
    public static CommandNode argument(String name, ArgumentType<?> type, String description) {
        if (name == null || name.isEmpty() || HasWhitespace(name))
            throw new IllegalArgumentException("Invalid argument name ->" + name + "<-");
        if (type == null)
            throw new IllegalArgumentException("The argument ->" + name + "<- needs a type");

        return new CommandNode(Kind.ARGUMENT, name, description, type);
    }

    public CommandNode then(CommandNode child) {
        if (child == null || child.kind == Kind.ROOT)
            throw new IllegalArgumentException("Invalid child of ->" + name + "<-");

        children.add(child);
        return this;
    }

    // Valid in the subtree of this node, anywhere after it
    public CommandNode option(Option option) {
        if (option == null)
            throw new IllegalArgumentException("Null option of ->" + name + "<-");

        options.add(option);
        return this;
    }

    // What the branch ending here does
    public CommandNode executes(ICommandHandler handler) {
        return executes(null, handler);
    }

    /**
     * What the branch ending here does.
     * @param description of the branch, shown with its usage line (the description of the node otherwise)
     */
    public CommandNode executes(String description, ICommandHandler handler) {
        if (handler == null)
            throw new IllegalArgumentException("Null handler of ->" + name + "<-");

        this.handler = handler;
        this.branchDescription = description;
        return this;
    }

    // Argument only: can be left out at the end of the line (only optional arguments can follow it)
    public CommandNode optional() {
        requireArgument("optional");
        optional = true;
        return this;
    }

    // Argument only: takes every following word (at least one unless optional), the last of its branch
    public CommandNode variadic() {
        requireArgument("variadic");
        variadic = true;
        return this;
    }

    private void requireArgument(String what) {
        if (kind != Kind.ARGUMENT)
            throw new IllegalStateException("Only an argument can be " + what + " : ->" + name + "<-");
    }

    // Nothing declared: the command has no syntax
    public boolean isEmpty() {
        return handler == null && children.isEmpty() && options.isEmpty();
    }

    Kind kind() { return kind; }
    String name() { return name; }
    String description() { return description; }
    ArgumentType<?> type() { return type; }
    List<CommandNode> children() { return Collections.unmodifiableList(children); }
    List<Option> options() { return Collections.unmodifiableList(options); }
    boolean isOptional() { return optional; }
    boolean isVariadic() { return variadic; }
    boolean isLiteral() { return kind == Kind.LITERAL; }
    boolean isArgument() { return kind == Kind.ARGUMENT; }
    boolean isGreedy() { return type != null && type.isGreedy(); }
    ICommandHandler handler() { return handler; }

    // The description of the branch ending here
    String branchDescription() {
        return branchDescription != null ? branchDescription : description;
    }

    // Checks the whole tree once (registration): a mistake of the command is found at once, not when typed
    void validate() {
        if (handler == null && children.isEmpty())
            throw new IllegalStateException("The branch ->" + name + "<- neither executes nor continues");

        validate(new HashSet<>());
    }

    // names: of the arguments and options above, they share the values of the context
    private void validate(Set<String> names) {
        Set<String> branchNames = new HashSet<>(names);
        if (kind == Kind.ARGUMENT && !branchNames.add(name))
            throw new IllegalStateException("Two arguments or options named ->" + name + "<- in a branch");

        for (Option option : options) {
            option.validate();
            if (!branchNames.add(option.getName()))
                throw new IllegalStateException("Two arguments or options named ->" + option.getName() + "<- in a branch");
        }

        if ((variadic || isGreedy()) && !children.isEmpty())
            throw new IllegalStateException("The argument ->" + name + "<- takes the rest of the line: it can't have children");
        if (kind != Kind.ROOT && handler == null && children.isEmpty())
            throw new IllegalStateException("The branch ->" + name + "<- neither executes nor continues");

        Set<String> literals = new HashSet<>();
        for (CommandNode child : children) {
            if (optional && !(child.kind == Kind.ARGUMENT && child.optional))
                throw new IllegalStateException("Only optional arguments can follow the optional argument ->" + name + "<-");
            if (child.kind == Kind.LITERAL && !literals.add(child.name.toLowerCase(Locale.ROOT)))
                throw new IllegalStateException("Two literals ->" + child.name + "<- after ->" + name + "<-");

            child.validate(branchNames);
        }
    }

    private static boolean HasWhitespace(String word) {
        for (int i = 0; i < word.length(); ++i) {
            if (Character.isWhitespace(word.charAt(i)))
                return true;
        }
        return false;
    }
}
