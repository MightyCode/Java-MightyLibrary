package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.resources.ResourceSelection;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;

public class ReloadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "reload";

    public ReloadGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return noEnoughArgumentError();

        ResourceSelection selection;
        try {
            selection = ResourceSelection.parse(Views.engine().resources(), args, 1);
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }

        int count = Views.engine().resources().reload(selection);
        return CommandResult.ok("Reload " + count + " data (" + selection + ")");
    }


    public String help() {
        return super.help() +
                "\n-> Resources type : name of the loader, for example TextureData, Shader, FontFace" +
                "\n3 uses : " +
                "\n1. reload [resources type] [name1,name2] [re:regex] : reload loaded resources of a type," +
                " only those named in the list and/or matching the regex (no space in it)" +
                "\n2. reload all [name1,name2] [re:regex] : same on every type" +
                "\n3. reload help : display tips about the command" +
                "\nNothing selected is not an error.";
    }
}
