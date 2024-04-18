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

    // Command without the name of this current command
    public abstract void sendCommand(String command);

    public boolean isCommand(String commandName){
        return this.commandName.equals(commandName);
    }

    public int size() { return commandName.length(); }

    // Help to know how use the command
    public void help(){
        System.out.println("Help for " + commandName + " command (" + range + ")");
    }

    public boolean checkArgumentHelp(String[] commands){
        if(commands.length > 1){
            if (commands[1].equals("help")) {
                this.help();
                return true;
            }
            return false;
        }
        return  false;
    }

    public void noEnoughArgumentError(){
        System.err.println("No argument !\nWrite -> " + commandName  + " help<- if you don't know what are/is argument(s)");
    }

    public void unknownArgumentError(String command){
        System.err.println("Unknown argument(s) ->" + command + "<-");
    }


}
