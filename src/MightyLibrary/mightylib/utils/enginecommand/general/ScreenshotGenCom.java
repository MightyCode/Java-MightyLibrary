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

    public ScreenshotGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        File file;
        if (args.length > 1) {
            file = new File(args[1]);
        } else {
            String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date());
            file = new File(DEFAULT_FOLDER, "screenshot_" + date + ".png");
        }

        // Answered once the image is written, after the next frame
        return CommandResult.later(Screenshot.request(file).thenApply(written -> {
            JSONObject data = new JSONObject();
            data.put("path", written.getAbsolutePath());

            return CommandResult.ok("Screenshot saved", data);
        }));
    }

    @Override
    public String help() {
        return super.help() +
                "\nscreenshot : save the next frame in " + DEFAULT_FOLDER + "/screenshot_[date].png" +
                "\nscreenshot [path] : save the next frame as a PNG at the given path";
    }
}
