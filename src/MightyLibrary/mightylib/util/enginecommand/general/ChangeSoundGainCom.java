package MightyLibrary.mightylib.util.enginecommand.general;

import MightyLibrary.mightylib.sounds.SoundManager;
import MightyLibrary.mightylib.util.enginecommand.BaseCommand;

public class ChangeSoundGainCom extends BaseCommand {
    public static final String COMMAND_NAME = "changegain";

    public ChangeSoundGainCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public void sendCommand(String command) {
        String[] sub = command.split(" ");
        if(checkArgumentHelp(sub))
            return;

        if (sub.length > 1) {
            if (sub[1].equalsIgnoreCase("list")){
                SoundManager.getInstance().printGainTree();
            } else {
                if (sub.length < 3){
                    System.err.println("Should precise new value of gain !");
                    return;
                }

                float value;

                try {
                    value = Float.parseFloat(sub[2]);
                } catch (NumberFormatException e){
                    System.err.println("Should write a valid new gain value (float) !");
                    return;
                }

                if (SoundManager.getInstance().changeGain(sub[1], value)){
                    System.out.println("Update " + sub[1] + " node to a new gain of " + value);
                } else {
                    System.err.println("Unknown node called " + sub[1]);
                }
            }
        } else {
            noEnoughArgumentError();
        }
    }


        public void help() {
        super.help();
        System.out.println("Change gain of a gain node");
        System.out.println("changegain node value : change the gain of a gain node by the writted value");
        System.out.println("changegain list : List all of the gain node and their value");
        System.out.println("changegain help : display tips about the command");
    }
}
