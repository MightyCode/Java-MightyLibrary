package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import io.github.mightycode.mightylib.main.monitors.Monitors;
import io.github.mightycode.mightylib.main.monitors.VideoMode;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

/**
 * Connected screens with their resolution, and the one showing the window.
 */
public class MonitorsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "monitors";

    public MonitorsGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "Connected screens (index, name, resolution, position) and the index of the one showing the window");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("The screen is chosen at start by EngineConfig.setMonitorSelector or"
                        + " -Dmightylib.window.monitor=primary|secondary|largest|index|part of a name",
                        context -> describe(false, false))
                .then(literal("full", "Also the work area (without the task bar) and the refresh rate")
                        .executes(context -> describe(true, false)))
                .then(literal("modes", "Also the video modes of each screen (WIDTHxHEIGHT@HZ, for a fullscreen resolution)")
                        .executes(context -> describe(false, true)));
    }

    // Short by default: work area and refresh rate only with full, the long video modes only with modes
    private static CommandResult describe(boolean full, boolean modes) {
        List<MonitorInfo> monitors = Monitors.List();
        JSONArray list = new JSONArray();
        for (MonitorInfo monitor : monitors) {
            JSONObject json = monitor.toJson();
            if (!full) {
                json.remove("workArea");
                json.remove("refreshRate");
            }
            if (modes) {
                JSONArray videoModes = new JSONArray();
                for (VideoMode mode : monitor.getVideoModes())
                    videoModes.put(mode.toString());
                json.put("videoModes", videoModes);
            }
            list.put(json);
        }

        JSONObject data = new JSONObject();
        data.put("monitors", list);

        WindowInfo window = Views.engine().commandWindow().getWindow().getInfo();
        MonitorInfo current = Monitors.OfWindow(window.getWindowId(), monitors);
        data.put("windowMonitor", current == null ? JSONObject.NULL : current.getIndex());

        return CommandResult.ok(monitors.size() + " monitor(s)", data);
    }
}
