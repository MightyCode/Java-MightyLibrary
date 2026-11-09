package io.github.mightycode.mightylib.utils.enginecommand;

import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.Scene;

import java.util.Collection;
import java.util.Collections;

public abstract class BaseCommand {
    // Typed as its path alone when no other visible command has it, else as namespace:path
    private final Identifier id;
    private final String commandName;
    // Set once registered
    private CommandOwner owner;

    /**
     * @param id the namespace of the owner (mightylib for the engine, the game or the mod) and the name of the command
     */
    public BaseCommand(Identifier id) {
        if (id == null)
            throw new IllegalArgumentException("A command needs an identifier");

        this.id = id;
        this.commandName = id.path();
    }

    /**
     * Execute the command, always called on the main thread.
     * @param args Words of the command, args[0] is the name of the command as typed
     */
    public abstract CommandResult execute(String[] args);

    public Identifier getId() {
        return id;
    }

    // The path of the identifier: the short name
    public String getName() {
        return commandName;
    }

    // Null until registered
    public CommandOwner getOwner() {
        return owner;
    }

    void setOwner(CommandOwner owner) {
        if (this.owner != null)
            throw new IllegalArgumentException("The command ->" + commandName + "<- is already registered");

        this.owner = owner;
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
