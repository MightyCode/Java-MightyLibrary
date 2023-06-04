package MightyLibrary.mightylib.util.enginecommand;

import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.util.enginecommand.general.ListGeneralCommand;

import java.util.ArrayList;
import java.util.Scanner;

public class Commands {
    private final ArrayList<BaseCommand> commands;
    private final int endGeneralCommand;
    private final Scanner scan;

    private final ArrayList<Boolean> previousMouseStates;

    public boolean isWriteCommands;

    public Commands() {
        isWriteCommands = false;

        scan = new Scanner(System.in);

        // Add commands -> General
        commands = new ArrayList<>();
        ListGeneralCommand.addCommands(this);
        // Size
        endGeneralCommand = commands.size();

        previousMouseStates = new ArrayList<>();
    }


    public void writeCommand(){
        for (Context context : ContextManager.getInstance().getAllContext()){
            previousMouseStates.add(context.getMouseManager().getCursorState());
            context.getMouseManager().setCursor(true);
        }

        isWriteCommands = true;
        System.out.println("Write a command:");

        String command = scan.nextLine();

        System.out.print("\n");
        checkCommand(command);

        int i = 0;
        for (Context context : ContextManager.getInstance().getAllContext()){
            context.getMouseManager().setCursor(previousMouseStates.get(i));
            ++i;
        }
        previousMouseStates.clear();
    }


    private void checkCommand(String command) {
        // Return -1 if error on detection
        int firstWord = CommandMethods.indexFirstWord(command);

        if (firstWord != -1)
            submitCommand(command, command.substring(0, firstWord));
        else
            System.err.println("Error on the message : \n invalids chars or nothing in the char");
    }

    private void submitCommand(String command, String firstWord) {
        boolean found = false;
        int i = 0;
        while (!found && i < commands.size()) {
            // If write command and command in the array match
            if (commands.get(i).isCommand(firstWord)) {
                // Send and remove the unused first word
                commands.get(i).sendCommand(command);
                found = true;
            }
            ++i;
        }

        if (!found) {
            System.err.println("No command with its name ->" + firstWord + "<-");
        }
    }


    public void cancelWritingCommand(Object o){
        if (o instanceof BaseCommand){
            isWriteCommands = false;
        }
    }


    public ArrayList<BaseCommand> getCommands(){
        return commands;
    }


    public void addCommand(BaseCommand command){
        commands.add(command);
    }


    public void removeSpecificCommand(){
        if (commands.size() > endGeneralCommand) {
            commands.subList(endGeneralCommand, commands.size()).clear();
        }
    }
}
