package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.resources.DataType;
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

        if (args[1].equalsIgnoreCase("all")) {
            Resources.getInstance().reloadAllOfType();

            return CommandResult.ok("Reload all resources");
        }

        Class<? extends DataType> result = Resources.getClassFromName(args[1]);
        if (result == null)
            return unknownArgumentError(args[1]);

        Resources.getInstance().reloadAllOfType(result);

        return CommandResult.ok("Reload " + args[1] + " resources");
    }


    public String help() {
        return super.help() +
                "\n-> Resources type : name of the loader, for example TextureData, Shader, FontFace" +
                "\n3 uses : " +
                "\n1. reload [resources type] : reload loaded resources of a type" +
                "\n2. reload all : reload all loaded resources" +
                "\n3. reload help : display tips about the command";
    }
}
