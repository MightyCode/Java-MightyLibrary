package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.main.ContextManager;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.main.monitors.MonitorInfo;
import io.github.mightycode.mightylib.main.monitors.Monitors;
import io.github.mightycode.mightylib.main.monitors.VideoMode;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Connected screens with their resolution, and the one showing the window.
 */
public class MonitorsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "monitors";

    public MonitorsGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME));
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        return args.length == 2 ? Arrays.asList("full", "modes") : Collections.emptyList();
    }

    @Override
    public CommandResult execute(String[] args) {
        if (checkArgumentHelp(args))
            return CommandResult.ok(help());

        // Short by default: work area and refresh rate only with "monitors full"
        boolean full = args.length > 1 && args[1].equals("full");
        // The video modes are long: only with "monitors modes"
        boolean modes = args.length > 1 && args[1].equals("modes");

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

    @Override
    public String help() {
        return super.help() +
                "\nmonitors full : also the work area (without the task bar) and the refresh rate" +
                "\nmonitors modes : also the video modes of each screen (WIDTHxHEIGHT@HZ, for a fullscreen resolution)" +
                "\nmonitors : connected screens (index, name, resolution, position) and the index of the one"
                + " showing the window. The screen is chosen at start by EngineConfig.setMonitorSelector or"
                + " -Dmightylib.window.monitor=primary|secondary|largest|index|part of a name";
    }
}
