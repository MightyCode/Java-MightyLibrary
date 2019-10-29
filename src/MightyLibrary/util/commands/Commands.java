package MightyLibrary.util.commands;

import MightyLibrary.inputs.MouseManager;
import MightyLibrary.main.ManagerContainer;
import MightyLibrary.util.commands.general.ListGeneralCommand;
import org.lwjgl.system.CallbackI;

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
        ListGeneralCommand.addCommand(commands);
        // Size
        endGeneralCommand = commands.size();
    }

    public void writeCommand(){
        boolean previousMouseState = mouseManager.getCursorState();
        mouseManager.setCursor(true);

        isWriteCommands = true;
        System.out.println("Write a command:");

        String hey = scan.nextLine();
        if(hey.equals("echap") || hey.equals("exit")){
            isWriteCommands = false;
        } else {
            System.out.println("");
            checkCommand(hey);
        }
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
        while (!found && i < commands.size()){
            // If write command and command in the array match
            if (commands.get(i).isCommand(firstWord)){
                // Send and remove the unused first word
                commands.get(i).sendCommand(command.substring(commands.get(i).size() + 1));
                found = true;
            }
            i++;
        }

        if(!found){
            System.err.println("No command with its name ->"  + firstWord + "<-");
        }
    }

    public void removeSpecificCommand(){
        if (commands.size() > endGeneralCommand) {
            commands.subList(endGeneralCommand, commands.size()).clear();
        }
    }
}
