package MightyLibrary.mightylib.util.commands;

import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.util.commands.general.ListGeneralCommand;

import java.util.ArrayList;
import java.util.Scanner;

public class Commands {
    private ArrayList<BaseCommand> commands;
    private int endGeneralCommand;
    private Scanner scan;
    private MouseManager mouseManager;

    public boolean isWriteCommands;

    public Commands() {
        isWriteCommands = false;
        mouseManager = ManagerContainer.getInstance().mouseManager;

        scan = new Scanner(System.in);

        // Add commands -> General
        commands = new ArrayList<>();
        ListGeneralCommand.addCommands(this);
        // Size
        endGeneralCommand = commands.size();
    }


    public void writeCommand(){
        boolean previousMouseState = mouseManager.getCursorState();
        mouseManager.setCursor(true);

        isWriteCommands = true;
        System.out.println("Write a command:");

        String command = scan.nextLine();

        System.out.println("");
        checkCommand(command);

        mouseManager.setCursor(previousMouseState);
    }


    private void checkCommand(String command) {
        // Return -1 if error on detection
        int firstWord = CommandMethods.indexFirstWord(command);
        if (firstWord != -1) submitCommand(command, command.substring(0, firstWord));
        else System.err.println("Error on the message : \n invalids chars or nothing in the char");
    }


    private void submitCommand(String command, String firstWord) {
        boolean found = false;
        int i = 0;
        while (!found && i < commands.size()) {
            // If write command and command in the array match
            if (commands.get(i).isCommand(firstWord)) {
                // Send and remove the unused first word
                if(command.length() > firstWord.length())  commands.get(i).sendCommand(command);
                else                                       commands.get(i).sendCommand(command);
                found = true;
            }
            ++i;;
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
