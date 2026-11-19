package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.color.IColorService;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONObject;

import java.util.Objects;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The player's settings of the picture (gamma, brightness, contrast), kept in its configuration
public final class PictureGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "picture";

    private final Supplier<IColorService> service;

    public PictureGenCom() {
        this(() -> Views.engine().colors());
    }

    // The service read when the command runs (tests give their own)
    PictureGenCom(Supplier<IColorService> service) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER,
                "Settings of the picture: gamma, brightness, contrast (1 = unchanged), saved");
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    protected void syntax(CommandNode root) {
        String range = "from " + Format(PictureSettings.MIN) + " to " + Format(PictureSettings.MAX) + ", 1 = unchanged";
        root.executes("The current settings", context -> show("Picture"))
                .then(literal("gamma", "Lighter above 1, darker below")
                        .then(argument("value", decimal(PictureSettings.MIN, PictureSettings.MAX), range)
                                .executes(context -> set(service.get().picture()
                                        .withGamma(value(context.get("value")))))))
                .then(literal("brightness", "Multiplies the colors")
                        .then(argument("value", decimal(PictureSettings.MIN, PictureSettings.MAX), range)
                                .executes(context -> set(service.get().picture()
                                        .withBrightness(value(context.get("value")))))))
                .then(literal("contrast", "Spreads the colors from the middle grey")
                        .then(argument("value", decimal(PictureSettings.MIN, PictureSettings.MAX), range)
                                .executes(context -> set(service.get().picture()
                                        .withContrast(value(context.get("value")))))))
                .then(literal("reset", "Back to the settings of the game")
                        .executes(context -> {
                            service.get().resetPicture();
                            return show("Picture reset");
                        }));
    }

    private static float value(Double value) {
        return value.floatValue();
    }

    private CommandResult set(PictureSettings picture) {
        service.get().setPicture(picture);
        return show("Picture");
    }

    private CommandResult show(String title) {
        PictureSettings picture = service.get().picture();
        JSONObject data = new JSONObject();
        data.put("gamma", picture.gamma());
        data.put("brightness", picture.brightness());
        data.put("contrast", picture.contrast());
        data.put("min", PictureSettings.MIN);
        data.put("max", PictureSettings.MAX);
        return CommandResult.ok(title + ": " + picture, data);
    }

    private static String Format(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.valueOf(value);
    }
}
