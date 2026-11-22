package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.enumOf;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The 2D batches: mode, counters of the last frame of each view (draw calls, barriers and their causes), and the mode
public final class BatchGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "batch";

    private final Supplier<JSONObject> describe;
    private final Consumer<EBatch2DMode> setMode;

    public BatchGenCom() {
        this(() -> Batches2D.of(Views.engine()).describe(), mode -> Views.engine().setBatch2DMode(mode));
    }

    // What the batches are and how their mode changes, read when the command runs (tests give their own)
    BatchGenCom(Supplier<JSONObject> describe, Consumer<EBatch2DMode> setMode) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The 2D batches: mode, draw calls and barriers of the last frame of each view");
        this.describe = Objects.requireNonNull(describe, "describe");
        this.setMode = Objects.requireNonNull(setMode, "setMode");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("The mode, and for each view the draws and draw calls of its last frame, its barriers and"
                        + " what caused them", context -> show())
                .then(literal("mode", "How the 2D draws are sent")
                        .then(argument("mode", enumOf(EBatch2DMode.class),
                                "off draws each renderer alone, orphan and ring batch them (two ways of filling the"
                                        + " buffers), instanced draws the sprites by instances")
                                .executes("Change how the 2D draws are sent, at once", context -> {
                                    EBatch2DMode mode = context.get("mode");
                                    try {
                                        setMode.accept(mode);
                                    } catch (IllegalStateException e) {
                                        return CommandResult.error(e.getMessage());
                                    }
                                    return CommandResult.ok("2D draws now sent in mode " + mode.optionName(),
                                            describe.get());
                                })));
    }

    private CommandResult show() {
        JSONObject data;
        try {
            data = describe.get();
        } catch (RuntimeException e) {
            return CommandResult.error("No 2D batch: " + e.getMessage());
        }

        StringBuilder message = new StringBuilder("Mode ").append(data.optString("mode"));
        JSONArray views = data.optJSONArray("views");
        if (views != null) {
            for (int i = 0; i < views.length(); ++i) {
                JSONObject view = views.getJSONObject(i);
                // A view not displayed in the last frame (a scene left, its batch not collected yet), or that drew
                // nothing (a loading screen not shown), is in the data only
                if (!view.optBoolean("shownLastFrame", true)
                        || (view.optInt("draws") == 0 && view.optInt("barriers") == 0))
                    continue;
                message.append("\n").append(view.optString("view")).append(": ")
                        .append(view.optInt("draws")).append(" draws in ").append(view.optInt("drawCalls"))
                        .append(" draw calls, ").append(view.optInt("barriers")).append(" barriers");
                if (!view.isNull("sortKey"))
                    message.append(", sorted by ").append(view.optString("sortKey")).append(" (")
                            .append(view.optInt("barriersWhileSorted")).append(" barriers cut the sort)");

                JSONObject causes = view.optJSONObject("barrierCauses");
                if (causes != null && !causes.isEmpty()) {
                    message.append(", caused by");
                    for (String cause : causes.keySet())
                        message.append(" ").append(shortName(cause)).append(" x")
                                .append(causes.getJSONObject(cause).optInt("count"));
                }

                // The batch formats of the draw calls (a format cuts the runs)
                JSONObject formats = view.optJSONObject("formats");
                if (formats != null && !formats.isEmpty()) {
                    message.append(", formats");
                    for (String format : new TreeSet<>(formats.keySet()))
                        message.append(" ").append(format).append(" x").append(formats.optInt(format));
                }
            }
        }

        return CommandResult.ok(message.toString(), data);
    }

    // The class without its package (a renderer), or the name as it is
    private static String shortName(String cause) {
        int dot = cause.lastIndexOf('.');
        return dot < 0 ? cause : cause.substring(dot + 1);
    }
}
