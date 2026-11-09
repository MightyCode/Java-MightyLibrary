package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import io.github.mightycode.mightylib.utils.enginecommand.JsonSelect;
import org.json.JSONObject;

import java.util.Arrays;

public class StatsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "stats";

    public StatsGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public Collection<String> completeArgument(String[] args) {
        // stats reset, or fields of the statistics (the first level, deeper ones are written with dots)
        List<String> values = new ArrayList<>(EngineStats.toJson().keySet());
        values.add("glObjects");
        if (args.length == 2)
            values.add("reset");
        return values;
    }

    @Override
    public CommandResult execute(String[] args) {
        if (args.length > 1 && args[1].equals("reset")) {
            EngineStats.resetMax();
            return CommandResult.ok("Worst frame timings reset");
        }

        JSONObject data = EngineStats.toJson();

        JSONObject glObjects = new JSONObject();
        for (GenDeleteResources.EGraphicResources type : GenDeleteResources.EGraphicResources.values())
            glObjects.put(type.name(), GenDeleteResources.Count(type));

        data.put("glObjects", glObjects);

        // stats field1 field2... : only these fields (dotted paths, for example glObjects.Texture)
        if (args.length > 1)
            data = JsonSelect.Select(data, Arrays.copyOfRange(args, 1, args.length));

        return CommandResult.ok("Statistics of the last second", data);
    }

    @Override
    public String help() {
        return super.help() +
                "\nstats : timings and rendering counters of the last second, OpenGL objects alive and memory" +
                "\nstats [field...] : only the given fields (dotted paths, for example fps glObjects.Texture)" +
                "\nstats reset :reset the worst frame timings (maxFrameMsSinceReset, maxFrameIntervalMsSinceReset)";
    }
}
