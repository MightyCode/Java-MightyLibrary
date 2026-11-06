package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.main.ContextManager;
import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.main.monitors.MonitorInfo;
import MightyLibrary.mightylib.main.monitors.Monitors;
import MightyLibrary.mightylib.utils.enginecommand.BaseCommand;
import MightyLibrary.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Connected screens with their resolution, and the one showing the window.
 */
public class MonitorsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "monitors";

    public MonitorsGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        if (checkArgumentHelp(args))
            return CommandResult.ok(help());

        // Short by default: work area and refresh rate only with "monitors full"
        boolean full = args.length > 1 && args[1].equals("full");

        List<MonitorInfo> monitors = Monitors.List();
        JSONArray list = new JSONArray();
        for (MonitorInfo monitor : monitors) {
            JSONObject json = monitor.toJson();
            if (!full) {
                json.remove("workArea");
                json.remove("refreshRate");
            }
            list.put(json);
        }

        JSONObject data = new JSONObject();
        data.put("monitors", list);

        WindowInfo window = ContextManager.getInstance().getMainContext().getWindow().getInfo();
        MonitorInfo current = Monitors.OfWindow(window.getWindowId(), monitors);
        data.put("windowMonitor", current == null ? JSONObject.NULL : current.getIndex());

        return CommandResult.ok(monitors.size() + " monitor(s)", data);
    }

    @Override
    public String help() {
        return super.help() +
                "\nmonitors full : also the work area (without the task bar) and the refresh rate" +
                "\nmonitors : connected screens (index, name, resolution, position) and the index of the one"
                + " showing the window. The screen is chosen at start by EngineConfig.setMonitorSelector or"
                + " -Dmightylib.window.monitor=primary|secondary|largest|index|part of a name";
    }
}
