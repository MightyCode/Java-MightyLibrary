package io.github.mightycode.mightylib.utils.enginecommand.internal;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.ViewCullTools;
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
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.Arguments.integer;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.argument;
import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

// The 2D batches: mode, counters of the last frame of each view (draw calls, barriers and their causes), the mode, and
// the view culling with its tools of verification
public final class BatchGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "batch";

    private final Supplier<JSONObject> describe;
    private final Consumer<EBatch2DMode> setMode;
    private final Consumer<Boolean> setCulling;
    private final Supplier<ViewCullTools> tools;
    // Freezes the boxes of the views of the batches (the table of their states)
    private final Runnable freeze;

    public BatchGenCom() {
        this(() -> Batches2D.of(Views.engine()).describe(), mode -> Views.engine().setBatch2DMode(mode),
                culling -> Views.engine().setViewCulling2D(culling),
                () -> Batches2D.of(Views.engine()).cullTools(),
                () -> Batches2D.of(Views.engine()).cullTools().freeze(Batches2D.of(Views.engine()).table()));
    }

    // What the batches are and how their mode changes, read when the command runs (tests give their own)
    BatchGenCom(Supplier<JSONObject> describe, Consumer<EBatch2DMode> setMode) {
        this(describe, setMode, culling -> { }, ViewCullTools::new, () -> { });
    }

    BatchGenCom(Supplier<JSONObject> describe, Consumer<EBatch2DMode> setMode, Consumer<Boolean> setCulling,
                Supplier<ViewCullTools> tools, Runnable freeze) {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The 2D batches: mode, draw calls and barriers of the last frame of each view, view culling");
        this.describe = Objects.requireNonNull(describe, "describe");
        this.setMode = Objects.requireNonNull(setMode, "setMode");
        this.setCulling = Objects.requireNonNull(setCulling, "setCulling");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.freeze = Objects.requireNonNull(freeze, "freeze");
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
                                })))
                .then(cull());
    }

    // batch cull: the view culling of the 2D draws and its tools of verification
    private CommandNode cull() {
        return literal("cull", "The view culling of the batched 2D draws: draws wholly outside the view are not written")
                .executes("The state of the view culling and of its tools", context -> cullState("View culling"))
                .then(literal("on", "Cull the draws outside the view (the default)")
                        .executes("Turn the view culling on", context -> setCulling(true)))
                .then(literal("off", "Write every draw, inside the view or not")
                        .executes("Turn the view culling off", context -> setCulling(false)))
                .then(literal("inset", "Reduce the test zone: the draws disappear this far inside the edges")
                        .then(argument("pixels", integer(0, 10000), "Pixels of the viewport on each side, 0 for none")
                                .executes("Reduce the test zone by these pixels", context -> {
                                    int pixels = context.get("pixels");
                                    tools.get().setInset(pixels);
                                    return cullState(pixels == 0 ? "Test zone back to the view"
                                            : "Test zone reduced by " + pixels + " pixels on each side");
                                })))
                .then(literal("show", "Draw the box of each tested draw (green drawn, red culled) in world space")
                        .executes("Show the boxes", context -> show(true))
                        .then(literal("on", "Show the boxes").executes("Show the boxes", context -> show(true)))
                        .then(literal("off", "Stop showing them").executes("Stop showing the boxes",
                                context -> show(false))))
                .then(literal("freeze", "Keep the box of each view: move the camera to see what it would cull")
                        .executes("Freeze the boxes of the views", context -> {
                            try {
                                freeze.run();
                            } catch (RuntimeException e) {
                                return CommandResult.error("No 2D batch: " + e.getMessage());
                            }
                            return cullState("Boxes of the views frozen, a camera new from now keeps its own");
                        }))
                .then(literal("unfreeze", "The boxes follow the cameras again")
                        .executes("Unfreeze the boxes", context -> {
                            tools.get().unfreeze();
                            return cullState("Boxes follow the cameras again");
                        }))
                .then(literal("list", "The draws culled of the last complete frame, as JSON (recorded from this call)")
                        .executes("List the culled draws", context -> {
                            ViewCullTools cullTools = tools.get();
                            if (!cullTools.isRecording()) {
                                cullTools.setRecording(true);
                                return CommandResult.ok("Recording the culled draws: ask again after a frame",
                                        cullTools.describe());
                            }

                            JSONObject last = cullTools.listLast();
                            if (last == null)
                                return CommandResult.ok("No complete frame recorded yet: ask again",
                                        cullTools.describe());
                            return CommandResult.ok(last.getInt("count") + " draws culled in the last frame"
                                    + (last.getBoolean("truncated") ? " (list truncated to " + last.getInt("limit")
                                    + ")" : ""), last);
                        })
                        .then(literal("off", "Stop recording").executes("Stop recording", context -> {
                            tools.get().setRecording(false);
                            return cullState("Recording of the culled draws stopped");
                        })));
    }

    private CommandResult setCulling(boolean on) {
        try {
            setCulling.accept(on);
        } catch (IllegalStateException e) {
            return CommandResult.error(e.getMessage());
        }
        return cullState("View culling " + (on ? "on" : "off"));
    }

    private CommandResult show(boolean on) {
        tools.get().setShow(on);
        return cullState(on ? "Boxes shown (world space, one frame each)" : "Boxes hidden");
    }

    // The message gives the tools that are on; the data the state of the tools and of the culling
    private CommandResult cullState(String title) {
        JSONObject data;
        try {
            data = describe.get();
        } catch (RuntimeException e) {
            return CommandResult.error("No 2D batch: " + e.getMessage());
        }

        JSONObject state = tools.get().describe().put("viewCulling", data.optBoolean("viewCulling", true));
        return CommandResult.ok(title + ": " + (state.getBoolean("viewCulling") ? "on" : "off")
                + (state.getInt("inset") > 0 ? ", inset " + state.getInt("inset") + " px" : "")
                + (state.getBoolean("show") ? ", boxes shown" : "")
                + (state.getBoolean("frozen") ? ", boxes frozen" : "")
                + (state.getBoolean("recording") ? ", recording the culled" : ""), state);
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

                // What was sent, and what the view culling left out (the classes that gave no box)
                if (view.has("bytes"))
                    message.append(", ").append(view.optLong("bytes")).append(" bytes sent (")
                            .append(Math.round(view.optDouble("bytesPerCall"))).append(" and ")
                            .append(Math.round(view.optDouble("drawsPerCall") * 10) / 10.0)
                            .append(" draws per call)");
                JSONObject cull = view.optJSONObject("viewCull");
                if (cull != null && cull.optInt("tested") + cull.optInt("noBounds") > 0) {
                    message.append(", view culling: ").append(cull.optInt("culled")).append(" of ")
                            .append(cull.optInt("tested")).append(" tested culled");
                    JSONObject noBounds = cull.optJSONObject("noBoundsClasses");
                    if (cull.optInt("noBounds") > 0) {
                        message.append(", ").append(cull.optInt("noBounds")).append(" without box");
                        if (noBounds != null) {
                            for (String owner : noBounds.keySet())
                                message.append(" ").append(shortName(owner)).append(" x")
                                        .append(noBounds.optInt(owner));
                        }
                    }
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
