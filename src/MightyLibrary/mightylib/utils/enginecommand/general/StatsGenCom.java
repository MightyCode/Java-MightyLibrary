package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.graphics.utils.GenDeleteResources;
import MightyLibrary.mightylib.main.utils.EngineStats;
import MightyLibrary.mightylib.utils.enginecommand.BaseCommand;
import MightyLibrary.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

public class StatsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "stats";

    public StatsGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        JSONObject data = EngineStats.toJson();

        JSONObject glObjects = new JSONObject();
        for (GenDeleteResources.EGraphicResources type : GenDeleteResources.EGraphicResources.values())
            glObjects.put(type.name(), GenDeleteResources.Count(type));

        data.put("glObjects", glObjects);

        return CommandResult.ok("Statistics of the last second", data);
    }

    @Override
    public String help() {
        return super.help() +
                "\nstats : timings and rendering counters of the last second, OpenGL objects alive and memory";
    }
}
