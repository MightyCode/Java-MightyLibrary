package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class ChangeSoundGainCom extends BaseCommand {
    public static final String COMMAND_NAME = "changegain";

    public ChangeSoundGainCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return noEnoughArgumentError();

        if (args[1].equalsIgnoreCase("list")){
            SoundManager.getInstance().printGainTree();

            return CommandResult.ok("Gain tree printed in the program console");
        }

        if (args.length < 3)
            return CommandResult.error("Should precise new value of gain !");

        float value;

        try {
            value = Float.parseFloat(args[2]);
        } catch (NumberFormatException e){
            return CommandResult.error("Should write a valid new gain value (float) !");
        }

        if (!SoundManager.getInstance().changeGain(args[1], value))
            return CommandResult.error("Unknown node called " + args[1]);

        return CommandResult.ok("Update " + args[1] + " node to a new gain of " + value);
    }


    public String help() {
        return super.help() +
                "\nChange gain of a gain node" +
                "\nchangegain node value : change the gain of a gain node by the written value" +
                "\nchangegain list : list all of the gain nodes and their value" +
                "\nchangegain help : display tips about the command";
    }
}
