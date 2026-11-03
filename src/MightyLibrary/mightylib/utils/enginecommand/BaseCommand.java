package MightyLibrary.mightylib.utils.enginecommand;

public abstract class BaseCommand {
    public static final int GENERAL_COMMAND = 0;
    public static final int SPECIFIC_COMMAND = 1;

    private final String commandName;
    private String range;

    public BaseCommand (String commandName, int range){
        this.commandName = commandName;
        if (range == GENERAL_COMMAND) this.range = "General";
        else if (range == SPECIFIC_COMMAND) this.range = "Specific";
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

    public int size() { return commandName.length(); }

    // Help to know how use the command
    public String help(){
        return "Help for " + commandName + " command (" + range + ")";
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
