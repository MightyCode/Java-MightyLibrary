package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.graphics.utils.Screenshot;
import MightyLibrary.mightylib.utils.enginecommand.BaseCommand;
import MightyLibrary.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "screenshot";
    private static final String DEFAULT_FOLDER = "screenshots";

    // Region keywords: two corners, or a corner and a size
    private static final String CORNERS = "corners";
    private static final String RECT = "rect";

    public ScreenshotGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (checkArgumentHelp(args))
            return CommandResult.ok(help());

        int index = 1;
        File file;
        if (args.length > index && !isRegionKeyword(args[index])) {
            file = new File(args[index]);
            ++index;
        } else {
            String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date());
            file = new File(DEFAULT_FOLDER, "screenshot_" + date + ".png");
        }

        int[] region = null;
        if (args.length > index) {
            String keyword = args[index];
            if (!isRegionKeyword(keyword) || args.length != index + 5)
                return CommandResult.error("Usage : screenshot [path] [" + CORNERS + " x y x2 y2 | " + RECT
                        + " x y width height]");

            int[] values = new int[4];
            try {
                for (int i = 0; i < 4; ++i)
                    values[i] = Integer.parseInt(args[index + 1 + i]);
            } catch (NumberFormatException e) {
                return CommandResult.error("Invalid number : " + e.getMessage());
            }

            if (keyword.equals(CORNERS)) {
                int x = Math.min(values[0], values[2]);
                int y = Math.min(values[1], values[3]);
                region = new int[]{ x, y, Math.abs(values[2] - values[0]), Math.abs(values[3] - values[1]) };
            } else {
                region = values;
            }

            if (region[2] <= 0 || region[3] <= 0)
                return CommandResult.error("Empty screenshot region");
        }

        // Answered once the image is written, after the next frame
        final int[] requestedRegion = region;
        return CommandResult.later(Screenshot.request(file, region).thenApply(written -> {
            JSONObject data = new JSONObject();
            data.put("path", written.getAbsolutePath());
            if (requestedRegion != null)
                data.put("region", requestedRegion[0] + "," + requestedRegion[1] + " "
                        + requestedRegion[2] + "x" + requestedRegion[3]);

            return CommandResult.ok("Screenshot saved", data);
        }));
    }

    private static boolean isRegionKeyword(String word) {
        return CORNERS.equals(word) || RECT.equals(word);
    }

    @Override
    public String help() {
        return super.help() +
                "\nscreenshot : save the next frame in " + DEFAULT_FOLDER + "/screenshot_[date].png" +
                "\nscreenshot [path] : save the next frame as a PNG at the given path" +
                "\nscreenshot [path] " + CORNERS + " x y x2 y2 : keep only the part between two corners" +
                "\nscreenshot [path] " + RECT + " x y width height : keep only a rectangle" +
                "\nRegions are in pixels of the window, origin at the top left, clipped to the image";
    }
}
