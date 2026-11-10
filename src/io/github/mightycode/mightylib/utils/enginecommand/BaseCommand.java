package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
    // Built at the first registration. Provisional (migration): null for a command without syntax
    private CommandParser parser;
    private boolean syntaxBuilt;

    /**
     * Provisional (migration to the declared syntax): a command without description nor syntax, read by
     * execute(String[]) and completeArgument. Removed once every command declares its syntax.
     */
    public BaseCommand(Identifier id, ECommandLevel level) {
        this(id, level, null, false);
    }

    /**
     * @param id          the namespace of the owner (mightylib for the engine, the game or the mod) and the name of
     *                    the command
     * @param level       the lowest level that sees the command: hidden from the others, as if it did not exist
     * @param description what the command does, first line of its help
     */
    public BaseCommand(Identifier id, ECommandLevel level, String description) {
        this(id, level, description, true);
    }

    private BaseCommand(Identifier id, ECommandLevel level, String description, boolean described) {
        if (id == null)
            throw new IllegalArgumentException("A command needs an identifier");
        if (level == null)
            throw new IllegalArgumentException("The command " + id + " needs a level");
        if (described && (description == null || description.isEmpty()))
            throw new IllegalArgumentException("The command " + id + " needs a description");

        this.id = id;
        this.commandName = id.path();
        this.level = level;
        this.description = description;
    }

    /**
     * Declares the syntax of the command on its root, called once at its first registration (a mistake of the syntax
     * is an IllegalStateException then). Each branch ends with executes(handler):
     * root.then(literal("mode", "...").then(argument("mode", choice("poll", "service"), "...")
     *         .option(Option.of("interval", "...").value("ms", longValue(1, 60000)))
     *         .executes(ctx -> ...)))
     * Provisional (migration): empty by default, the command then keeps execute(String[]); abstract once every
     * command declares its syntax.
     */
    protected void syntax(CommandNode root) {
    }

    /**
     * Provisional (migration to the declared syntax): execution of a command without syntax, always called on the
     * main thread. A command with a syntax runs the handler of its branch instead.
     * @param args Words of the command, args[0] is the name of the command as typed
     */
    public CommandResult execute(String[] args) {
        return CommandResult.error("The command " + id + " has no syntax");
    }

    public String getDescription() {
        return description;
    }

    // ---- Declared syntax ----

    // Main thread, at the first registration
    private void buildSyntax() {
        if (syntaxBuilt)
            return;

        CommandNode root = CommandNode.root(commandName, description);
        syntax(root);
        parser = root.isEmpty() ? null : new CommandParser(root);
        syntaxBuilt = true;
    }

    // Provisional (migration): false for a command read by execute(String[])
    boolean hasSyntax() {
        return parser != null;
    }

    // The values of a line, with the level and the source running or completing the command
    private CommandContext newContext(String typedName) {
        ICommandSource source = registry == null ? ICommandSource.API : registry.getCurrentSource();
        return new CommandContext(typedName, source.level(), source);
    }

    /**
     * False when a hidden condition of the root is false: the command does not exist for the source, like a command
     * above its level.
     */
    boolean isAvailable() {
        return parser == null || parser.isAvailable(newContext(commandName));
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
     * Main thread. The help the current source sees: help <cmd> and <cmd> --help. A command with a syntax gives its
     * generated help, with its visible tree in JSON in the data.
     */
    public CommandResult helpResult() {
        return owner == null ? helpNow() : owner.run(this::helpNow);
    }

    private CommandResult helpNow() {
        if (parser == null)
            return CommandResult.ok(help());

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

    /**
     * Level of the source of the command being run or completed: the answer can be adapted to it (a readable
     * message for the player, details in the data for the developer).
     */
    protected ECommandLevel currentLevel() {
        return registry == null ? ECommandLevel.DEV : registry.getCurrentLevel();
    }

    // A sub-command (an argument) reserved to a level: hide it from the completion of the lower levels
    protected boolean hasLevel(ECommandLevel level) {
        return currentLevel().includes(level);
    }

    /**
     * A sub-command reserved to a level: below it, the command stops with the error of an unknown argument (or of a
     * missing one when typed is null), the sub-command does not exist for the source.
     */
    protected void requireLevel(ECommandLevel level, String typed) {
        if (hasLevel(level))
            return;

        throw new CommandException(typed == null ? noEnoughArgumentError().getMessage()
                : unknownArgumentError(typed).getMessage());
    }

    // Help to know how use the command
    public String help(){
        return "Help for " + id + " command";
    }

    /**
     * Values the word being typed can take, for the completion (Tab in the console). Main thread.
     * Unfiltered values are fine: they are filtered by what is typed.
     * @param args Words typed, args[0] is the name of the command and the last one is the word being completed
     *             (unfinished, possibly empty)
     */
    public Collection<String> completeArgument(String[] args) {
        return Collections.emptyList();
    }

    public boolean checkArgumentHelp(String[] commands){
        return commands.length > 1 && commands[1].equals("help");
    }

    public CommandResult noEnoughArgumentError(){
        return CommandResult.error("No argument !\nWrite -> " + commandName  + " help<- if you don't know what are/is argument(s)");
    }

    /**
     * Stop the command with "Scene still loading" until the scene is launched: its cameras, renderers and objects
     * only exist from launch() on. A command of a scene can be typed while the scene loads.
     */
    protected static void requireLaunched(Scene scene) {
        if (scene == null || !scene.isLaunched())
            throw new CommandException("Scene still loading");
    }

    public CommandResult unknownArgumentError(String command){
        return CommandResult.error("Unknown argument(s) ->" + command + "<-");
    }
}
