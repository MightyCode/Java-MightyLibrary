package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.ResourceArguments;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

public class ReloadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "reload";

    public ReloadGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Reload loaded resources of a type, only those named and/or matching a regex. Nothing selected is not an error");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.then(ResourceArguments.selection(() -> Views.engine().resources(),
                "all or the name of a resource type, for example TextureData, ShaderData, FontFace", selection -> {
                    int count = Views.engine().resources().reload(selection);
                    return CommandResult.ok("Reload " + count + " data (" + selection + ")");
                }));
    }
}
