package MightyLibrary.util.commands;

import MightyLibrary.main.ManagerContainer;

public class Commands {

    private ManagerContainer manContainer;

    public Commands() {
        manContainer = ManagerContainer.getInstance();
    }

    public void writeCommand(String command){

    }

    public void command(String command) {
        int firstCommand = CommandMethods.indexFirstWord(command);
        if (firstCommand != -1) submitCommand(command, firstCommand);
        else System.err.println("Error on the message : \n invalids chars or nothing in the char");

    }

    private void submitCommand(String command, int index) {
        String sub = command.substring(0, index);

        switch (index) {
            case 1:
                submitCommand1(command, sub);
                break;
        }
    }

    private void submitCommand1(String command, String sub) {
    }
}
