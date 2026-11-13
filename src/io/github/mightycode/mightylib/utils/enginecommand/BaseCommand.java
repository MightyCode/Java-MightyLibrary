package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandParser;

import java.util.Arrays;
import java.util.Collection;

/**
 * A command: its identifier, its level, its description and its declared syntax (syntax(root)), from which come the
 * parsing of the line, the completion, the help and its JSON, the levels and conditions of each branch.
 */
public abstract class BaseCommand {
    // Typed as its path alone when no other visible command has it, else as namespace:path
    private final Identifier id;
    private final String commandName;
    private final ECommandLevel level;
    private final String description;
    // Set once registered
    private CommandOwner owner;
    private Commands registry;
    // Built at the first registration
    private CommandParser parser;

    /**
     * @param id          the namespace of the owner (mightylib for the engine, the game or the mod) and the name of
     *                    the command
     * @param level       the lowest level that sees the command: hidden from the others, as if it did not exist
     * @param description what the command does, first line of its help
     */
    public BaseCommand(Identifier id, ECommandLevel level, String description) {
        if (id == null)
            throw new IllegalArgumentException("A command needs an identifier");
        if (level == null)
            throw new IllegalArgumentException("The command " + id + " needs a level");
        if (description == null || description.isEmpty())
            throw new IllegalArgumentException("The command " + id + " needs a description");

        this.id = id;
        this.commandName = id.path();
        this.level = level;
        this.description = description;
    }

    /**
     * Declares the syntax of the command on its root, called once at its first registration (a mistake of the syntax
     * is an IllegalStateException then): the fields it uses must be set by the constructor. Each branch ends with
     * executes(handler):
     * root.then(literal("mode", "...").then(argument("mode", choice("poll", "service"), "...")
     *         .option(Option.of("interval", "...").value("ms", longValue(1, 60000)))
     *         .executes(ctx -> ...)))
     */
    protected abstract void syntax(CommandNode root);

    public String getDescription() {
        return description;
    }

    // ---- Declared syntax ----

    // Main thread, at the first registration (or the first help of a command not registered)
    private void buildSyntax() {
        if (parser != null)
            return;

        CommandNode root = CommandNode.root(commandName, description);
        syntax(root);
        if (root.isEmpty())
            throw new IllegalStateException("The command " + id + " declares no syntax");

        parser = new CommandParser(root);
    }

    // The values of a line, with the level and the source running or completing the command
    private CommandContext newContext(String typedName) {
        ICommandSource source = registry == null ? ICommandSource.API : registry.getCurrentSource();
        return new CommandContext(typedName, source.level(), source);
    }

    /**
     * False when a hidden condition of the root is false: the command does not exist for the source, like a command
     * above its level. Asked under the owner, like the execution: only for a root with a hidden condition (asked for
     * every command at each lookup and completion).
     */
    boolean isAvailable() {
        if (parser == null || !parser.hasHiddenRootCondition())
            return true;

        return owner == null ? availableNow() : owner.run(this::availableNow);
    }

    private boolean availableNow() {
        return parser.isAvailable(newContext(commandName));
    }

    // Main thread, under the owner. Parses the words (args[0] = name typed) and runs the branch, or gives the help
    CommandResult run(String[] args) {
        CommandParser.Parsed parsed = parser.parse(Arrays.copyOfRange(args, 1, args.length), newContext(args[0]));
        if (parsed.isHelp())
            return helpNow();

        CommandResult result = parsed.getHandler().run(parsed.getContext());
        return result == null ? CommandResult.error("The command " + commandName + " answered nothing") : result;
    }

    // Main thread, under the owner. Candidates for the last word (words[0] = name typed), unfiltered
    Collection<String> completeSyntax(String[] words) {
        return parser.complete(Arrays.copyOfRange(words, 1, words.length), newContext(words[0]));
    }

    /**
     * Main thread. The help the current source sees: help <cmd> and <cmd> --help. The generated help, with the visible
     * tree of the syntax in JSON in the data. A command not registered yet declares its syntax first.
     */
    public CommandResult helpResult() {
        if (owner == null) {
            buildSyntax();
            return helpNow();
        }
        return owner.run(this::helpNow);
    }

    private CommandResult helpNow() {
        return parser.help(id.toString(), level, newContext(commandName));
    }

    public Identifier getId() {
        return id;
    }

    // The path of the identifier: the short name
    public String getName() {
        return commandName;
    }

    public ECommandLevel getLevel() {
        return level;
    }

    // Null until registered
    public CommandOwner getOwner() {
        return owner;
    }

    void setRegistration(Commands registry, CommandOwner owner) {
        if (this.owner != null)
            throw new IllegalArgumentException("The command ->" + commandName + "<- is already registered");

        buildSyntax();
        this.registry = registry;
        this.owner = owner;
    }

    // Removed from its registry: it can be registered again (a scene used again registers its commands again)
    void clearRegistration() {
        registry = null;
        owner = null;
    }
}
