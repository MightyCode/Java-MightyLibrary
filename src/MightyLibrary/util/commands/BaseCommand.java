package MightyLibrary.util.commands;

public abstract class BaseCommand {
    private String commandName;


    public BaseCommand (String commandName){
        this.commandName = commandName;
    }

    // Command without the name of this current command
    public abstract void sendCommand(String command);

    // Help to know how use the command
    public abstract void help();

    public void error(String command){
        System.err.println("Unknown argument(s) ->" + command + "<-");
    }

    public boolean isCommand(String commandName){
        return this.commandName.equals(commandName);
    }

    public int size(){return commandName.length();}

}
