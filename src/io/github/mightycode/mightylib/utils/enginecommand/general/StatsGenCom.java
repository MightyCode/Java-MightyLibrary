package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.JsonSelect;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.ArgumentType;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.suggest;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class StatsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "stats";
    // The only statistic of the player (USER), the others are for the developer
    private static final String FPS = "fps";

    public StatsGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.USER,
                "Timings and rendering counters of the last second, OpenGL objects alive and memory");
    }

    @Override
    protected void syntax(CommandNode root) {
        // The fields of the statistics (the first level, deeper ones are written with dots)
        ArgumentType<String> fields = suggest(() -> {
            List<String> values = new ArrayList<>(EngineStats.toJson().keySet());
            values.add("glObjects");
            return values;
        });

        root.then(literal(FPS, "Frames per second")
                        .executes(context -> context.currentLevel().includes(ECommandLevel.DEV)
                                ? fields(new String[]{FPS})
                                : CommandResult.ok("FPS : " + EngineStats.toJson().opt(FPS)))
                        .then(argument("fields", fields, "Other fields, dotted paths").variadic().level(ECommandLevel.DEV)
                                .executes(context -> {
                                    List<String> others = context.getList("fields");
                                    List<String> all = new ArrayList<>();
                                    all.add(FPS);
                                    all.addAll(others);
                                    return fields(all.toArray(new String[0]));
                                })))
                .then(literal("reset", "Reset the worst frame timings (maxFrameMsSinceReset,"
                        + " maxFrameIntervalMsSinceReset)").level(ECommandLevel.DEV)
                        .executes(context -> {
                            EngineStats.resetMax();
                            return CommandResult.ok("Worst frame timings reset");
                        }))
                .then(argument("fields", fields, "Only these fields (dotted paths, for example fps glObjects.Texture)")
                        .variadic().optional().level(ECommandLevel.DEV)
                        .executes("Every statistic, or only the given fields", context -> {
                            List<String> names = context.getList("fields");
                            return fields(names.toArray(new String[0]));
                        }));
    }

    // The statistics, or only the given fields
    private static CommandResult fields(String[] names) {
        JSONObject data = EngineStats.toJson();

        JSONObject glObjects = new JSONObject();
        for (GenDeleteResources.EGraphicResources type : GenDeleteResources.EGraphicResources.values())
            glObjects.put(type.name(), GenDeleteResources.Count(type));

        data.put("glObjects", glObjects);

        if (names.length > 0)
            data = JsonSelect.Select(data, names);

        return CommandResult.ok("Statistics of the last second", data);
    }
}
