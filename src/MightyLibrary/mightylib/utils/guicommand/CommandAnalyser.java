package MightyLibrary.mightylib.utils.guicommand;

import java.util.HashMap;

public class CommandAnalyser {
    public static final int EMPTY_COMMAND = 0;
    public static final int NO_OPTIONAL_ARGUMENT = 1;

    public static final int COMMAND_TYPE_ARGUMENT = 0;
    public static final int FIRST_OPTIONAL_ARGUMENT = 1;

    HashMap<String, ICommand> relations;

    public CommandAnalyser() {
        relations = new HashMap<>();

    }

    public void addCommand(String commandName, ICommand command){
        relations.put(commandName, command);
    }

    public ResultCommand analyseCommand(String command){
        String[] args = command.replace("/", "").replace("\n", "").split(" ");

        if (args.length == EMPTY_COMMAND)
            return new PromptResultCommand("> NOK : Empty command !");

        if ( ! relations.containsKey(args[COMMAND_TYPE_ARGUMENT]))
            return new PromptResultCommand("-> NOK : command introuvable");

        if (args.length == NO_OPTIONAL_ARGUMENT)
            return relations.get(args[COMMAND_TYPE_ARGUMENT]).process(args);

        if (args[FIRST_OPTIONAL_ARGUMENT].equalsIgnoreCase("help") || args[FIRST_OPTIONAL_ARGUMENT].equalsIgnoreCase("-h") )
            return relations.get(args[COMMAND_TYPE_ARGUMENT]).returnHelp();


        return relations.get(args[COMMAND_TYPE_ARGUMENT]).process(args);
    }
}
