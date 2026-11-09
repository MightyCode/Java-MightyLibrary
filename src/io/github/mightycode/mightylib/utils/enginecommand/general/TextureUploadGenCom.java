package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.resources.texture.ETextureUpload;
import io.github.mightycode.mightylib.resources.texture.TextureUploadBench;
import io.github.mightycode.mightylib.resources.texture.TextureUploader;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TextureUploadGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "textureupload";

    public TextureUploadGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV);
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        return args.length == 2 ? Arrays.asList("direct", "pbo", "bench") : Collections.emptyList();
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length < 2)
            return CommandResult.ok("How the big textures are sent to the card", TextureUploader.describe());

        if (args[1].equals("bench"))
            return CommandResult.ok("Time of the ways to send a texture, in milliseconds (median)", TextureUploadBench.run());

        ETextureUpload mode = ETextureUpload.parse(args[1]);
        if (mode == null)
            return CommandResult.error("Unknown way ->" + args[1] + "<-, expected direct, pbo or bench");

        TextureUploader.setMode(mode);
        return CommandResult.ok("Textures are now sent in mode " + mode, TextureUploader.describe());
    }

    @Override
    public String help() {
        return super.help() +
                "\ntextureupload : how the big textures are sent to the card, and what it cost since the mode was set." +
                "\ntextureupload direct|pbo : change the way (direct reads the memory of the program, pbo uses pixel buffers)." +
                "\ntextureupload bench : measure each way on this card and driver (blocks the program for about a second).";
    }
}
