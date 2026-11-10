package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.resources.texture.ETextureUpload;
import io.github.mightycode.mightylib.resources.texture.TextureUploadBench;
import io.github.mightycode.mightylib.resources.texture.TextureUploader;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.enumOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class TextureUploadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "textureupload";

    public TextureUploadGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "How the big textures are sent to the card, and what it cost since the mode was set");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("How the big textures are sent to the card",
                        context -> CommandResult.ok("How the big textures are sent to the card", TextureUploader.describe()))
                .then(literal("bench", "Measure each way on this card and driver (blocks the program for about a second)")
                        .executes(context -> CommandResult.ok("Time of the ways to send a texture, in milliseconds (median)",
                                TextureUploadBench.run())))
                .then(argument("mode", enumOf(ETextureUpload.class),
                        "direct reads the memory of the program, pbo uses pixel buffers")
                        .executes("Change the way the textures are sent", context -> {
                            ETextureUpload mode = context.get("mode");
                            TextureUploader.setMode(mode);
                            return CommandResult.ok("Textures are now sent in mode " + mode, TextureUploader.describe());
                        }));
    }
}
