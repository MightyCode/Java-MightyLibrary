package io.github.mightycode.mightylib.utils.enginecommand;

public abstract class BaseCommand {
    private final String commandName;

    public BaseCommand(String commandName) {
        if (commandName == null || commandName.isEmpty() || !commandName.equals(commandName.trim()) || commandName.contains("\""))
            throw new IllegalArgumentException("Invalid command name ->" + commandName + "<-");

        this.commandName = commandName;
    }

    /**
     * Execute the command, always called on the main thread.
     * @param args Words of the command, args[0] is the name of the command
     */
    public abstract CommandResult execute(String[] args);

    public String getName() {
        return commandName;
    }

    public boolean isCommand(String commandName){
        return this.commandName.equals(commandName);
    }

    // Help to know how use the command
    public String help(){
        return "Help for " + commandName + " command";
    }

    public boolean checkArgumentHelp(String[] commands){
        return commands.length > 1 && commands[1].equals("help");
    }

    public CommandResult noEnoughArgumentError(){
        return CommandResult.error("No argument !\nWrite -> " + commandName  + " help<- if you don't know what are/is argument(s)");
    }

    public CommandResult unknownArgumentError(String command){
        return CommandResult.error("Unknown argument(s) ->" + command + "<-");
    }
}
