package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.graphics.utils.Screenshot;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.Option;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.decimal;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.path;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;

public class ScreenshotGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "screenshot";
    private static final String DEFAULT_FOLDER = "screenshots";

    // Region options: two corners, or a corner and a size
    private static final String CORNERS = "corners";
    private static final String RECT = "rect";
    private static final String SCALE = "scale";

    public ScreenshotGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER,
                "Save the next frame as a PNG, in " + DEFAULT_FOLDER + "/screenshot_[date].png by default");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.option(Option.of(RECT, "Keep only a rectangle, in pixels of the window, origin at the top left,"
                        + " clipped to the image")
                        .value("x", integer()).value("y", integer())
                        .value("width", integer(1, Integer.MAX_VALUE)).value("height", integer(1, Integer.MAX_VALUE))
                        .excludes(CORNERS))
                .option(Option.of(CORNERS, "Keep only the part between two corners, in pixels of the window")
                        .value("x", integer()).value("y", integer()).value("x2", integer()).value("y2", integer())
                        .excludes(RECT))
                .option(Option.of(SCALE, "Reduce the image, factor in ]0, 1] (0.5 : 4 times fewer pixels)")
                        .value("factor", decimal(0.001, 1)).defaultValue(1.0))
                .executes("Save the next frame in " + DEFAULT_FOLDER, context -> shoot(context, null))
                // The player only writes in the default folder
                .then(argument("path", path(), "Where to save the PNG").level(ECommandLevel.DEV)
                        .executes("Save the next frame as a PNG at the given path",
                                context -> shoot(context, context.<Path>get("path").toFile())));
    }

    private static CommandResult shoot(CommandContext context, File chosen) {
        File file = chosen;
        if (file == null) {
            String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date());
            file = new File(DEFAULT_FOLDER, "screenshot_" + date + ".png");
        }

        int[] region = null;
        if (context.has(RECT)) {
            List<Integer> values = context.get(RECT);
            region = new int[]{values.get(0), values.get(1), values.get(2), values.get(3)};
        } else if (context.has(CORNERS)) {
            List<Integer> values = context.get(CORNERS);
            int x = Math.min(values.get(0), values.get(2));
            int y = Math.min(values.get(1), values.get(3));
            region = new int[]{x, y, Math.abs(values.get(2) - values.get(0)), Math.abs(values.get(3) - values.get(1))};
        }

        if (region != null && (region[2] <= 0 || region[3] <= 0))
            return CommandResult.error("Empty screenshot region");

        float scale = context.<Double>get(SCALE).floatValue();

        // Answered once the image is written, after the next frame
        final int[] requestedRegion = region;
        return CommandResult.later(Screenshot.request(file, region, scale, Views.engine().commandWindow().getWindow().getInfo().getWindowId()).thenApply(written -> {
            JSONObject data = new JSONObject();
            data.put("path", written.getAbsolutePath());
            if (requestedRegion != null)
                data.put("region", requestedRegion[0] + "," + requestedRegion[1] + " "
                        + requestedRegion[2] + "x" + requestedRegion[3]);

            return CommandResult.ok("Screenshot saved", data);
        }));
    }
}
