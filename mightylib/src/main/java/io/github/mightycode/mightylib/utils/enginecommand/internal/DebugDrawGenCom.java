package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.debug.DebugDraws;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The debug drawings of the views (IView.debugDraw): on or off, dropped, counted by lifetime
public final class DebugDrawGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "debugdraw";

    private final Supplier<DebugDraws> debugDraws;

    public DebugDrawGenCom() {
        this(() -> Views.engine().debugDraws());
    }

    // The debug drawings of the engine, read when the command runs (tests give their own)
    DebugDrawGenCom(Supplier<DebugDraws> debugDraws) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The debug drawings of the views: on or off, dropped, counted by lifetime");
        this.debugDraws = Objects.requireNonNull(debugDraws, "debugDraws");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("On or off, and the drawings alive of each view by lifetime", context -> list())
                .then(literal("on", "Record and draw the debug drawings")
                        .executes("Debug drawings on", context -> {
                            debugDraws.get().setEnabled(true);
                            return CommandResult.ok("Debug drawings on", debugDraws.get().describe());
                        }))
                .then(literal("off", "Neither record nor draw them, those waiting are dropped")
                        .executes("Debug drawings off", context -> {
                            debugDraws.get().setEnabled(false);
                            return CommandResult.ok("Debug drawings off", debugDraws.get().describe());
                        }))
                .then(literal("clear", "Drop the drawings waiting, of every lifetime")
                        .executes("Drop the debug drawings", context -> {
                            int count = debugDraws.get().describe().optInt("drawings");
                            debugDraws.get().clear();
                            return CommandResult.ok(count + " debug drawings dropped", debugDraws.get().describe());
                        }))
                .then(literal("list", "The drawings alive of each view, by lifetime")
                        .executes("Count the debug drawings", context -> list()));
    }

    private CommandResult list() {
        JSONObject data = debugDraws.get().describe();
        StringBuilder message = new StringBuilder("Debug drawings ").append(data.optBoolean("enabled") ? "on" : "off")
                .append(", ").append(data.optInt("drawings")).append(" alive");
        JSONArray views = data.optJSONArray("views");
        if (views != null) {
            for (int i = 0; i < views.length(); ++i) {
                JSONObject view = views.getJSONObject(i);
                if (view.optInt("drawings") == 0)
                    continue;

                message.append("\n").append(view.optString("view")).append(": ");
                JSONObject lifetimes = view.optJSONObject("lifetimes");
                boolean first = true;
                for (String lifetime : new TreeSet<>(lifetimes.keySet())) {
                    message.append(first ? "" : ", ").append(lifetime).append(" x").append(lifetimes.optInt(lifetime));
                    first = false;
                }
            }
        }

        return CommandResult.ok(message.toString(), data);
    }
}
